package com.springrest.controller;

import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.springrest.Entities.WorkflowStep;
import com.springrest.Entities.WorkflowTrigger;
import com.springrest.services.WorkflowExecutionService;
import com.springrest.services.WorkflowService;

@RestController
@RequestMapping("/webhooks")
public class WebhookController {

	private final WorkflowExecutionService workflowExecutionService;
	private final WorkflowService workflowService;

	public WebhookController(WorkflowExecutionService workflowExecutionService, WorkflowService workflowService) {
		this.workflowExecutionService = workflowExecutionService;
		this.workflowService = workflowService;
	}

	@PostMapping("/{webhookUuid}")
	public ResponseEntity<String> receiveWebhook(@PathVariable String webhookUuid,
			@RequestBody Map<String, Object> payload) throws Exception {
		System.out.println("webhookUuid  "+webhookUuid);
		WorkflowTrigger workflowTrigger = workflowExecutionService.getTriggerByWebhookUuid(webhookUuid);
		Long workflowId = workflowTrigger.getWorkflow().getId();
		workflowService.storeWebhookPayload(workflowId, payload);
		if (Boolean.TRUE.equals(workflowTrigger.getWorkflow().isActive())) {
			workflowExecutionService.executeWorkflow(workflowId, payload);
		}
		return ResponseEntity.ok("Webhook processed");
	}
}
