package com.springrest.services;

import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springrest.Entities.*;
import com.springrest.helpers.PostScriptResult;
import com.springrest.helpers.ScriptResult;
import com.springrest.httpUtils.HttpUtils;
import com.springrest.kafka.WorkflowKafkaProducer;
import com.springrest.repository.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PollingService {

	private final WorkflowRepository workflowRepo;
	private final WorkflowStepInputRepository inputRepo;
	private final WorkflowStepRepository stepRepo;
	private final WorkflowRunRepository workflowRunRepo;
	private final WorkflowStepOutputRepository outputRepo;
	private final ProcessedRecordRepository processedRecordRepo;
	private final WorkflowHistoryRepository historyRepo;
	private final HttpUtils httpUtils;
	private final ScriptEngineService scriptEngineService;
	private final WorkflowExecutionService workflowExecutionService;
	private final AuthGenerationService authService;
	private static final Logger log = LoggerFactory.getLogger(PollingService.class);

	private final ExecutorService executor = new ThreadPoolExecutor(20, 50, 60, TimeUnit.SECONDS,
			new LinkedBlockingQueue<>(1000), new ThreadPoolExecutor.CallerRunsPolicy());
	private final ObjectMapper objectMapper = new ObjectMapper();
	@Autowired
	private WorkflowKafkaProducer kafkaProducer;

	public PollingService(WorkflowRepository workflowRepo, WorkflowStepRepository stepRepo,
			WorkflowStepOutputRepository outputRepo, WorkflowStepInputRepository inputRepo,
			ProcessedRecordRepository processedRecordRepo, HttpUtils httpUtils, ScriptEngineService scriptEngineService,
			WorkflowExecutionService workflowExecutionService, AuthGenerationService authService,
			WorkflowHistoryRepository historyRepo, WorkflowRunRepository workflowRunRepo) {
		this.workflowRepo = workflowRepo;
		this.inputRepo = inputRepo;
		this.stepRepo = stepRepo;
		this.outputRepo = outputRepo;
		this.historyRepo = historyRepo;
		this.workflowRunRepo = workflowRunRepo;
		this.processedRecordRepo = processedRecordRepo;
		this.httpUtils = httpUtils;
		this.scriptEngineService = scriptEngineService;
		this.workflowExecutionService = workflowExecutionService;
		this.authService = authService;
	}

	@Transactional
	@Scheduled(fixedRate = 120000)
	public void pollActiveWorkflows() {
   System.out.println("method called");
		List<Workflow> workflows = workflowRepo.findReadyWorkflows(PageRequest.of(0, 50));
		System.out.println("Found workflows = " + workflows.size());

		for (Workflow wf : workflows) {
			Long workflowId = wf.getId();
			executor.submit(() -> processWorkflowSafely(workflowId));
		}
	}

	private void processWorkflowSafely(Long workflowId) {
		Workflow wf = workflowRepo.findById(workflowId).orElse(null);
		if (wf == null)
			return;

		try {
			wf.setLocked(true);
			workflowRepo.save(wf);

			processWorkflow(workflowId);

		} catch (Exception e) {
			log.error("Error processing workflow {}", workflowId, e);
		} finally {
			wf.setLocked(false);
			wf.setNextPollTime(LocalDateTime.now().plusMinutes(2));
			workflowRepo.save(wf);
		}
	}

	private void processWorkflow(Long workflowId) {
		// findByWorkflowIdOrderByStepOrderAsc replace by findFullStepsByWorkflowId
		try {
			List<WorkflowStep> steps = stepRepo.findFullStepsByWorkflowId(workflowId);
			if (steps == null || steps.isEmpty())
				return;

			WorkflowStep triggerStep = steps.get(0);
			if (triggerStep.getTrigger() == null || "webhook".equals(triggerStep.getTrigger().getTriggerType()))
				return;

			List<Map<String, Object>> records = fetchTriggerRecords(triggerStep);
			if (records == null || records.isEmpty())
				return;

			String recordKey = triggerStep.getTrigger().getRecordIdentifierKey();
			if (recordKey == null || recordKey.isBlank())
				return;

			for (Map<String, Object> record : records) {

				String uniqueValue = extractIdentifierValue(record, recordKey);
				if (uniqueValue == null)
					continue;

				if (processedRecordRepo.existsByWorkflowIdAndTriggerUniqueId(workflowId, uniqueValue)) {
					continue;
				}

				long startTime = System.currentTimeMillis();

				WorkflowRun run = new WorkflowRun();
				run.setWorkflow(triggerStep.getWorkflow());
				run.setStatus("RUNNING");
				run.setStartedAt(LocalDateTime.now());
				run.setIteration(0);
				run = workflowRunRepo.save(run);

				WorkflowHistory history = new WorkflowHistory();
				history.setRun(run);
				history.setStep(triggerStep);
				history.setStatus("SUCCESS");
				history.setRequestJson("{}");
				history.setResponseJson(objectMapper.writeValueAsString(record));
				history.setExecutionTimeMs(System.currentTimeMillis() - startTime);
				history.setCreatedAt(LocalDateTime.now());
				historyRepo.save(history);
				saveTriggerOutputs(triggerStep, record);
				processedRecordRepo.save(new ProcessedRecord(workflowId, uniqueValue, LocalDateTime.now()));
//				executor.submit(() -> {
//					try {
//						workflowExecutionService.executeWorkflow(workflowId, seedTriggerOutputs(workflowId));
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//				});
				Map<String, Object> payload = seedTriggerOutputs(workflowId);

				kafkaProducer.sendWorkflowEvent(workflowId, payload);
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private List<Map<String, Object>> fetchTriggerRecords(WorkflowStep triggerStep) throws IOException {

		Trigger trigger = triggerStep.getTrigger();

		String url = trigger.getApiEndpoint();
		String method = trigger.getHttpMethod();
		String script = trigger.getScript();
		Long connectionId = triggerStep.getConnection().getId();
		AuthGenerationService.AuthResult auth = authService.applyAuth(connectionId, url);
		String finalUrl = auth.getFinalUrl();
		Map<String, String> finalHeaders = new HashMap<>(auth.getHeaders());
		Map<String, String> finalQueryParams = new HashMap<>(auth.getQueryParams());
		List<WorkflowStepInput> triggerInuts = inputRepo.findByStepId(triggerStep.getId());
		Map<String, String> triggerContext = new HashMap<>();
		for (WorkflowStepInput out : triggerInuts) {
			triggerContext.put(out.getStepKey(), out.getValue());
		}
		String RawBody = toJsonString(triggerContext);
		ScriptResult pre = scriptEngineService.executePreScript(script, finalUrl, "{}", RawBody, finalHeaders,
				finalQueryParams, null);

		String preBody = pre.getBody() != null ? pre.getBody() : "{}";

		finalHeaders.putAll(pre.getHeaders());
		finalQueryParams.putAll(pre.getQueryParams());
		finalUrl = pre.getUrl() != null ? pre.getUrl() : finalUrl;

		long startTime = System.currentTimeMillis();
		Map<String, Object> apiResponse = httpUtils.callApiWithPlacement(finalUrl, method, finalHeaders,
				finalQueryParams, preBody);
		int statusCode = (int) apiResponse.get("statusCode");
		String responseBody = (String) apiResponse.get("body");
		long endTime = System.currentTimeMillis();
		long duration = endTime - startTime;
		try {
			PostScriptResult post = scriptEngineService.executePostScript(script, statusCode, responseBody, null);
			System.out.println("outside condition " + responseBody);
			if (!"success".equalsIgnoreCase(post.getStatus())) {
				System.out.println("under condition " + post.getStatus());

				savePollingFailureHistory(triggerStep, post, preBody, responseBody, duration);
				return Collections.emptyList();
			}
			Object parsed = objectMapper.readValue(post.getMessage(), Object.class);

			if (parsed instanceof List)
				return (List<Map<String, Object>>) parsed;

			if (parsed instanceof Map)
				return Collections.singletonList((Map<String, Object>) parsed);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return Collections.emptyList();
	}

	private String extractIdentifierValue(Map<String, Object> record, String recordKey) {
		Object value = getNestedValue(record, recordKey);
		return value != null ? value.toString() : null;
	}

	private void saveTriggerOutputs(WorkflowStep triggerStep, Map<String, Object> record) {

		String stepInputJson = buildStepInputJson(triggerStep.getId());

		Map<String, Object> filteredOutputs = new HashMap<>();

		Set<String> staticKeys = new HashSet<>();

		for (TriggerField f : triggerStep.getTrigger().getFields()) {

			if (!"output".equalsIgnoreCase(f.getFieldType()))
				continue;

			staticKeys.add(f.getKey());

			Object value = getNestedValue(record, f.getKey());

			if (value != null) {
				filteredOutputs.put(f.getKey(), value);
			}
		}

		List<Map<String, Object>> dynamicFields = getDynamicTriggerFields(triggerStep, stepInputJson);

		Set<String> dynamicKeys = new HashSet<>();

		for (Map<String, Object> field : dynamicFields) {

			String key = (String) field.get("key");

			if (key != null) {

				dynamicKeys.add(key);

				Object value = getNestedValue(record, key);

				if (value != null && !filteredOutputs.containsKey(key)) {
					filteredOutputs.put(key, value);
				}
			}
		}
		System.out.println("filteredOutputs  :" + filteredOutputs.toString());
		for (String key : staticKeys) {

			if (!filteredOutputs.containsKey(key))
				continue;

			String value = filteredOutputs.get(key).toString();

			Optional<WorkflowStepOutput> existing = outputRepo.findByStepIdAndActionKey(triggerStep.getId(), key);

			WorkflowStepOutput out;

			if (existing.isPresent()) {
				out = existing.get();
				out.setValue(value);
			} else {
				out = new WorkflowStepOutput(key, value, triggerStep);
			}

			outputRepo.save(out);
		}

		syncDynamicTriggerOutputs(triggerStep, filteredOutputs, dynamicKeys, staticKeys);
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

				if (current instanceof Map)
					current = ((Map<?, ?>) current).get(key);

				if (current instanceof List) {
					List<?> list = (List<?>) current;
					if (index < 0 || index >= list.size())
						return null;
					current = list.get(index);
				}

			} else {
				if (current instanceof Map)
					current = ((Map<?, ?>) current).get(token);
			}
		}

		return current;
	}

	private void savePollingFailureHistory(WorkflowStep triggerStep, PostScriptResult post, String requestJson,
			String responseBody, long duration) {
		try {
			WorkflowHistory history = new WorkflowHistory();
			history.setRun(null);

			history.setStep(triggerStep);
			history.setStatus("FAILED");

			history.setErrorType(post.getStatus());
			history.setErrorMessage(post.getMessage());

			history.setRequestJson(requestJson);
			history.setResponseJson(responseBody);

			history.setExecutionTimeMs(duration);
			history.setCreatedAt(LocalDateTime.now());

			historyRepo.save(history);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String buildStepInputJson(Long stepId) {
		try {
			List<WorkflowStepInput> inputs = inputRepo.findByStepId(stepId);

			Map<String, Object> inputMap = new HashMap<>();

			for (WorkflowStepInput input : inputs) {
				inputMap.put(input.getStepKey(), input.getValue());
			}

			return objectMapper.writeValueAsString(inputMap);

		} catch (Exception e) {
			e.printStackTrace();
			return "{}";
		}
	}

	private List<Map<String, Object>> getDynamicTriggerFields(WorkflowStep step, String stepInputJson) {
		try {
			if (step.getTrigger() == null || !Boolean.TRUE.equals(step.getTrigger().getHasDynamicFields())) {
				return Collections.emptyList();
			}

			String script = step.getTrigger().getScript();

			String outputFieldsJson = scriptEngineService.executeOutputFields(script, stepInputJson,
					step.getConnection());

			return objectMapper.readValue(outputFieldsJson, new TypeReference<List<Map<String, Object>>>() {
			});

		} catch (Exception e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
	}

	private void syncDynamicTriggerOutputs(WorkflowStep step, Map<String, Object> filteredOutputs,
			Set<String> dynamicKeys, Set<String> staticKeys) {

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

		for (WorkflowStepOutput leftover : existingMap.values()) {

			String key = leftover.getKey();

			if (staticKeys.contains(key))
				continue;

			if (!dynamicKeys.contains(key)) {
				outputRepo.delete(leftover);
			}
		}
	}

	private Map<String, Object>  seedTriggerOutputs(Long workflowId) {
		Map<String, Map<String, Object>> executionContext = new LinkedHashMap<>();
		List<WorkflowStep> steps = stepRepo.findByWorkflowIdOrderByStepOrderAsc(workflowId);
		if (steps.isEmpty())
			return null;
		WorkflowStep triggerStep = steps.get(0);
		if (triggerStep.getTrigger() == null)
			return null;

		List<WorkflowStepOutput> triggerOutputs = outputRepo.findByStepId(triggerStep.getId());
		Map<String, Object> triggerContext = new HashMap<>();
		for (WorkflowStepOutput out : triggerOutputs) {
			triggerContext.put(out.getKey(), out.getValue());
		}
		executionContext.put("step1", triggerContext);
		return triggerContext;
	}
	
	private String toJsonString(Object obj) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			return mapper.writeValueAsString(obj);
		} catch (Exception e) {
			return "{}";
		}
	}
}

/*
 * old polling code package com.springrest.services;
 * 
 * import com.fasterxml.jackson.core.type.TypeReference; import
 * com.fasterxml.jackson.databind.ObjectMapper; import
 * com.springrest.Entities.*; import com.springrest.helpers.PostScriptResult;
 * import com.springrest.helpers.ScriptResult; import
 * com.springrest.httpUtils.HttpUtils; import com.springrest.repository.*;
 * import org.springframework.scheduling.annotation.Scheduled; import
 * org.springframework.stereotype.Service; import
 * org.springframework.transaction.annotation.Transactional;
 * 
 * import java.io.IOException; import java.time.LocalDateTime; import
 * java.util.*;
 * 
 * @Service public class PollingService {
 * 
 * private final WorkflowRepository workflowRepo; private final
 * WorkflowStepInputRepository inputRepo; private final WorkflowStepRepository
 * stepRepo; private final WorkflowRunRepository workflowRunRepo; private final
 * WorkflowStepOutputRepository outputRepo; private final
 * ProcessedRecordRepository processedRecordRepo; private final
 * WorkflowHistoryRepository historyRepo; private final HttpUtils httpUtils;
 * private final ScriptEngineService scriptEngineService; private final
 * WorkflowExecutionService workflowExecutionService; private final
 * AuthGenerationService authService;
 * 
 * private final ObjectMapper objectMapper = new ObjectMapper();
 * 
 * public PollingService(WorkflowRepository workflowRepo, WorkflowStepRepository
 * stepRepo, WorkflowStepOutputRepository outputRepo,WorkflowStepInputRepository
 * inputRepo, ProcessedRecordRepository processedRecordRepo, HttpUtils
 * httpUtils, ScriptEngineService scriptEngineService, WorkflowExecutionService
 * workflowExecutionService, AuthGenerationService authService,
 * WorkflowHistoryRepository historyRepo,WorkflowRunRepository workflowRunRepo)
 * { this.workflowRepo = workflowRepo; this.inputRepo=inputRepo; this.stepRepo =
 * stepRepo; this.outputRepo = outputRepo; this.historyRepo = historyRepo;
 * this.workflowRunRepo=workflowRunRepo; this.processedRecordRepo =
 * processedRecordRepo; this.httpUtils = httpUtils; this.scriptEngineService =
 * scriptEngineService; this.workflowExecutionService =
 * workflowExecutionService; this.authService = authService; }
 * 
 * @Transactional
 * 
 * @Scheduled(fixedRate = 120000) public void pollActiveWorkflows() throws
 * IOException { List<Workflow> workflows = workflowRepo.findAll(); for
 * (Workflow wf : workflows) { if (!wf.isActive()) continue; List<WorkflowStep>
 * steps = stepRepo.findByWorkflowIdOrderByStepOrderAsc(wf.getId()); if (steps
 * == null || steps.isEmpty()) continue; WorkflowStep triggerStep =
 * steps.get(0); if (triggerStep.getTrigger() == null
 * ||"webhook".equals(triggerStep.getTrigger().getTriggerType())) continue; long
 * startTime = System.currentTimeMillis(); List<Map<String, Object>> records =
 * fetchTriggerRecords(triggerStep);
 * 
 * if (records == null || records.isEmpty()) continue;
 * 
 * String recordKey = triggerStep.getTrigger().getRecordIdentifierKey(); if
 * (recordKey == null || recordKey.isBlank()) continue;
 * 
 * for (Map<String, Object> record : records) {
 * 
 * String uniqueValue = extractIdentifierValue(record, recordKey); if
 * (uniqueValue == null) continue;
 * 
 * if (processedRecordRepo.existsByWorkflowIdAndTriggerUniqueId(wf.getId(),
 * uniqueValue)) { continue; } long endTime = System.currentTimeMillis(); long
 * duration = endTime - startTime; WorkflowHistory history = new
 * WorkflowHistory(); WorkflowRun run = new WorkflowRun();
 * run.setWorkflow(triggerStep.getWorkflow()); run.setStatus("RUNNING");
 * run.setStartedAt(LocalDateTime.now()); run = workflowRunRepo.save(run);
 * history.setRun(run); history.setStep(triggerStep); run.setIteration(0);
 * history.setStatus("SUCCESS");
 * 
 * history.setRequestJson("{}");
 * 
 * history.setResponseJson(objectMapper.writeValueAsString(record));
 * 
 * history.setExecutionTimeMs(duration);
 * 
 * history.setCreatedAt(LocalDateTime.now());
 * 
 * historyRepo.save(history); saveTriggerOutputs(triggerStep, record);
 * 
 * processedRecordRepo.save(new ProcessedRecord(wf.getId(), uniqueValue,
 * LocalDateTime.now())); try {
 * workflowExecutionService.executeWorkflow(wf.getId(),null); } catch (Exception
 * e) { e.printStackTrace(); } } } } private List<Map<String, Object>>
 * fetchTriggerRecords(WorkflowStep triggerStep) throws IOException {
 * 
 * Trigger trigger = triggerStep.getTrigger();
 * 
 * String url = trigger.getApiEndpoint(); String method =
 * trigger.getHttpMethod(); String script = trigger.getScript(); Long
 * connectionId = triggerStep.getConnection().getId();
 * AuthGenerationService.AuthResult auth = authService.applyAuth(connectionId,
 * url); String finalUrl = auth.getFinalUrl(); Map<String, String> finalHeaders
 * = new HashMap<>(auth.getHeaders()); Map<String, String> finalQueryParams =
 * new HashMap<>(auth.getQueryParams()); List<WorkflowStepInput>
 * triggerInuts=inputRepo.findByStepId(triggerStep.getId()); Map<String, String>
 * triggerContext = new HashMap<>(); for (WorkflowStepInput out : triggerInuts)
 * { triggerContext.put( out.getStepKey(), out.getValue() ); } String RawBody =
 * toJsonString(triggerContext); ScriptResult pre =
 * scriptEngineService.executePreScript(script, finalUrl, "{}", RawBody,
 * finalHeaders, finalQueryParams,null);
 * 
 * String preBody = pre.getBody() != null ? pre.getBody() : "{}";
 * 
 * finalHeaders.putAll(pre.getHeaders());
 * finalQueryParams.putAll(pre.getQueryParams()); finalUrl = pre.getUrl() !=
 * null ? pre.getUrl() : finalUrl;
 * 
 * long startTime = System.currentTimeMillis(); Map<String, Object> apiResponse
 * = httpUtils.callApiWithPlacement(finalUrl, method, finalHeaders,
 * finalQueryParams, preBody); int statusCode = (int)
 * apiResponse.get("statusCode"); String responseBody = (String)
 * apiResponse.get("body"); long endTime = System.currentTimeMillis(); long
 * duration = endTime - startTime; try { PostScriptResult post =
 * scriptEngineService.executePostScript(script, statusCode, responseBody,null);
 * System.out.println("outside condition " + responseBody); if
 * (!"success".equalsIgnoreCase(post.getStatus())) {
 * System.out.println("under condition " + post.getStatus());
 * 
 * savePollingFailureHistory( triggerStep, post, preBody, responseBody, duration
 * ); return Collections.emptyList(); } Object parsed =
 * objectMapper.readValue(post.getMessage(), Object.class);
 * 
 * if (parsed instanceof List) return (List<Map<String, Object>>) parsed;
 * 
 * if (parsed instanceof Map) return Collections.singletonList((Map<String,
 * Object>) parsed);
 * 
 * } catch (Exception e) { e.printStackTrace(); }
 * 
 * return Collections.emptyList(); } private String
 * extractIdentifierValue(Map<String, Object> record, String recordKey) { Object
 * value = getNestedValue(record, recordKey); return value != null ?
 * value.toString() : null; }
 * 
 * private void saveTriggerOutputs(WorkflowStep triggerStep, Map<String, Object>
 * record) {
 * 
 * String stepInputJson = buildStepInputJson(triggerStep.getId());
 * 
 * Map<String, Object> filteredOutputs = new HashMap<>();
 * 
 * Set<String> staticKeys = new HashSet<>();
 * 
 * for (TriggerField f : triggerStep.getTrigger().getFields()) {
 * 
 * if (!"output".equalsIgnoreCase(f.getFieldType())) continue;
 * 
 * staticKeys.add(f.getKey());
 * 
 * Object value = getNestedValue(record, f.getKey());
 * 
 * if (value != null) { filteredOutputs.put(f.getKey(), value); } }
 * 
 * 
 * List<Map<String, Object>> dynamicFields =
 * getDynamicTriggerFields(triggerStep, stepInputJson);
 * 
 * Set<String> dynamicKeys = new HashSet<>();
 * 
 * for (Map<String, Object> field : dynamicFields) {
 * 
 * String key = (String) field.get("key");
 * 
 * if (key != null) {
 * 
 * dynamicKeys.add(key);
 * 
 * Object value = getNestedValue(record, key);
 * 
 * if (value != null && !filteredOutputs.containsKey(key)) {
 * filteredOutputs.put(key, value); } } }
 * System.out.println("filteredOutputs  :"+filteredOutputs.toString()); for
 * (String key : staticKeys) {
 * 
 * if (!filteredOutputs.containsKey(key)) continue;
 * 
 * String value = filteredOutputs.get(key).toString();
 * 
 * Optional<WorkflowStepOutput> existing =
 * outputRepo.findByStepIdAndActionKey(triggerStep.getId(), key);
 * 
 * WorkflowStepOutput out;
 * 
 * if (existing.isPresent()) { out = existing.get(); out.setValue(value); } else
 * { out = new WorkflowStepOutput(key, value, triggerStep); }
 * 
 * outputRepo.save(out); }
 * 
 * syncDynamicTriggerOutputs(triggerStep, filteredOutputs, dynamicKeys,
 * staticKeys); } private Object getNestedValue(Object root, String path) { if
 * (root == null || path == null) return null;
 * 
 * String[] tokens = path.split("\\."); Object current = root;
 * 
 * for (String token : tokens) {
 * 
 * if (current == null) return null;
 * 
 * if (token.contains("[")) { String key = token.substring(0,
 * token.indexOf("[")); int index =
 * Integer.parseInt(token.substring(token.indexOf("[") + 1,
 * token.indexOf("]")));
 * 
 * if (current instanceof Map) current = ((Map<?, ?>) current).get(key);
 * 
 * if (current instanceof List) { List<?> list = (List<?>) current; if (index <
 * 0 || index >= list.size()) return null; current = list.get(index); }
 * 
 * } else { if (current instanceof Map) current = ((Map<?, ?>)
 * current).get(token); } }
 * 
 * return current; } private void savePollingFailureHistory( WorkflowStep
 * triggerStep, PostScriptResult post, String requestJson, String responseBody,
 * long duration ) { try { WorkflowHistory history = new WorkflowHistory();
 * history.setRun(null);
 * 
 * history.setStep(triggerStep); history.setStatus("FAILED");
 * 
 * history.setErrorType(post.getStatus());
 * history.setErrorMessage(post.getMessage());
 * 
 * history.setRequestJson(requestJson); history.setResponseJson(responseBody);
 * 
 * history.setExecutionTimeMs(duration);
 * history.setCreatedAt(LocalDateTime.now());
 * 
 * historyRepo.save(history);
 * 
 * } catch (Exception e) { e.printStackTrace(); } } private String
 * buildStepInputJson(Long stepId) { try { List<WorkflowStepInput> inputs =
 * inputRepo.findByStepId(stepId);
 * 
 * Map<String, Object> inputMap = new HashMap<>();
 * 
 * for (WorkflowStepInput input : inputs) { inputMap.put(input.getStepKey(),
 * input.getValue()); }
 * 
 * return objectMapper.writeValueAsString(inputMap);
 * 
 * } catch (Exception e) { e.printStackTrace(); return "{}"; } } private
 * List<Map<String, Object>> getDynamicTriggerFields( WorkflowStep step, String
 * stepInputJson ) { try { if (step.getTrigger() == null ||
 * !Boolean.TRUE.equals(step.getTrigger().getHasDynamicFields())) { return
 * Collections.emptyList(); }
 * 
 * String script = step.getTrigger().getScript();
 * 
 * String outputFieldsJson = scriptEngineService.executeOutputFields( script,
 * stepInputJson, step.getConnection() );
 * 
 * return objectMapper.readValue( outputFieldsJson, new
 * TypeReference<List<Map<String, Object>>>() {} );
 * 
 * } catch (Exception e) { e.printStackTrace(); return Collections.emptyList();
 * } } private void syncDynamicTriggerOutputs( WorkflowStep step, Map<String,
 * Object> filteredOutputs, Set<String> dynamicKeys, Set<String> staticKeys ) {
 * 
 * List<WorkflowStepOutput> existingOutputs =
 * outputRepo.findByStepId(step.getId());
 * 
 * Map<String, WorkflowStepOutput> existingMap = new HashMap<>();
 * 
 * for (WorkflowStepOutput o : existingOutputs) { existingMap.put(o.getKey(),
 * o); }
 * 
 * for (String key : dynamicKeys) {
 * 
 * String value = filteredOutputs.get(key) != null ?
 * filteredOutputs.get(key).toString() : "";
 * 
 * WorkflowStepOutput output;
 * 
 * if (existingMap.containsKey(key)) { output = existingMap.get(key);
 * output.setValue(value); existingMap.remove(key); } else { output = new
 * WorkflowStepOutput(key, value, step); }
 * 
 * outputRepo.save(output); }
 * 
 * for (WorkflowStepOutput leftover : existingMap.values()) {
 * 
 * String key = leftover.getKey();
 * 
 * if (staticKeys.contains(key)) continue;
 * 
 * if (!dynamicKeys.contains(key)) { outputRepo.delete(leftover); } } } private
 * String toJsonString(Object obj) { try { ObjectMapper mapper = new
 * ObjectMapper(); return mapper.writeValueAsString(obj); } catch (Exception e)
 * { return "{}"; } } }
 */
