package com.springrest.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springrest.Entities.*;
import com.springrest.helpers.PostScriptResult;
import com.springrest.helpers.ScriptResult;
import com.springrest.httpUtils.HttpUtils;
import com.springrest.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WorkflowService {

	private final WorkflowStepRepository stepRepo;
	private final WorkflowStepOutputRepository outputRepo;
	private final WorkflowStepInputRepository inputRepo;
	private final ProcessedRecordRepository processedRecordRepo;
	private final AuthGenerationService authService;
	private final HttpUtils httpUtils;
	private final WebhookPayloadRepository webhookPayloadRepository;
	private final ScriptEngineService scriptEngineService;
	private final Map<Long, Map<String, Object>> webhookCache = new ConcurrentHashMap<>();
	private ConnectionRepository connectionRepository;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public WorkflowService(WorkflowStepRepository stepRepo, WorkflowStepOutputRepository outputRepo,
			ProcessedRecordRepository processedRecordRepo, AuthGenerationService authService,WorkflowStepInputRepository inputRepo,  HttpUtils httpUtils,
			ConnectionRepository connectionRepository, ScriptEngineService scriptEngineService,
			WebhookPayloadRepository webhookPayloadRepository) {
		this.stepRepo = stepRepo;
this.inputRepo=inputRepo;
		this.outputRepo = outputRepo;
		this.processedRecordRepo = processedRecordRepo;
		this.authService = authService;
		this.httpUtils = httpUtils;
		this.connectionRepository = connectionRepository;
		this.scriptEngineService = scriptEngineService;
		this.webhookPayloadRepository = webhookPayloadRepository;
	}

	@Transactional
	public Map<String, Object> testNewTriggerStep(Long stepId, boolean ignoreProcessed, boolean fetchWebhookPayload)
			throws IOException {

		WorkflowStep step = stepRepo.findById(stepId).orElseThrow(() -> new RuntimeException("Step not found"));
		Trigger trigger = step.getTrigger();
		if (trigger == null)
			throw new RuntimeException("Not a trigger step");

		if ("webhook".equalsIgnoreCase(trigger.getTriggerType()) && fetchWebhookPayload) {

			Map<String, Object> webhookLatestRecord = getLatestWebhookPayload(step);

			if (webhookLatestRecord == null || webhookLatestRecord.isEmpty()) {
				throw new RuntimeException("Send test webhook first");
			}
			saveTriggerOutputs(step, webhookLatestRecord);
			return webhookLatestRecord;
		}
		List<Map<String, Object>> records = fetchTriggerRecords(step);
		if (records == null || records.isEmpty())
			return Collections.emptyMap();

		String recordKey = trigger.getRecordIdentifierKey();
		Long workflowId = step.getWorkflow().getId();
		Map<String, Object> latestRecord = records.isEmpty() ? null : records.get(0);
		for (Map<String, Object> record : records) {

			String id = extractIdentifierValue(record, recordKey);
			if (id == null)
				continue;

			if (!processedRecordRepo.existsByWorkflowIdAndTriggerUniqueId(workflowId, id)) {
				processedRecordRepo.save(new ProcessedRecord(workflowId, id, LocalDateTime.now()));
			}
		}
		if (latestRecord != null) {
			saveTriggerOutputs(step, latestRecord);
		}

		return latestRecord;
	}
	private String toJsonString(Object obj) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			return mapper.writeValueAsString(obj);
		} catch (Exception e) {
			return "{}";
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
		List<WorkflowStepInput> triggerInuts=inputRepo.findByStepId(triggerStep.getId());
		Map<String, String> triggerContext = new HashMap<>();
		for (WorkflowStepInput out : triggerInuts) {
		    triggerContext.put(
		        out.getStepKey(),
		        out.getValue()
		    );
		}
		String RawBody = toJsonString(triggerContext);
		ScriptResult pre = scriptEngineService.executePreScript(script, finalUrl, "{}", RawBody, finalHeaders,
				finalQueryParams, null);
		String preBody = pre.getBody() != null ? pre.getBody() : "{}";
		finalHeaders.putAll(pre.getHeaders());
		finalQueryParams.putAll(pre.getQueryParams());
		finalUrl = pre.getUrl() != null ? pre.getUrl() : finalUrl;

		Map<String, Object> apiResponse = httpUtils.callApiWithPlacement(finalUrl, method, finalHeaders,
				finalQueryParams, preBody);
		int statusCode = (int) apiResponse.get("statusCode");
		String responseBody = (String) apiResponse.get("body");

		try {
			PostScriptResult post = scriptEngineService.executePostScript(script, statusCode, responseBody, null);
			if (!"success".equalsIgnoreCase(post.getStatus())) {
				throw new RuntimeException("Trigger API failed: " + post.getMessage());
			}

			Object parsed = objectMapper.readValue(post.getMessage(), Object.class);
			if (parsed instanceof List)
				return (List<Map<String, Object>>) parsed;
			if (parsed instanceof Map)
				return Collections.singletonList((Map<String, Object>) parsed);

		} catch (Exception e) {
			throw new RuntimeException("Failed to parse trigger response: " + e.getMessage(), e);
		}

		return Collections.emptyList();
	}

	private void saveTriggerOutputs(WorkflowStep triggerStep, Map<String, Object> record) {
	    if ("webhook".equalsIgnoreCase(triggerStep.getTrigger().getTriggerType())) {

	        for (Map.Entry<String, Object> entry : record.entrySet()) {

	            String key = entry.getKey();
	            Object value = entry.getValue();
	            if (value == null) continue;

	            List<WorkflowStepOutput> existing =
	                    outputRepo.findAllByStepIdAndActionKey(triggerStep.getId(), key);

	            WorkflowStepOutput out;

	            if (!existing.isEmpty()) {
	                out = existing.get(0);
	                out.setValue(value.toString());

	                if (existing.size() > 1) {
	                    outputRepo.deleteAll(existing.subList(1, existing.size()));
	                }

	            } else {
	                out = new WorkflowStepOutput(key, value.toString(), triggerStep);
	            }

	            outputRepo.save(out);
	        }
	        return;
	    }

	   
	    String stepInputJson = buildStepInputJson(triggerStep.getId());

	    Map<String, Object> filteredOutputs = new HashMap<>();

	    Set<String> staticKeys = new HashSet<>();

	    for (TriggerField f : triggerStep.getTrigger().getFields()) {

	        if (!"output".equalsIgnoreCase(f.getFieldType())) continue;

	        staticKeys.add(f.getKey());

	        Object value = getNestedValue(record, f.getKey());

	        if (value != null) {
	            filteredOutputs.put(f.getKey(), value);
	        }
	    }

	    List<Map<String, Object>> dynamicFields =
	            getDynamicTriggerFields(triggerStep, stepInputJson);

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

	    for (String key : staticKeys) {

	        if (!filteredOutputs.containsKey(key)) continue;

	        String value = filteredOutputs.get(key).toString();

	        Optional<WorkflowStepOutput> existing =
	                outputRepo.findByStepIdAndActionKey(triggerStep.getId(), key);

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
	
	
	private void syncDynamicTriggerOutputs(
	        WorkflowStep step,
	        Map<String, Object> filteredOutputs,
	        Set<String> dynamicKeys,
	        Set<String> staticKeys
	) {

	    List<WorkflowStepOutput> existingOutputs =
	            outputRepo.findByStepId(step.getId());

	    Map<String, WorkflowStepOutput> existingMap = new HashMap<>();

	    for (WorkflowStepOutput o : existingOutputs) {
	        existingMap.put(o.getKey(), o);
	    }

	    for (String key : dynamicKeys) {

	        String value = filteredOutputs.get(key) != null
	                ? filteredOutputs.get(key).toString()
	                : "";

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

	        if (staticKeys.contains(key)) continue;

	        if (!dynamicKeys.contains(key)) {
	            outputRepo.delete(leftover);
	        }
	    }
	}
	
	private String extractIdentifierValue(Map<String, Object> record, String recordKey) {
		Object value = getNestedValue(record, recordKey);
		return value != null ? value.toString() : null;
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

	@Transactional
	public void assignConnectionToStep(Long stepId, Long connectionId) {

		WorkflowStep step = stepRepo.findById(stepId)
				.orElseThrow(() -> new RuntimeException("Workflow step not found: " + stepId));

		Connection connection = connectionRepository.findById(connectionId)
				.orElseThrow(() -> new RuntimeException("Connection not found: " + connectionId));

		step.setConnection(connection);

		stepRepo.save(step);
	}

	public Map<String, Object> getLatestWebhookPayload(WorkflowStep step) {

		Long workflowId = step.getWorkflow().getId();

		try {

			WebhookPayload latest = webhookPayloadRepository.findTopByWorkflowIdOrderByCreatedAtDesc(workflowId);

			if (latest == null)
				return Collections.emptyMap();

			return objectMapper.readValue(latest.getPayloadJson(), Map.class);

		} catch (Exception e) {
			throw new RuntimeException("Failed to read webhook payload", e);
		}
	}

	public void storeWebhookPayload(Long workflowId, Map<String, Object> payload) {

		try {

			Map<String, Object> flattened = flattenJson(payload, "");

			String jsonPayload = objectMapper.writeValueAsString(flattened);

			WebhookPayload entity = new WebhookPayload();
			entity.setWorkflowId(workflowId);
			entity.setPayloadJson(jsonPayload);
			entity.setCreatedAt(LocalDateTime.now());

			webhookPayloadRepository.save(entity);

		} catch (Exception e) {
			throw new RuntimeException("Failed to store webhook payload", e);
		}
	}

	private Map<String, Object> flattenJson(Object obj, String prefix) {

		Map<String, Object> result = new LinkedHashMap<>();

		if (obj instanceof Map) {
			Map<String, Object> map = (Map<String, Object>) obj;

			for (Map.Entry<String, Object> entry : map.entrySet()) {
				String newKey = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
				result.putAll(flattenJson(entry.getValue(), newKey));
			}
		} else if (obj instanceof List) {
			List<?> list = (List<?>) obj;

			for (int i = 0; i < list.size(); i++) {
				String newKey = prefix + "[" + i + "]";
				result.putAll(flattenJson(list.get(i), newKey));
			}
		} else {
			result.put(prefix, obj);
		}

		return result;
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
	private List<Map<String, Object>> getDynamicTriggerFields(
	        WorkflowStep step,
	        String stepInputJson
	) {
	    try {
	    
	        if (step.getTrigger() == null ||
	            !Boolean.TRUE.equals(step.getTrigger().getHasDynamicFields())) {
	            return Collections.emptyList();
	        }

	        String script = step.getTrigger().getScript();

	        String outputFieldsJson = scriptEngineService.executeOutputFields(
	                script,
	                stepInputJson,
	                step.getConnection()
	        );

	        return objectMapper.readValue(
	                outputFieldsJson,
	                new TypeReference<List<Map<String, Object>>>() {}
	        );

	    } catch (Exception e) {
	        e.printStackTrace();
	        return Collections.emptyList();
	    }
	}
}
