package com.springrest.dto;

import java.time.LocalDateTime;

public class WorkflowHistoryDTO {

    private Long id;
    private Long runId;
    private Long stepId;
    private String status;
    private String inputData;
    private String outputData;
    private LocalDateTime createdAt;
    public WorkflowHistoryDTO() {
    }

    public WorkflowHistoryDTO(Long id, Long runId, Long stepId, String status, String inputData, String outputData,LocalDateTime createdAt) {
        this.id = id;
        this.runId = runId;
        this.stepId = stepId;
        this.status = status;
        this.inputData = inputData;
        this.outputData = outputData;
        this.createdAt=createdAt;
    }
    public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRunId() {
        return runId;
    }

    public void setRunId(Long runId) {
        this.runId = runId;
    }

    public Long getStepId() {
        return stepId;
    }

    public void setStepId(Long stepId) {
        this.stepId = stepId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInputData() {
        return inputData;
    }

    public void setInputData(String inputData) {
        this.inputData = inputData;
    }

    public String getOutputData() {
        return outputData;
    }

    public void setOutputData(String outputData) {
        this.outputData = outputData;
    }
}