package com.springrest.repository;

import com.springrest.Entities.WorkflowHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WorkflowHistoryRepository extends JpaRepository<WorkflowHistory, Long> {

    List<WorkflowHistory> findByStepWorkflowIdOrderByCreatedAtDesc(Long workflowId);

    List<WorkflowHistory> findByStepIdOrderByCreatedAtDesc(Long stepId);
    List<WorkflowHistory> findByRunId(Long runId);
    List<WorkflowHistory> findTop50ByOrderByCreatedAtDesc();
}