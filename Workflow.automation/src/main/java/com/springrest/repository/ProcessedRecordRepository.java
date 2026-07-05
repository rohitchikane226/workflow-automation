package com.springrest.repository;

import com.springrest.Entities.ProcessedRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedRecordRepository extends JpaRepository<ProcessedRecord, Long> {

    boolean existsByWorkflowIdAndTriggerUniqueId(Long workflowId, String triggerUniqueId);
}
