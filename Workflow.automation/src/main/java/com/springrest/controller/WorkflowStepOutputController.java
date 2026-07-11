package com.springrest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.springrest.Entities.*;
import com.springrest.services.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/workflow-steps/{stepId}/outputs")
public class WorkflowStepOutputController {
   private final WorkflowStepOutputService outputService;

    public WorkflowStepOutputController(WorkflowStepOutputService outputService) {
        this.outputService = outputService;
    }

    @GetMapping
    public List<WorkflowStepOutput> getOutputs(@PathVariable Long stepId) {
        return outputService.getOutputsByStep(stepId);
    }

    @PostMapping
    public WorkflowStepOutput addOutput(@PathVariable Long stepId, @RequestBody WorkflowStepOutput output) {
        return outputService.addOutput(stepId, output);
    }

    @PutMapping("/{outputId}")
    public ResponseEntity<WorkflowStepOutput> updateOutput(@PathVariable Long stepId, @PathVariable Long outputId, @RequestBody WorkflowStepOutput output) {
        return ResponseEntity.ok(outputService.updateOutput(outputId, output));
    }

    @DeleteMapping("/{outputId}")
    public ResponseEntity<Void> deleteOutput(@PathVariable Long stepId, @PathVariable Long outputId) {
        outputService.deleteOutput(outputId);
        return ResponseEntity.noContent().build();
    }
}

