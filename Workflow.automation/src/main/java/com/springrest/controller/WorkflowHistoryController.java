package com.springrest.controller;

import com.springrest.Entities.WorkflowHistory;
import com.springrest.dto.WorkflowHistoryDTO;
import com.springrest.services.WorkflowHistoryService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history")
@CrossOrigin
public class WorkflowHistoryController {

	@Autowired
    private final WorkflowHistoryService historyService;

    public WorkflowHistoryController(WorkflowHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/workflow/{workflowId}")
    public List<WorkflowHistoryDTO> getWorkflowHistory(@PathVariable Long workflowId) {
        return historyService.getWorkflowHistory(workflowId);
    }

    @GetMapping("/step/{stepId}")
    public List<WorkflowHistory> getStepHistory(@PathVariable Long stepId) {
        return historyService.getStepHistory(stepId);
    }
    @GetMapping("/recent")
    public List<WorkflowHistory> getRecentHistory() {
        return historyService.getRecentHistory();
    }
}