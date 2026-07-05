package com.springrest.repository;

import com.springrest.Entities.WorkflowRun;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WorkflowRunRepository extends JpaRepository<WorkflowRun, Long> {
    List<WorkflowRun> findByWorkflowIdOrderByIterationDesc(Long workflowId);
}
