package com.springrest.kafka;

import java.io.Serializable;
import java.util.Map;

public class WorkflowEvent implements Serializable {

    private Long workflowId;
    private Map<String, Object> payload;

    // Default Constructor (IMPORTANT for Kafka)
    public WorkflowEvent() {
    }

    // Parameterized Constructor
    public WorkflowEvent(Long workflowId, Map<String, Object> payload) {
        this.workflowId = workflowId;
        this.payload = payload;
    }

    // Getter for workflowId
    public Long getWorkflowId() {
        return workflowId;
    }

    // Setter for workflowId
    public void setWorkflowId(Long workflowId) {
        this.workflowId = workflowId;
    }

    // Getter for payload
    public Map<String, Object> getPayload() {
        return payload;
    }

    // Setter for payload
    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}