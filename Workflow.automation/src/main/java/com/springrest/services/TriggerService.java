package com.springrest.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springrest.Entities.*;
import com.springrest.helpers.PostScriptResult;
import com.springrest.helpers.ScriptResult;
import com.springrest.httpUtils.HttpUtils;
import com.springrest.repository.*;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TriggerService {

	@Autowired
	private ActionRepository actionRepository;
	@Autowired
	private WebhookService webhookService;

	@Autowired
	private TriggerFieldRepository triggerFieldRepository;

	@Autowired
	private WorkflowStepRepository stepRepo;

	@Autowired
	private WorkflowStepInputRepository inputRepo;

	@Autowired
	private ScriptEngineService scriptEngineService;

	@Autowired
	private AuthGenerationService authService;

	@Autowired
	private HttpUtils httpUtils;

	private final ObjectMapper mapper = new ObjectMapper();
	public void populateDynamicDropdown(TriggerField field, Long triggerId, Long workflowId, Long stepId) {

		if (!"dynamic_dropdown".equalsIgnoreCase(field.getFieldDataType()))
			return;

		WorkflowStep step = stepRepo.findById(stepId).orElseThrow(() -> new RuntimeException("Step not found"));

		if (!step.getWorkflow().getId().equals(workflowId))
			throw new RuntimeException("Step not part of workflow");

		Connection connection = step.getConnection();
		if (connection == null) {
			field.setOptions("");
			triggerFieldRepository.save(field);
			return;
		}

		try {
			Map<String, Object> rawMap = buildParentContext(step, field);

			String rawBody = mapper.writeValueAsString(rawMap);
			Action dropdownAction = actionRepository
			        .findById(field.getDynamicDropdownActionId())
			        .orElseThrow(() -> new RuntimeException("Dropdown action not found"));
			AuthGenerationService.AuthResult auth = authService.applyAuth(connection.getId(), dropdownAction.getApiEndpoint());

			String url = auth.getFinalUrl();
			Map<String, String> headers = new HashMap<>(auth.getHeaders());
			Map<String, String> params = new HashMap<>(auth.getQueryParams());

			ScriptResult pre = scriptEngineService.executePreScript(dropdownAction.getScript(), url, "{}", rawBody, headers,
					params, connection);

			headers.putAll(pre.getHeaders());
			params.putAll(pre.getQueryParams());
			if (pre.getUrl() != null)
				url = pre.getUrl().trim();

			if (url.contains("{{")) {
				field.setOptions("");
				triggerFieldRepository.save(field);
				return;
			}

			Map<String, Object> apiRes = httpUtils.callApiWithPlacement(url, dropdownAction.getHttpMethod(), headers, params,
					null);
			int status = (int) apiRes.get("statusCode");
			String body = (String) apiRes.get("body");
			PostScriptResult post = scriptEngineService.executePostScript(dropdownAction.getScript(), status, body,
					connection);

			if (!"success".equalsIgnoreCase(post.getStatus()))
				throw new RuntimeException(post.getMessage());
			List<Map<String, Object>> rows = mapper.readValue(post.getMessage(),
					new TypeReference<List<Map<String, Object>>>() {
					});
			System.out.println("rows "+rows.toString());
			List<String> options = new ArrayList<>();

			for (Map<String, Object> r : rows) {

				Object val = r.get(field.getDynamicDropdownKey());
				Object label = r.get(field.getPlaceholder());

				if (val != null && label != null)
					options.add(val + "|" + label);
			}

			field.setOptions(String.join(",", options));
			triggerFieldRepository.save(field);

		} catch (Exception e) {
			throw new RuntimeException("Trigger dropdown failed", e);
		}
	}
	
	
	public void refreshDependentFields(Long stepId, String changedKey) {

	    WorkflowStep step = stepRepo.findById(stepId).orElseThrow();

	    List<TriggerField> fields = step.getTrigger().getFields().stream()
	            .sorted(Comparator.comparing(TriggerField::getId))
	            .toList();

	    Map<String, TriggerField> map =
	            fields.stream().collect(Collectors.toMap(TriggerField::getKey, f -> f));

	    TriggerField changedField = map.get(changedKey);

	    if (changedField != null
	            && changedField.isRefresh()
	            && "dynamic_dropdown".equalsIgnoreCase(changedField.getFieldDataType())
	            && "webhook".equalsIgnoreCase(step.getTrigger().getTriggerType())) {

	        WorkflowStepInput input =
	                inputRepo.findByStepIdAndStepKey(stepId, changedKey);

	        if (input != null && input.getValue() != null && !input.getValue().isBlank()) {
	            List<WorkflowStepInput> inputs =
	                    inputRepo.findByStepId(stepId);

	            Map<String,Object> triggerFields = new HashMap<>();

	            for (WorkflowStepInput i : inputs) {
	                triggerFields.put(i.getStepKey(), i.getValue());
	            }
	            try {
					webhookService.subscribe(
					        step.getWorkflow(),
					        step.getTrigger(),
					        step.getConnection(),
					        triggerFields
					);
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}
	        }
	    }
	}

	private Map<String, Object> buildParentContext(WorkflowStep step, TriggerField current) {

		Map<String, Object> map = new LinkedHashMap<>();

		List<TriggerField> ordered = step.getTrigger().getFields().stream()
				.sorted(Comparator.comparing(TriggerField::getId)).toList();

		Map<String, WorkflowStepInput> inputs = inputRepo.findByStepId(step.getId()).stream()
				.collect(Collectors.toMap(WorkflowStepInput::getStepKey, i -> i));

		for (TriggerField f : ordered) {

			if (f.getId().equals(current.getId()))
				break;

			if (f.isRefresh()) {
				WorkflowStepInput input = inputs.get(f.getKey());

				if (input != null && input.getValue() != null && !input.getValue().isBlank()) {

					map.put(f.getKey(), input.getValue());
				}
			}
		}
		return map;
	}

	private Map<String, List<String>> buildDependencyGraph(List<TriggerField> fields) {

	    Map<String, List<String>> graph = new HashMap<>();

	    List<TriggerField> ordered = fields.stream()
	            .sorted(Comparator.comparing(TriggerField::getId))
	            .toList();

	    TriggerField lastRefresh = null;

	    for (TriggerField f : ordered) {

	        if (lastRefresh != null &&
	            "dynamic_dropdown".equalsIgnoreCase(f.getFieldDataType())) {

	            graph.computeIfAbsent(lastRefresh.getKey(), k -> new ArrayList<>())
	                 .add(f.getKey());
	        }

	        if (f.isRefresh()) {
	            lastRefresh = f;
	        }
	    }

	    return graph;
	}


	private void collectDependent(String key, Map<String, List<String>> graph, Set<String> out) {

		List<String> children = graph.get(key);
		if (children == null)
			return;

		for (String c : children) {
			if (out.add(c))
				collectDependent(c, graph, out);
		}
	}

	@Transactional
	public void clearDependentInputs(Long stepId, String changedKey) {

		WorkflowStep step = stepRepo.findById(stepId).orElseThrow();

		List<TriggerField> fields = triggerFieldRepository.findByTriggerId(step.getTrigger().getId());

		Map<String, List<String>> graph = buildDependencyGraph(fields);

		Set<String> keys = new HashSet<>();
		collectDependent(changedKey, graph, keys);

		if (keys.isEmpty())
			return;

		inputRepo.deleteByStepIdAndStepKeyIn(stepId, keys);

		for (TriggerField f : fields) {
			if (keys.contains(f.getKey()) && "dynamic_dropdown".equalsIgnoreCase(f.getFieldDataType())) {

				f.setOptions("");
				triggerFieldRepository.save(f);
			}
		}
	}
}
