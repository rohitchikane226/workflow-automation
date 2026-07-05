package com.springrest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.springrest.Entities.WorkflowStepOutput;

import java.util.List;
import java.util.Optional;

public interface WorkflowStepOutputRepository extends JpaRepository<WorkflowStepOutput, Long> {

    // Get all outputs for a specific step
    List<WorkflowStepOutput> findByStepId(Long stepId);
    @Modifying
    @Query("DELETE FROM WorkflowStepOutput o WHERE o.step.id = :stepId")
    void deleteByStepId(@Param("stepId") Long stepId);

    // Optional: fetch a specific output by step + key
    //WorkflowStepOutput findByStepIdAndActionKey(Long stepId, String actionKey);
    Optional<WorkflowStepOutput> findByStepIdAndActionKey(Long stepId, String key);
    List<WorkflowStepOutput> findAllByStepIdAndActionKey(Long stepId, String key);
}
