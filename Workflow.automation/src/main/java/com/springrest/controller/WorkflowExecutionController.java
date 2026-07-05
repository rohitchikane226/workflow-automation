package com.springrest.controller;

import org.springframework.web.bind.annotation.*;

import com.springrest.services.WorkflowExecutionService;

import org.springframework.http.ResponseEntity;

//import com.springrest.service.WorkflowExecutionService;

import java.util.Map;

@RestController
@RequestMapping("/api/workflow-execution")
public class WorkflowExecutionController {

    private final WorkflowExecutionService workflowExecutionService;

    public WorkflowExecutionController(WorkflowExecutionService workflowExecutionService) {
        this.workflowExecutionService = workflowExecutionService;
    }

    // Run a workflow by ID
    @PostMapping("/{workflowId}/run")
    public ResponseEntity<Map<String, Object>> runWorkflow(@PathVariable Long workflowId) throws Exception {
        Map<String, Object> result = workflowExecutionService.executeWorkflow(workflowId,null);
        return ResponseEntity.ok(result);
    }
}
