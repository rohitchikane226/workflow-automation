package com.springrest.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springrest.Entities.*;
import com.springrest.helpers.PostScriptResult;
import com.springrest.helpers.ScriptResult;
import com.springrest.httpUtils.HttpUtils;
import com.springrest.repository.ActionFieldRepository;
import com.springrest.repository.ActionRepository;
import com.springrest.repository.WorkflowStepInputRepository;
import com.springrest.repository.WorkflowStepRepository;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ActionService {

	@Autowired
	private ActionRepository actionRepository;

	@Autowired
	private ActionFieldRepository actionFieldRepository;

	@Autowired
	private AuthGenerationService authGenerationService;

	@Autowired
	private ScriptEngineService scriptEngineService;

	@Autowired
	private WorkflowStepRepository workflowStepRepository;
	@Autowired
	private WorkflowStepInputRepository inputRepo;

	@Autowired
	private HttpUtils httpUtils;

	private final ObjectMapper mapper = new ObjectMapper();
	public void populateDynamicDropdown(ActionField field, Long actionId, Long workflowId, Long stepId) {

		if (!"dynamic_dropdown".equalsIgnoreCase(field.getFieldDataType())) {
			return;
		}
		WorkflowStep step = workflowStepRepository.findById(stepId)
				.orElseThrow(() -> new RuntimeException("Step not found"));

		if (!step.getWorkflow().getId().equals(workflowId)) {
			throw new RuntimeException("Step does not belong to workflow");
		}

		if (!step.getAction().getId().equals(actionId)) {
			throw new RuntimeException("Step does not belong to action");
		}

		Connection connection = step.getConnection();
		if (connection == null) {
			field.setOptions("");
			actionFieldRepository.save(field);
			return;
		}
		Action dropdownAction = actionRepository.findById(field.getDynamicDropdownActionId())
				.orElseThrow(() -> new RuntimeException("Dropdown action not found"));

		try {
	
			Map<String, Object> refreshContext = new LinkedHashMap<>();

			List<ActionField> orderedFields = step.getAction().getFields().stream()
					.sorted(Comparator.comparing(ActionField::getId)).toList();

			Map<String, WorkflowStepInput> inputMap = inputRepo.findByStepId(step.getId()).stream()
					.collect(Collectors.toMap(WorkflowStepInput::getStepKey, i -> i));

			for (ActionField f : orderedFields) {
				if (f.getId().equals(field.getId()))
					break;

				if (f.isRefresh()) {
					WorkflowStepInput input = inputMap.get(f.getKey());
					if (input != null && input.getValue() != null && !input.getValue().isBlank()) {
						refreshContext.put(f.getKey(), input.getValue());
					}
				}
			}

			String rawBody = mapper.writeValueAsString(refreshContext);
			AuthGenerationService.AuthResult auth = authGenerationService.applyAuth(connection.getId(),
					dropdownAction.getApiEndpoint());

			String finalUrl = auth.getFinalUrl();
			Map<String, String> headers = new HashMap<>(auth.getHeaders());
			Map<String, String> params = new HashMap<>(auth.getQueryParams());

			ScriptResult preScript = scriptEngineService.executePreScript(dropdownAction.getScript(), finalUrl, "{}",
					rawBody, headers, params, connection);

			headers.putAll(preScript.getHeaders());
			params.putAll(preScript.getQueryParams());
			if (preScript.getUrl() != null) {
				finalUrl = preScript.getUrl().trim();
			}
			if (finalUrl.contains("{{")) {
				field.setOptions("");
				actionFieldRepository.save(field);
				return;
			}

			Map<String, Object> apiResponse = httpUtils.callApiWithPlacement(finalUrl, dropdownAction.getHttpMethod(),
					headers, params, null);

			int statusCode = (int) apiResponse.get("statusCode");
			String responseBody = (String) apiResponse.get("body");
			PostScriptResult postScript = scriptEngineService.executePostScript(dropdownAction.getScript(), statusCode,
					responseBody, connection);

			if (!"success".equalsIgnoreCase(postScript.getStatus())) {
				throw new RuntimeException(postScript.getMessage());
			}

			List<Map<String, Object>> rows = mapper.readValue(postScript.getMessage(),
					new TypeReference<List<Map<String, Object>>>() {
					});

			String valueKey = field.getDynamicDropdownKey();
			String labelKey = field.getPlaceholder();

			List<String> options = new ArrayList<>();

			for (Map<String, Object> row : rows) {
				Object value = row.get(valueKey);
				Object label = row.get(labelKey);
				if (value != null && label != null) {
					options.add(value + "|" + label);  
				}
			}

			field.setOptions(String.join(",", options));
			actionFieldRepository.save(field);

		} catch (Exception e) {
			throw new RuntimeException("Failed to populate dynamic dropdown for field: " + field.getKey(), e);
		}
	}

	public void refreshBelowDynamicDropdowns(Long stepId, String changedFieldKey) {
		WorkflowStep step = workflowStepRepository.findById(stepId)
				.orElseThrow(() -> new RuntimeException("Step not found"));

		List<ActionField> fields = step.getAction().getFields().stream()
				.sorted(Comparator.comparing(ActionField::getId)).toList();

		Map<String, ActionField> fieldMap = fields.stream().collect(Collectors.toMap(ActionField::getKey, f -> f));

		Map<String, List<String>> graph = buildDependencyGraph(fields);

		Set<String> toRefresh = new LinkedHashSet<>();
		collectDependentKeys(changedFieldKey, graph, toRefresh);

		for (String key : toRefresh) {
			ActionField field = fieldMap.get(key);
			if (field != null && "dynamic_dropdown".equalsIgnoreCase(field.getFieldDataType())) {

				populateDynamicDropdown(field, step.getAction().getId(), step.getWorkflow().getId(), step.getId());
			}
		}
	}

	@Transactional
	public String populateDynamicInputFields(Long stepId, String changedFieldKey) {
		WorkflowStep step = workflowStepRepository.findById(stepId)
				.orElseThrow(() -> new RuntimeException("Step not found"));

		Action action = step.getAction();
		if (action == null || !Boolean.TRUE.equals(action.isHasDynamicFields())) {
			return "[]";
		}

		if (changedFieldKey != null) {
			clearDependentStepInputs(stepId, changedFieldKey);
		}

		try {
			Map<String, Object> params = buildParamsForDynamicFields(step, changedFieldKey);

			return scriptEngineService.executeInputFields(action.getScript(), mapper.writeValueAsString(params),
					step.getConnection());
		} catch (Exception e) {
			e.printStackTrace();
			return "[]";
		}
	}

	private Map<String, Object> buildParamsForDynamicFields(WorkflowStep step, String changedFieldKey) {
		Map<String, Object> params = new LinkedHashMap<>();
		List<ActionField> orderedFields = actionFieldRepository.findByActionId(step.getAction().getId()).stream()
				.sorted(Comparator.comparing(ActionField::getId)).toList();
		Map<String, WorkflowStepInput> inputMap = inputRepo.findByStepId(step.getId()).stream()
				.collect(Collectors.toMap(WorkflowStepInput::getStepKey, i -> i, (oldVal, newVal) -> newVal));

		for (ActionField field : orderedFields) {

			String key = field.getKey();
			if (changedFieldKey != null && key.equals(changedFieldKey)) {
				WorkflowStepInput input = inputMap.get(key);
				if (input != null && input.getValue() != null) {
					params.put(key, input.getValue());
				}
				break;
			}
			if (field.isRefresh()) {
				WorkflowStepInput input = inputMap.get(key);
				if (input != null && input.getValue() != null && !input.getValue().isBlank()) {
					params.put(key, input.getValue());
				}
			}
		}

		return params;
	}
	private Map<String, List<String>> buildDependencyGraph(List<ActionField> fields) {
		Map<String, List<String>> graph = new HashMap<>();

		List<ActionField> ordered = fields.stream().sorted(Comparator.comparing(ActionField::getId)).toList();

		ActionField lastRefreshField = null;

		for (ActionField field : ordered) {

			if (lastRefreshField != null && "dynamic_dropdown".equalsIgnoreCase(field.getFieldDataType())) {

				graph.computeIfAbsent(lastRefreshField.getKey(), k -> new ArrayList<>()).add(field.getKey());
			}

			if (field.isRefresh()) {
				lastRefreshField = field;
			}
		}
		return graph;
	}

	private void collectDependentKeys(String currentKey, Map<String, List<String>> graph, Set<String> result) {
		List<String> children = graph.get(currentKey);
		if (children == null)
			return;

		for (String child : children) {
			if (result.add(child)) {
				collectDependentKeys(child, graph, result);
			}
		}
	}

	@Transactional
	public void clearDependentStepInputs(Long stepId, String changedFieldKey) {
		WorkflowStep step = workflowStepRepository.findById(stepId)
				.orElseThrow(() -> new RuntimeException("Step not found"));

		List<ActionField> fields = actionFieldRepository.findByActionId(step.getAction().getId());
		Map<String, List<String>> graph = buildDependencyGraph(fields);
		Set<String> dependentKeys = new HashSet<>();
		collectDependentKeys(changedFieldKey, graph, dependentKeys);
		if (!dependentKeys.isEmpty()) {
			inputRepo.deleteByStepIdAndStepKeyIn(stepId, dependentKeys);
			for (ActionField field : fields) {
				if (dependentKeys.contains(field.getKey())
						&& "dynamic_dropdown".equalsIgnoreCase(field.getFieldDataType())) {

					field.setOptions("");
					actionFieldRepository.save(field);
				}
			}
		}

	}
	@Transactional
	public String populateDynamicOutputFields(Long stepId, String changedFieldKey) {

	    WorkflowStep step = workflowStepRepository.findById(stepId)
	            .orElseThrow(() -> new RuntimeException("Step not found"));

	    Action action = step.getAction();

	    if (action == null || !Boolean.TRUE.equals(action.isHasDynamicFields())) {
	        return "[]";
	    }

	    try {
	  
	        Map<String, Object> params = buildParamsForDynamicFields(step, changedFieldKey);

	        return scriptEngineService.executeOutputFields(
	                action.getScript(),
	                mapper.writeValueAsString(params),
	                step.getConnection()
	        );

	    } catch (Exception e) {
	        e.printStackTrace();
	        return "[]";
	    }
	}
	public String refreshDynamicOutputFields(Long stepId, String changedFieldKey) {
	    return populateDynamicOutputFields(stepId, changedFieldKey);
	}
	

};
