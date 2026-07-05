package com.springrest.dto;

public class WorkflowStepInputDto {

    private Long stepId;     
    private Long inputId;     
    private String stepKey;   
    private String value;    

    public WorkflowStepInputDto() {
    }

    public WorkflowStepInputDto(Long stepId, Long inputId, String stepKey, String value) {
        this.stepId = stepId;
        this.inputId = inputId;
        this.stepKey = stepKey;
        this.value = value;
    }
    public Long getStepId() {
        return stepId;
    }

    public void setStepId(Long stepId) {
        this.stepId = stepId;
    }

    public Long getInputId() {
        return inputId;
    }

    public void setInputId(Long inputId) {
        this.inputId = inputId;
    }

    public String getStepKey() {
        return stepKey;
    }

    public void setStepKey(String stepKey) {
        this.stepKey = stepKey;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
