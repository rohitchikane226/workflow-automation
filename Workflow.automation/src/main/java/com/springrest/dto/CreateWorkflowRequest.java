package com.springrest.dto;

public class CreateWorkflowRequest {

    private String name;
    private Long triggerId;
    private Long actionId;

    public CreateWorkflowRequest() {}

    public CreateWorkflowRequest(String name, Long triggerId, Long actionId) {
        this.name = name;
        this.triggerId = triggerId;
        this.actionId = actionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getTriggerId() {
        return triggerId;
    }

    public void setTriggerId(Long triggerId) {
        this.triggerId = triggerId;
    }

    public Long getActionId() {
        return actionId;
    }

    public void setActionId(Long actionId) {
        this.actionId = actionId;
    }
}