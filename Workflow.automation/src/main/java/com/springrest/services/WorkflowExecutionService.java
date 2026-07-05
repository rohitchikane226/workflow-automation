package com.springrest.services;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import com.springrest.Entities.*;
import com.springrest.httpUtils.HttpUtils;
import com.springrest.kafka.WorkflowKafkaProducer;
import com.springrest.repository.*;
import com.springrest.helpers.PostScriptResult;
import com.springrest.helpers.ScriptResult;

@Service
public class WorkflowExecutionService {

	private final WorkflowRepository workflowRepo;
	private final WorkflowStepRepository stepRepo;
	private final WorkflowStepInputRepository inputRepo;
	private final WorkflowStepOutputRepository outputRepo;
	private final WorkflowRunRepository runRepo;
	private final WorkflowHistoryRepository historyRepo;
	private final AuthGenerationService authGenerationService;
	private final ConditionService conditionService;
	private final HttpUtils httpUtils;
	private final ScriptEngineService scriptEngineService;
	private WorkflowTriggerRepository workflowTriggerRepository;
	private final ObjectMapper mapper = new ObjectMapper();
	private WorkflowKafkaProducer kafkaProducer;
	private boolean useMock = false;

	public WorkflowExecutionService(WorkflowRepository workflowRepo, WorkflowStepRepository stepRepo ,WorkflowKafkaProducer kafkaProducer,
			ConditionService conditionService, WorkflowStepInputRepository inputRepo,
			WorkflowStepOutputRepository outputRepo, WorkflowRunRepository runRepo,
			WorkflowHistoryRepository historyRepo, HttpUtils httpUtils, AuthGenerationService authGenerationService,
			ScriptEngineService scriptEngineService, WorkflowTriggerRepository workflowTriggerRepository) {
		this.workflowRepo = workflowRepo;
        this.kafkaProducer=kafkaProducer;
		this.stepRepo = stepRepo;
		this.inputRepo = inputRepo;
		this.conditionService = conditionService;
		this.outputRepo = outputRepo;
		this.workflowTriggerRepository = workflowTriggerRepository;
		this.authGenerationService = authGenerationService;
		this.runRepo = runRepo;
		this.historyRepo = historyRepo;
		this.httpUtils = httpUtils;
		this.scriptEngineService = scriptEngineService;
	}

	@Transactional
	public Map<String, Object> executeWorkflow(Long workflowId, Map<String, Object> payload) throws Exception {
		Workflow workflow = workflowRepo.findById(workflowId)
				.orElseThrow(() -> new RuntimeException("Workflow not found"));
		WorkflowRun run = new WorkflowRun();
		run.setWorkflow(workflow);
		run.setIteration(getNextIteration(workflowId));
		run.setStatus("RUNNING");
		runRepo.save(run);

		Map<String, Map<String, Object>> executionContext = new LinkedHashMap<>();
		Set<Long> allowedSteps = new HashSet<>();
	   executionContext.put("step1", payload);

//		if (webhookPayload == null) {
//			seedTriggerOutputs(workflowId, executionContext);
//		} else {
//			executionContext.put("step1", webhookPayload);
//		}

		List<WorkflowStep> steps = stepRepo.findByWorkflowIdOrderByStepOrderAsc(workflowId);

		boolean success = true;
		String errorMessage = null;
		try {
			for (WorkflowStep step : steps) {
				if (step.getTrigger() != null)
					continue;
				if (!allowedSteps.isEmpty() && !allowedSteps.contains(step.getId())) {
					System.out.println("⏭ Skipping step (not in active branch): " + step.getId());
					continue;
				}
				if (step.getStepType() == StepType.CONDITION) {
					System.out.println("🔀 Condition step: " + step.getId());
					ConditionRule rule = mapper.readValue(step.getConditionJson(), ConditionRule.class);
					Map<String, Object> flatContext = new HashMap<>();

					for (Map.Entry<String, Map<String, Object>> entry : executionContext.entrySet()) {
						String stepKey = entry.getKey();
						Map<String, Object> stepData = entry.getValue();

						for (Map.Entry<String, Object> field : stepData.entrySet()) {

							flatContext.put(field.getKey(), field.getValue());
							flatContext.put(stepKey + "." + field.getKey(), field.getValue());
						}
					}

					boolean result = conditionService.evaluate(rule, flatContext);
					System.out.println("Condition result: " + result);

					if (result) {
						if (step.getTrueStepId() != null) {
							allowedSteps.clear();
							allowedSteps.add(step.getTrueStepId());
						}
					} else {
						if (step.getFalseStepId() != null) {
							allowedSteps.clear();
							allowedSteps.add(step.getFalseStepId());
						}
					}
					Map<String, Object> conditionData = new HashMap<>();
					conditionData.put("result", result);
					conditionData.put("timestamp", System.currentTimeMillis());
					conditionData.put("stepId", step.getId());

					executionContext.put("condition_" + step.getStepOrder(), conditionData);

					continue;
				}
	
				Long stepId = step.getId();
				System.out.println("▶️ Executing Step ID: " + stepId);

				List<WorkflowStepInput> inputs = inputRepo.findByStepId(stepId);
				Map<String, Object> resolvedInputs = new HashMap<>();
				for (WorkflowStepInput input : inputs) {
					String rawValue = input.getValue();
					String finalValue = resolvePlaceholders(rawValue, executionContext);
					resolvedInputs.put(input.getStepKey(), finalValue);
				}

				long startTime = System.currentTimeMillis();
				if (step.getAction() != null 
					    && step.getAction().getKey() != null 
					    && step.getAction().getKey().equalsIgnoreCase("Get Image Status")) {

					    System.out.println("⏳ Waiting 10 seconds before calling Get Image Status...");

					    try {
					        Thread.sleep(20000); 
					    } catch (InterruptedException e) {
					        Thread.currentThread().interrupt();
					    }
					}
				Map<String, Object> apiResponse = useMock ? getMockResponse(step, resolvedInputs)
						: executeRealApi(step, resolvedInputs);
				System.out.println("apiResponse " + apiResponse.toString());
				long endTime = System.currentTimeMillis();

				long duration = endTime - startTime;
				if (apiResponse.containsKey("errorType")) {
					String type = apiResponse.get("errorType").toString();
					String message = apiResponse.get("errorMessage").toString();
					String raw = apiResponse.get("raw").toString();
					success = false;
					errorMessage = message;
					WorkflowHistory history = new WorkflowHistory();
					history.setRun(run);
					history.setStep(step);
					history.setStatus("FAILED");
					history.setRequestJson(toJsonString(resolvedInputs));
					history.setErrorMessage(message);
					historyRepo.save(history);

					break;
				}

				Map<String, Object> filteredOutputs = new HashMap<>();
				if (step.getAction() != null) {
					for (ActionField f : step.getAction().getFields()) {
						if ("output".equalsIgnoreCase(f.getFieldType())) {
							Object value = getNestedValue(apiResponse, f.getKey());
							if (value != null)
								filteredOutputs.put(f.getKey(), value);
						}
					}
				}
				System.out.println("step id" + step.getId());

				List<Map<String, Object>> dynamicFields = getDynamicOutputFields(step, toJsonString(resolvedInputs));
				System.out.println("dynamicFields " + dynamicFields.toString());

				Set<String> dynamicKeys = new HashSet<>();

				for (Map<String, Object> field : dynamicFields) {
					String key = (String) field.get("key");
					if (key != null) {
						dynamicKeys.add(key);

						Object value = getNestedValue(apiResponse, key);
						if (value != null && !filteredOutputs.containsKey(key)) {
							filteredOutputs.put(key, value);
						}
					}
				}

				Set<String> staticKeys = new HashSet<>();

				if (step.getAction() != null) {
					for (ActionField f : step.getAction().getFields()) {
						if ("output".equalsIgnoreCase(f.getFieldType())) {
							staticKeys.add(f.getKey());
						}
					}
				}

				saveStaticOutputs(step, filteredOutputs, staticKeys);

				syncDynamicOutputs(step, filteredOutputs, dynamicKeys);
				WorkflowHistory history = new WorkflowHistory();
				history.setRun(run);
				history.setStep(step);
				history.setStatus("SUCCESS");
				history.setRequestJson(toJsonString(resolvedInputs));
				history.setResponseJson(toJsonString(apiResponse));
				history.setExecutionTimeMs(duration);
				System.out.println("history  " + history.toString());
				historyRepo.save(history);
				executionContext.put("step" + step.getStepOrder(), filteredOutputs);
			}

		} catch (Exception e) {
			success = false;
			errorMessage = e.getMessage();
			e.printStackTrace();
		}

		run.setStatus(success ? "SUCCESS" : "FAILED");
		runRepo.save(run);

		Map<String, Object> response = new HashMap<>();
		response.put("workflowId", workflowId);
		response.put("iteration", run.getIteration());
		response.put("status", success ? "SUCCESS" : "FAILED");
		response.put("results", executionContext);

		if (errorMessage != null) {
			response.put("error", errorMessage);
		}

		return response;

	}

	private int getNextIteration(Long workflowId) {
		List<WorkflowRun> runs = runRepo.findByWorkflowIdOrderByIterationDesc(workflowId);
		if (runs.isEmpty())
			return 1;
		return runs.get(0).getIteration() + 1;
	}

	private void seedTriggerOutputs(Long workflowId, Map<String, Map<String, Object>> context) {
		List<WorkflowStep> steps = stepRepo.findByWorkflowIdOrderByStepOrderAsc(workflowId);
		if (steps.isEmpty())
			return;
		WorkflowStep triggerStep = steps.get(0);
		if (triggerStep.getTrigger() == null)
			return;

		List<WorkflowStepOutput> triggerOutputs = outputRepo.findByStepId(triggerStep.getId());
		Map<String, Object> triggerContext = new HashMap<>();
		for (WorkflowStepOutput out : triggerOutputs) {
			triggerContext.put(out.getKey(), out.getValue());
		}
		context.put("step1", triggerContext);
	}

	private Map<String, Object> executeRealApi(WorkflowStep step, Map<String, Object> inputs) {

		Map<String, Object> finalResponse = new HashMap<>();

		try {
			String url = step.getAction().getApiEndpoint();
			String method = step.getAction().getHttpMethod();
			String script = step.getAction().getScript();
			Connection connection = step.getConnection();
			Connector connector = step.getAction().getConnector();
			boolean isWebhookConnector = connector != null && "webhook".equalsIgnoreCase(connector.getAppKey());

			Map<String, String> headers = new HashMap<>();
			Map<String, String> query = new HashMap<>();
			Map<String, Object> body = new HashMap<>();
			System.out.println("connector Name" + step.getAction().getConnector().getName());

			for (ActionField f : step.getAction().getFields()) {
				if (!"input".equalsIgnoreCase(f.getFieldType()))
					continue;
				String val = inputs.getOrDefault(f.getKey(), "").toString();
				putByPlacement(f.getPlacement(), f.getKey(), val, headers, query, body);
			}
			String finalUrl = url;
			Map<String, String> finalHeaders = new HashMap<>(headers);
			Map<String, String> finalQueryParams = new HashMap<>(query);

			if (!isWebhookConnector) {
				AuthGenerationService.AuthResult auth = authGenerationService.applyAuth(connection.getId(), url);
				finalUrl = auth.getFinalUrl();
				finalHeaders = merge(auth.getHeaders(), headers);

				finalQueryParams = merge(auth.getQueryParams(), query);
			}
			Map<String, Object> rawBody = new HashMap<>();

			ScriptResult scriptResult = scriptEngineService.executePreScript(script, finalUrl, toJsonString(body), // Body
																													// (API
																													// body)
					toJsonString(inputs), finalHeaders, finalQueryParams, connection);
			String finalBody = scriptResult.getBody() != null ? scriptResult.getBody() : toJsonString(body);
			System.out.println("finalBody " + finalBody);
			Map<String, String> newHeaders = merge(finalHeaders, scriptResult.getHeaders());
			Map<String, String> newQuery = merge(finalQueryParams, scriptResult.getQueryParams());
			String newUrl = scriptResult.getUrl() != null ? scriptResult.getUrl() : finalUrl;
			Map<String, Object> apiRawResponse = httpUtils.callApiWithPlacement(newUrl, method, newHeaders, newQuery,
					finalBody);
			int statusCode = (int) apiRawResponse.get("statusCode");
			String responseBody = (String) apiRawResponse.get("body");
			System.out.println("apiRawResponse " + responseBody);

			PostScriptResult postResult = scriptEngineService.executePostScript(script, statusCode, responseBody,
					connection);

			if (!"success".equalsIgnoreCase(postResult.getStatus())) {

				finalResponse.put("errorType", postResult.getStatus());
				finalResponse.put("errorMessage", postResult.getMessage());
				finalResponse.put("raw", postResult.getRaw());

				return finalResponse;
			}

			Map<String, Object> jsonMap = mapper.readValue(postResult.getMessage(),
					new TypeReference<Map<String, Object>>() {
					});

			finalResponse.putAll(jsonMap);

		} catch (Exception ex) {
			finalResponse.put("errorType", "hard_error");
			finalResponse.put("errorMessage", ex.getMessage());
			finalResponse.put("raw", "");
		}

		return finalResponse;
	}

	private String resolvePlaceholders(String raw, Map<String, Map<String, Object>> context) {

		if (raw == null)
			return null;

		Pattern pattern = Pattern.compile("\\{\\{(.*?)\\}\\}");
		Matcher matcher = pattern.matcher(raw);

		StringBuffer result = new StringBuffer();

		while (matcher.find()) {

			String expression = matcher.group(1);

			String[] parts = expression.split("\\.", 2);

			if (parts.length < 2)
				continue;

			String stepKey = parts[0];
			String path = parts[1];

			Object value = getNestedValue(context.get(stepKey), path);

			matcher.appendReplacement(result, value != null ? value.toString() : "");
		}

		matcher.appendTail(result);

		return result.toString();
	}

	private Object getNestedValue(Object root, String path) {
		if (root == null || path == null)
			return null;
		String[] tokens = path.split("\\.");
		Object current = root;
		for (String token : tokens) {
			if (current == null)
				return null;
			if (token.contains("[")) {
				String key = token.substring(0, token.indexOf("["));
				int index = Integer.parseInt(token.substring(token.indexOf("[") + 1, token.indexOf("]")));
				if (current instanceof Map) {
					current = ((Map<?, ?>) current).get(key);
				}
				if (current instanceof List) {
					List<?> list = (List<?>) current;
					if (index < 0 || index >= list.size())
						return null;
					current = list.get(index);
				} else {
					return null;
				}
			} else {
				if (current instanceof Map) {
					current = ((Map<?, ?>) current).get(token);
				} else {
					return null;
				}
			}
		}
		return current;
	}

	private Map<String, String> merge(Map<String, String> base, Map<String, String> extra) {
		Map<String, String> result = new HashMap<>(base);
		if (extra != null)
			result.putAll(extra);
		return result;
	}

	private String toJsonString(Object obj) {
		try {
			return mapper.writeValueAsString(obj);
		} catch (Exception e) {
			return "{}";
		}
	}

	private void putByPlacement(String placement, String key, String value, Map<String, String> headers,
			Map<String, String> queryParams, Map<String, Object> bodyParams) {
		if ("header".equalsIgnoreCase(placement)) {
			headers.put(key, value);
		} else if ("query".equalsIgnoreCase(placement)) {
			queryParams.put(key, value);
		} else {
			bodyParams.put(key, value);
		}
	}

	private Map<String, Object> getMockResponse(WorkflowStep step, Map<String, Object> inputs) {
		Map<String, Object> mock = new HashMap<>();
		if (step.getAction() != null) {
			for (ActionField f : step.getAction().getFields()) {
				if ("output".equalsIgnoreCase(f.getFieldType())) {
					mock.put(f.getKey(), inputs.getOrDefault(f.getKey(), "mock_" + f.getKey()));
				}
			}
		}
		return mock;
	}

	public void saveTriggerOutputs(WorkflowStep triggerStep, Map<String, Object> payload) {
		Long stepId = triggerStep.getId();

		outputRepo.deleteByStepId(stepId);

		Map<String, String> flatMap = new HashMap<>();
		flattenJson("", payload, flatMap);

		for (Map.Entry<String, String> entry : flatMap.entrySet()) {
			WorkflowStepOutput output = new WorkflowStepOutput(entry.getKey(), entry.getValue(), triggerStep);
			outputRepo.save(output);
		}
	}

	private void flattenJson(String prefix, Object value, Map<String, String> flatMap) {

		if (value instanceof Map) {
			Map<String, Object> map = (Map<String, Object>) value;
			for (Map.Entry<String, Object> entry : map.entrySet()) {
				flattenJson(prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey(), entry.getValue(),
						flatMap);
			}
		} else if (value instanceof List) {
			List<Object> list = (List<Object>) value;
			for (int i = 0; i < list.size(); i++) {
				flattenJson(prefix + "[" + i + "]", list.get(i), flatMap);
			}
		}

		else {

			flatMap.put(prefix, value != null ? value.toString() : "");
		}
	}

	@Transactional
	public Map<String, Object> testStep(Long stepId) throws Exception {

		WorkflowStep step = stepRepo.findById(stepId).orElseThrow(() -> new RuntimeException("Step not found"));

		Map<String, Map<String, Object>> executionContext = new LinkedHashMap<>();
		Set<Long> allowedSteps = new HashSet<>();

		seedTriggerOutputs(step.getWorkflow().getId(), executionContext);

		seedTestOutputs(step.getWorkflow(), step.getStepOrder(), executionContext);

		List<WorkflowStepInput> inputs = inputRepo.findByStepId(stepId);
		Map<String, Object> resolvedInputs = new HashMap<>();

		for (WorkflowStepInput input : inputs) {
			validatePlaceholders(input.getValue(), step.getStepOrder());
			String finalValue = resolvePlaceholders(input.getValue(), executionContext);
			resolvedInputs.put(input.getStepKey(), finalValue);
		}

		Map<String, Object> apiResponse = executeRealApi(step, resolvedInputs);

		if (apiResponse.containsKey("errorType")) {
			return Map.of("status", "FAILED", "errorType", apiResponse.get("errorType"), "errorMessage",
					apiResponse.get("errorMessage"), "raw", apiResponse.get("raw"));
		}

		Map<String, Object> filteredOutputs = new HashMap<>();

		if (step.getAction() != null) {
			for (ActionField f : step.getAction().getFields()) {
				if ("output".equalsIgnoreCase(f.getFieldType())) {
					Object value = getNestedValue(apiResponse, f.getKey());
					if (value != null) {
						filteredOutputs.put(f.getKey(), value);
					}
				}
			}
		}

		for (Map.Entry<String, Object> entry : filteredOutputs.entrySet()) {

			String key = entry.getKey();
			String value = entry.getValue() != null ? entry.getValue().toString() : "";

			Optional<WorkflowStepOutput> existingOutputOpt = outputRepo.findByStepIdAndActionKey(step.getId(), key);

			WorkflowStepOutput output;
			if (existingOutputOpt.isPresent()) {
				output = existingOutputOpt.get();
				output.setValue(value);
			} else {
				output = new WorkflowStepOutput(key, value, step);
			}

			outputRepo.save(output);
		}

		List<Map<String, Object>> dynamicFields = getDynamicOutputFields(step, toJsonString(resolvedInputs));

		System.out.println("dynamicFields  " + dynamicFields);

		Set<String> dynamicKeys = new HashSet<>();

		for (Map<String, Object> field : dynamicFields) {
			String key = (String) field.get("key");
			if (key != null) {
				dynamicKeys.add(key);

				Object value = getNestedValue(apiResponse, key);
				if (value != null && !filteredOutputs.containsKey(key)) {
					filteredOutputs.put(key, value);
				}
			}
		}

		Set<String> staticKeys = new HashSet<>();

		if (step.getAction() != null) {
			for (ActionField f : step.getAction().getFields()) {
				if ("output".equalsIgnoreCase(f.getFieldType())) {
					staticKeys.add(f.getKey());
				}
			}
		}

		saveStaticOutputs(step, filteredOutputs, staticKeys);

		syncDynamicOutputs(step, filteredOutputs, dynamicKeys);

		return Map.of("status", "SUCCESS", "resolvedInputs", resolvedInputs, "outputs", filteredOutputs, "rawResponse",
				apiResponse);
	}

	private void seedTestOutputs(Workflow workflow, int currentOrder, Map<String, Map<String, Object>> ctx) {
		List<WorkflowStep> steps = stepRepo.findByWorkflowIdOrderByStepOrderAsc(workflow.getId());

		for (WorkflowStep s : steps) {
			if (s.getStepOrder() >= currentOrder)
				break;

			List<WorkflowStepOutput> outputs = outputRepo.findByStepId(s.getId());

			if (!outputs.isEmpty()) {
				Map<String, Object> map = new HashMap<>();
				for (WorkflowStepOutput o : outputs) {
					map.put(o.getKey(), o.getValue());
				}
				ctx.put("step" + s.getStepOrder(), map);
			}
		}
	}

	private void validatePlaceholders(String value, int currentOrder) {
		Pattern p = Pattern.compile("\\{\\{step(\\d+)\\.");
		Matcher m = p.matcher(value);

		while (m.find()) {
			int stepNum = Integer.parseInt(m.group(1));
			if (stepNum >= currentOrder) {
				throw new RuntimeException("Invalid reference: step" + stepNum + " is not available during step test");
			}
		}
	}

	public WorkflowTrigger getTriggerByWebhookUuid(String webhookUuid) {

		return workflowTriggerRepository.findByWebhookUuid(webhookUuid).orElseThrow(null);
	}

	private WorkflowStep getPreviousStep(List<WorkflowStep> steps, WorkflowStep current) {

		for (int i = 0; i < steps.size(); i++) {
			if (steps.get(i).getId().equals(current.getId())) {
				if (i > 0) {
					return steps.get(i - 1);
				}
			}
		}
		return null;
	}

	private List<Map<String, Object>> getDynamicOutputFields(WorkflowStep step, String apiResponseJson) {
		try {
			if (step.getAction() == null || !step.getAction().isHasDynamicFields()) {
				return Collections.emptyList();
			}

			String script = step.getAction().getScript();

			String outputFieldsJson = scriptEngineService.executeOutputFields(script, apiResponseJson,
					step.getConnection());

			return mapper.readValue(outputFieldsJson, new TypeReference<List<Map<String, Object>>>() {
			});
		} catch (Exception e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
	}

	private void syncDynamicOutputs(WorkflowStep step, Map<String, Object> filteredOutputs, Set<String> dynamicKeys) {

		Set<String> staticKeys = new HashSet<>();

		if (step.getAction() != null) {
			for (ActionField f : step.getAction().getFields()) {
				if ("output".equalsIgnoreCase(f.getFieldType())) {
					staticKeys.add(f.getKey());
				}
			}
		}

		List<WorkflowStepOutput> existingOutputs = outputRepo.findByStepId(step.getId());

		Map<String, WorkflowStepOutput> existingMap = new HashMap<>();

		for (WorkflowStepOutput o : existingOutputs) {
			existingMap.put(o.getKey(), o);
		}

		for (String key : dynamicKeys) {

			String value = filteredOutputs.get(key) != null ? filteredOutputs.get(key).toString() : "";

			WorkflowStepOutput output;

			if (existingMap.containsKey(key)) {
				output = existingMap.get(key);
				output.setValue(value);

				existingMap.remove(key);
			} else {
				output = new WorkflowStepOutput(key, value, step);
			}

			outputRepo.save(output);
		}
		System.out.println("DB KEYS: " + existingMap.keySet());
		System.out.println("STATIC KEYS: " + staticKeys);
		System.out.println("DYNAMIC KEYS: " + dynamicKeys);

		for (WorkflowStepOutput leftover : existingMap.values()) {

			String key = leftover.getKey();

			if (staticKeys.contains(key)) {
				continue;
			}

			if (!dynamicKeys.contains(key)) {
				outputRepo.delete(leftover);
			}
		}
	}

	private void saveStaticOutputs(WorkflowStep step, Map<String, Object> filteredOutputs, Set<String> staticKeys) {
		for (String key : staticKeys) {

			if (!filteredOutputs.containsKey(key))
				continue;

			String value = filteredOutputs.get(key) != null ? filteredOutputs.get(key).toString() : "";

			Optional<WorkflowStepOutput> existing = outputRepo.findByStepIdAndActionKey(step.getId(), key);

			WorkflowStepOutput output;

			if (existing.isPresent()) {
				output = existing.get();
				output.setValue(value);
			} else {
				output = new WorkflowStepOutput(key, value, step);
			}

			outputRepo.save(output);
		}
	}

}
