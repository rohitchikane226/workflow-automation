package com.springrest.controller;

import com.springrest.services.WebhookService;
import com.springrest.services.WorkflowExecutionService;
import com.springrest.services.WorkflowService;

import jakarta.transaction.Transactional;

import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.springrest.Entities.*;
import com.springrest.dto.CreateWorkflowRequest;
import com.springrest.dto.WorkflowBuilderResponse;
import com.springrest.dto.WorkflowStepRequestDTO;
import com.springrest.mapper.WorkflowBuilderMapper;
import com.springrest.repository.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

	private final WorkflowRepository workflowRepo;
	private final WorkflowStepRepository stepRepo;
	private final TriggerRepository triggerRepo;
	private final ActionRepository actionRepo;
	private final ConnectionRepository connectionRepo;
	private final WorkflowService workflowService;
	private final WebhookService webhookService;
	private final WorkflowExecutionService workflowExecutionService;
	@Autowired
	private UserRepository userRepo;
	public WorkflowController(WorkflowRepository workflowRepo, WorkflowStepRepository stepRepo,
			TriggerRepository triggerRepo, ActionRepository actionRepo,
			WorkflowExecutionService workflowExecutionService, ConnectionRepository connectionRepo,
			WorkflowService workflowService, WebhookService webhookService) {
		this.workflowRepo = workflowRepo;
		this.stepRepo = stepRepo;
		this.triggerRepo = triggerRepo;
		this.actionRepo = actionRepo;
		this.workflowExecutionService = workflowExecutionService;
		this.workflowService = workflowService;
		this.connectionRepo = connectionRepo;
		this.webhookService = webhookService;
	}

	@GetMapping
	public List<Workflow> getAllWorkflows() {
	    User user = getLoggedInUser();
	    return workflowRepo.findByUser(user);
	}

//    @GetMapping("/{id}")
//    //@Transactional
//    public ResponseEntity<Workflow> getWorkflow(@PathVariable Long id) {
//        return workflowRepo.findById(id)
//                .map(ResponseEntity::ok)
//                .orElse(ResponseEntity.notFound().build());
//    }

	@GetMapping("builder/{id}")
	public WorkflowBuilderResponse getWorkflowForBuilder(@PathVariable Long id) {

	    User user = getLoggedInUser();

	    Workflow wf = workflowRepo.findById(id)
	            .filter(w -> w.getUser().getId().equals(user.getId()))
	            .orElseThrow(() -> new RuntimeException("Workflow not found or access denied"));

	    return WorkflowBuilderMapper.toDto(wf);
	}

	@PostMapping
	public Workflow createWorkflow(@RequestBody CreateWorkflowRequest request) {

	    User user = getLoggedInUser();

	    Workflow workflow = new Workflow();
	    workflow.setName(request.getName());
	    workflow.setActive(false);
	    workflow.setUser(user); 

	    return workflowRepo.save(workflow);
	}

	@PutMapping("/{id}")
	public ResponseEntity<Workflow> updateWorkflow(@PathVariable Long id, @RequestBody Workflow workflow) {

	    User user = getLoggedInUser();

	    return workflowRepo.findById(id)
	            .filter(w -> w.getUser().getId().equals(user.getId()))
	            .map(existing -> {
	                workflow.setId(id);
	                workflow.setUser(user); // 🔥 maintain ownership
	                return ResponseEntity.ok(workflowRepo.save(workflow));
	            })
	            .orElse(ResponseEntity.status(403).build()); // 🔥 forbidden
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Object> deleteWorkflow(@PathVariable Long id) {

	    User user = getLoggedInUser();

	    return workflowRepo.findById(id)
	            .filter(w -> w.getUser().getId().equals(user.getId()))
	            .map(existing -> {
	                workflowRepo.deleteById(id);
	                return ResponseEntity.noContent().build();
	            })
	            .orElse(ResponseEntity.status(403).build());
	}
//	@PostMapping("/{workflowId}/steps")
//	public ResponseEntity<WorkflowStep> addStep(@PathVariable Long workflowId,
//			@RequestBody WorkflowStepRequestDTO dto) {
//
//		workflowRepo.findById(workflowId)
//		        .filter(w -> w.getUser().getId().equals(getLoggedInUser().getId()))
//		        .orElseThrow(() -> new RuntimeException("Access denied"));
//		WorkflowStep step = new WorkflowStep();
//		step.setStepOrder(dto.getStepOrder());
//
//		// Load and attach Trigger
//		if (dto.getTriggerId() != null) {
//			Trigger trigger = triggerRepo.findById(dto.getTriggerId())
//					.orElseThrow(() -> new RuntimeException("Trigger not found"));
//			step.setTrigger(trigger);
//		}
//
//		// Load and attach Action
//		if (dto.getActionId() != null) {
//			Action action = actionRepo.findById(dto.getActionId())
//					.orElseThrow(() -> new RuntimeException("Action not found"));
//			step.setAction(action);
//		}
//
//		// Load and attach Connection
//		if (dto.getConnectionId() != null) {
//			Connection connection = connectionRepo.findById(dto.getConnectionId())
//					.orElseThrow(() -> new RuntimeException("Connection not found"));
//			step.setConnection(connection);
//		}
//
//		// Config map
////        step.setConfig(dto.getConfig());
//
//		// Set workflow parent
//		Workflow workflow = workflowRepo.findById(workflowId)
//				.orElseThrow(() -> new RuntimeException("Workflow not found"));
//		step.setWorkflow(workflow);
//
//		// Save step
//		WorkflowStep saved = stepRepo.save(step);
//		return ResponseEntity.ok(saved);
//	}
	
	
	
	@PostMapping("/{workflowId}/steps")
	public ResponseEntity<WorkflowStep> addStep(
	        @PathVariable Long workflowId,
	        @RequestBody WorkflowStepRequestDTO dto) {

	    workflowRepo.findById(workflowId)
	            .filter(w -> w.getUser().getId().equals(getLoggedInUser().getId()))
	            .orElseThrow(() -> new RuntimeException("Access denied"));

	    WorkflowStep step = new WorkflowStep();

	    step.setStepOrder(dto.getStepOrder());

	    // ✅ SAVE STEP TYPE
	    

	    // =========================
	    // ✅ CONDITION STEP SUPPORT
	    // =========================
	    if (dto.getStepType()==StepType.CONDITION) {
	    	step.setStepType(dto.getStepType());
	        step.setConditionJson(dto.getConditionJson());

	        if (dto.getTrueStepId() != null) {
	            WorkflowStep trueStep = stepRepo.findById(dto.getTrueStepId()).orElse(null);
	            step.setTrueStepId(trueStep.getTrueStepId());
	        }

	        if (dto.getFalseStepId() != null) {
	            WorkflowStep falseStep = stepRepo.findById(dto.getFalseStepId()).orElse(null);
	            step.setFalseStepId(falseStep.getFalseStepId());
	        }

	    } else {
	        if (dto.getTriggerId() != null) {
	            Trigger trigger = triggerRepo.findById(dto.getTriggerId())
	                    .orElseThrow(() -> new RuntimeException("Trigger not found"));
	            step.setTrigger(trigger);
	        }
	        if (dto.getActionId() != null) {
	            Action action = actionRepo.findById(dto.getActionId())
	                    .orElseThrow(() -> new RuntimeException("Action not found"));
	            step.setAction(action);
	        }
	        if (dto.getConnectionId() != null) {
	            Connection connection = connectionRepo.findById(dto.getConnectionId())
	                    .orElseThrow(() -> new RuntimeException("Connection not found"));
	            step.setConnection(connection);
	        }
	    }
	    Workflow workflow = workflowRepo.findById(workflowId)
	            .orElseThrow(() -> new RuntimeException("Workflow not found"));

	    step.setWorkflow(workflow);

	    WorkflowStep saved = stepRepo.save(step);

	    return ResponseEntity.ok(saved);
	}

	@GetMapping("/{workflowId}/steps")
	public List<WorkflowStep> getWorkflowSteps(@PathVariable Long workflowId) {

	    Workflow wf = workflowRepo.findById(workflowId)
	            .filter(w -> w.getUser().getId().equals(getLoggedInUser().getId()))
	            .orElseThrow(() -> new RuntimeException("Access denied"));

	    return stepRepo.findByWorkflowId(wf.getId());
	}

	@PutMapping("/{workflowId}/steps/{stepId}")
	public ResponseEntity<WorkflowStep> updateWorkflowStep(@PathVariable Long workflowId, @PathVariable Long stepId,
			@RequestBody WorkflowStep step) {
		return stepRepo.findById(stepId).filter(existing -> existing.getWorkflow().getId().equals(workflowId))
				.map(existing -> {

					step.setId(stepId);
					step.setWorkflow(existing.getWorkflow());

					// update trigger
					if (step.getTrigger() != null && step.getTrigger().getId() != null) {
						Trigger t = triggerRepo.findById(step.getTrigger().getId()).orElse(null);
						step.setTrigger(t);
					}

					// update action
					if (step.getAction() != null && step.getAction().getId() != null) {
						Action a = actionRepo.findById(step.getAction().getId()).orElse(null);
						step.setAction(a);
					}

					// update connection
					if (step.getConnection() != null && step.getConnection().getId() != null) {
						Connection c = connectionRepo.findById(step.getConnection().getId()).orElse(null);
						step.setConnection(c);
					}

					return ResponseEntity.ok(stepRepo.save(step));
				}).orElse(ResponseEntity.notFound().build());
	}

	@DeleteMapping("/{workflowId}/steps/{stepId}")
	public ResponseEntity<Object> deleteWorkflowStep(@PathVariable Long workflowId, @PathVariable Long stepId) {
		return stepRepo.findById(stepId).filter(existing -> existing.getWorkflow().getId().equals(workflowId))
				.map(existing -> {
					stepRepo.delete(existing);
					return ResponseEntity.noContent().build();
				}).orElse(ResponseEntity.notFound().build());
	}

	@PutMapping("/{workflowId}/activate")
	public ResponseEntity<?> activateWorkflow(@PathVariable Long workflowId) {
		Workflow wf = workflowRepo.findById(workflowId).orElseThrow();
		wf.setActive(true);
		workflowRepo.save(wf);
		return ResponseEntity.ok("Workflow activated");
	}

	@PutMapping("/{workflowId}/deactivate")
	public ResponseEntity<?> deactivateWorkflow(@PathVariable Long workflowId) {
		Workflow wf = workflowRepo.findById(workflowId).orElseThrow();
		wf.setActive(false);
		workflowRepo.save(wf);
		return ResponseEntity.ok("Workflow deactivated");
	}
	// ======================= CONNECTION CRUD =======================

	@GetMapping("/connections")
	public List<Connection> getAllConnections() {
		return connectionRepo.findAll();
	}

	@GetMapping("/connections/{id}")
	public ResponseEntity<Connection> getConnection(@PathVariable Long id) {
		return connectionRepo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
	}

	@PostMapping("/connections")
	public Connection createConnection(@RequestBody Connection connection) {
		if (connection.getConnector() != null && connection.getConnector().getId() != null) {
			Connector conn = connectionRepo.findConnectorById(connection.getConnector().getId());
			connection.setConnector(conn);
		}

		return connectionRepo.save(connection);
	}

	@PutMapping("/connections/{id}")
	public ResponseEntity<Connection> updateConnection(@PathVariable Long id, @RequestBody Connection connection) {
		return connectionRepo.findById(id).map(existing -> {
			connection.setId(id);

			// update connector
			if (connection.getConnector() != null && connection.getConnector().getId() != null) {
				Connector conn = connectionRepo.findConnectorById(connection.getConnector().getId());
				connection.setConnector(conn);
			}

			return ResponseEntity.ok(connectionRepo.save(connection));
		}).orElse(ResponseEntity.notFound().build());
	}

	@DeleteMapping("/connections/{id}")
	public ResponseEntity<?> deleteConnection(@PathVariable Long id) {
		return connectionRepo.findById(id).map(existing -> {
			connectionRepo.delete(existing);
			return ResponseEntity.noContent().build();
		}).orElse(ResponseEntity.notFound().build());
	}
	// ======================= GET STEP CONNECTION =======================

	// ======================= GET STEP CONNECTION =======================

	@GetMapping("/{workflowId}/steps/{stepId}/connection")
	public ResponseEntity<Connection> getStepConnection(@PathVariable Long workflowId, @PathVariable Long stepId) {

		return stepRepo.findById(stepId).filter(step -> step.getWorkflow().getId().equals(workflowId))
				.map(WorkflowStep::getConnection).map(ResponseEntity::ok) // wrap Connection in ResponseEntity.ok()
				.orElseGet(() -> ResponseEntity.notFound().build()); // if step or connection is null
	}
	// ======================= STEP CONNECTION =======================

	@PostMapping("/{workflowId}/steps/{stepId}/connection")
	public ResponseEntity<Map<String, Object>> addConnectionToStep(
	        @PathVariable Long workflowId,
	        @PathVariable Long stepId,
	        @RequestBody Map<String, Object> payload) {

	    Long connectionId = Long.valueOf(String.valueOf(payload.get("id")));

	    WorkflowStep step = stepRepo.findById(stepId)
	            .filter(s -> s.getWorkflow().getId().equals(workflowId))
	            .orElseThrow();

	    if (connectionId != null) {

	        Connection connection = connectionRepo.findById(connectionId).orElseThrow();

	        step.setConnection(connection);
	        stepRepo.save(step);

	        if (step.getTrigger() != null &&
	                "webhook".equals(step.getTrigger().getTriggerType())) {

	            try {
	                webhookService.subscribe(
	                        step.getWorkflow(),
	                        step.getTrigger(),
	                        connection,
	                        Collections.emptyMap()
	                );
	            } catch (JsonProcessingException e) {
	                e.printStackTrace();
	            }
	        }

	        Map<String, Object> response = new HashMap<>();
	        response.put("success", true);
	        response.put("message", "Connection added successfully.");
	        response.put("connectionId", connection.getId());
	        response.put("stepId", step.getId());

	        return ResponseEntity.ok(response);
	    }

	    return ResponseEntity.badRequest().body(
	            Map.of(
	                    "success", false,
	                    "message", "Invalid connection id."
	            )
	    );
	}

	@PutMapping("/{workflowId}/steps/{stepId}/connection")
	public ResponseEntity<WorkflowStep> updateStepConnection(@PathVariable Long workflowId, @PathVariable Long stepId,
			@RequestBody Connection connectionPayload) {

		// Find step by id and workflow
		return stepRepo.findById(stepId).filter(s -> s.getWorkflow().getId().equals(workflowId)).map(step -> {
			if (connectionPayload.getId() == null) {
				return null;
			}

			// Find the connection
			Connection connection = connectionRepo.findById(connectionPayload.getId())
					.orElseThrow(() -> new RuntimeException("Connection not found"));

			step.setConnection(connection);
			stepRepo.save(step);
			return step;
		}).map(ResponseEntity::ok) // convert the step to ResponseEntity
				.orElseGet(() -> {
					if (connectionPayload.getId() == null) {
						return ResponseEntity.badRequest().build();
					}
					return ResponseEntity.notFound().build();
				});
	}
	// endpoint for testting step

	@PostMapping("/steps/{stepId}/test-trigger")
	public ResponseEntity<?> testTriggerStep(@PathVariable Long stepId) {
		try {
			Map<String, Object> records = workflowService.testNewTriggerStep(stepId, true,true);
			return ResponseEntity.ok(records);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
		}
	}

	@PostMapping("/steps/{stepId}/test")
	public ResponseEntity<Map<String, Object>> testStep(@PathVariable Long stepId) {
		try {
			// 1️⃣ Fetch the step
			WorkflowStep step = stepRepo.findById(stepId).orElseThrow(() -> new RuntimeException("Step not found"));

			// 2️⃣ Test the step (includes seeding trigger + previous step outputs)
			Map<String, Object> result = workflowExecutionService.testStep(step.getId());

			return ResponseEntity.ok(result);

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(Map.of("status", "FAILED", "error", e.getMessage()));
		}
	}
	private User getLoggedInUser() {
	    String email = org.springframework.security.core.context.SecurityContextHolder
	            .getContext()
	            .getAuthentication()
	            .getName();

	    return userRepo.findByEmail(email);
	}

}
