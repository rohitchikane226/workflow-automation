package com.springrest.Entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "processed_records", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"workflow_id", "trigger_unique_id"})
})
public class ProcessedRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workflow_id", nullable = false)
    private Long workflowId;

    @Column(name = "trigger_unique_id", nullable = false)
    private String triggerUniqueId;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    public ProcessedRecord() {
    }

    public ProcessedRecord(Long workflowId, String triggerUniqueId, LocalDateTime processedAt) {
        this.workflowId = workflowId;
        this.triggerUniqueId = triggerUniqueId;
        this.processedAt = processedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(Long workflowId) {
        this.workflowId = workflowId;
    }

    public String getTriggerUniqueId() {
        return triggerUniqueId;
    }

    public void setTriggerUniqueId(String triggerUniqueId) {
        this.triggerUniqueId = triggerUniqueId;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}

