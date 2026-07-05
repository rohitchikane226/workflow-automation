package com.springrest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.springrest.Entities.WorkflowStepInput;

import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Set;

public interface WorkflowStepInputRepository extends JpaRepository<WorkflowStepInput, Long> {

    // Get all inputs for a specific step
    List<WorkflowStepInput> findByStepId(Long stepId);

    // Optional: fetch by step + key (useful when resolving mappings)
    WorkflowStepInput findByStepIdAndStepKey(Long stepId, String stepKey);
    @Modifying
    @Query("DELETE FROM WorkflowStepInput w WHERE w.step.id = :stepId AND w.stepKey IN :keys")
    void deleteByStepIdAndStepKeyIn(
            @Param("stepId") Long stepId,
            @Param("keys") Set<String> keys
    );
//    @Modifying(clearAutomatically = true, flushAutomatically = true)
//    @Transactional
//    @Query("""
//        delete from WorkflowStepInput i
//        where i.step.id = :stepId
//          and i.stepKey in :keys
//    """)
//    void deleteByStepIdAndStepKeyIn(
//            @Param("stepId") Long stepId,
//            @Param("keys") Set<String> keys
//    );

          
}