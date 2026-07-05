package com.springrest.controller;

import com.springrest.Entities.WorkflowRun;
import com.springrest.Entities.WorkflowHistory;
import com.springrest.repository.WorkflowRunRepository;
import com.springrest.repository.WorkflowHistoryRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/workflow-runs")
@CrossOrigin(origins = "*")
public class WorkflowRunController {

    @Autowired
    private WorkflowRunRepository runRepo;

    @Autowired
    private WorkflowHistoryRepository historyRepo;

    @GetMapping("/workflow/{workflowId}")
    public ResponseEntity<List<WorkflowRun>> getWorkflowRuns(@PathVariable Long workflowId) {
        List<WorkflowRun> runs = runRepo.findByWorkflowIdOrderByIterationDesc(workflowId);
        return ResponseEntity.ok(runs);
    }

    @GetMapping("/{runId}/records")
    public ResponseEntity<List<WorkflowHistory>> getRunRecords(@PathVariable Long runId) {
        List<WorkflowHistory> records = historyRepo.findByRunId(runId);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/{runId}")
    public ResponseEntity<WorkflowRun> getRunDetails(@PathVariable Long runId) {
        Optional<WorkflowRun> runOpt = runRepo.findById(runId);
        return runOpt.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{runId}")
    public ResponseEntity<Void> deleteRun(@PathVariable Long runId) {
        if (!runRepo.existsById(runId)) {
            return ResponseEntity.notFound().build();
        }
        List<WorkflowHistory> records = historyRepo.findByRunId(runId);
        historyRepo.deleteAll(records);
        runRepo.deleteById(runId);
        return ResponseEntity.noContent().build();
    }
}
