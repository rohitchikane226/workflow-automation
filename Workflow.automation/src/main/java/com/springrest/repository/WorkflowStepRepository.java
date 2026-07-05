package com.springrest.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.*;

import com.springrest.Entities.WorkflowStep;

import java.util.List;
import java.util.Optional;

public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, Long> {

    // Find all steps belonging to a specific workflow
    List<WorkflowStep> findByWorkflowId(Long workflowId);

    Optional<WorkflowStep> findById(Long id);
    @Query("SELECT ws FROM WorkflowStep ws WHERE ws.action.id = :actionId")
    Optional<WorkflowStep> findByActionId(@Param("actionId") Long actionId);
    @Query("SELECT ws FROM WorkflowStep ws WHERE ws.action.id = :actionId AND ws.workflow.id = :workflowId")
    Optional<WorkflowStep> findByActionIdAndWorkflowId(
            @Param("actionId") Long actionId,
            @Param("workflowId") Long workflowId
    );
    @Query("""
    	    SELECT DISTINCT ws FROM WorkflowStep ws
    	    LEFT JOIN FETCH ws.trigger t
    	    LEFT JOIN FETCH t.fields tf
    	    LEFT JOIN FETCH ws.connection c
    	    LEFT JOIN FETCH c.connector conn
    	    WHERE ws.workflow.id = :workflowId
    	    ORDER BY ws.stepOrder ASC
    	""")
	List<WorkflowStep> findFullStepsByWorkflowId(Long workflowId);

    
    List<WorkflowStep> findByWorkflowIdOrderByStepOrderAsc(Long workflowId);
    // Optional: find step by workflow and step id (if needed)
    // WorkflowStep findByIdAndWorkflowId(Long stepId, Long workflowId);
//	 Optional<WorkflowStep> findByWebhookId(String webhookId);
}
