//package com.springrest.Entities;
//
//import jakarta.persistence.*;
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "workflow_history")
//public class WorkflowHistory {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    private String actionKey;
//
//    @Column(columnDefinition = "TEXT")
//    private String actionValue;
//
//    @ManyToOne
//    @JoinColumn(name = "run_id")
//    private WorkflowRun run;
//
//    @ManyToOne
//    @JoinColumn(name = "step_id")
//    private WorkflowStep step;
//
//    private LocalDateTime createdAt = LocalDateTime.now();
//
//    public WorkflowHistory() {}
//
//    public WorkflowHistory(String actionKey, String actionValue, WorkflowRun run, WorkflowStep step) {
//        this.actionKey = actionKey;
//        this.actionValue = actionValue;
//        this.run = run;
//        this.step = step;
//        this.createdAt = LocalDateTime.now();
//    }
//
//    // Getters and Setters
//    public Long getId() { return id; }
//    public void setId(Long id) { this.id = id; }
//
//    public String getActionKey() { return actionKey; }
//    public void setActionKey(String actionKey) { this.actionKey = actionKey; }
//
//    public String getActionValue() { return actionValue; }
//    public void setActionValue(String actionValue) { this.actionValue = actionValue; }
//
//    public WorkflowRun getRun() { return run; }
//    public void setRun(WorkflowRun run) { this.run = run; }
//
//    public WorkflowStep getStep() { return step; }
//    public void setStep(WorkflowStep step) { this.step = step; }
//
//    public LocalDateTime getCreatedAt() { return createdAt; }
//    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
//}

package com.springrest.Entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_history", indexes = { @Index(name = "idx_history_run", columnList = "run_id"),
		@Index(name = "idx_history_step", columnList = "step_id") })
public class WorkflowHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "run_id")
	private WorkflowRun run;

	@ManyToOne
	@JoinColumn(name = "step_id")
	private WorkflowStep step;

	private String status; 

	private String errorType; 
	@Column(columnDefinition = "TEXT")
	private String requestJson;

	@Column(columnDefinition = "TEXT")
	private String responseJson;

	@Column(columnDefinition = "TEXT")
	private String errorMessage;

	private Long executionTimeMs;
	

	private LocalDateTime createdAt = LocalDateTime.now();

	public WorkflowHistory() {
	}

	public WorkflowHistory(WorkflowRun run, WorkflowStep step, String status, String errorType, String requestJson,
			String responseJson, String errorMessage, Long executionTimeMs) {
		this.run = run;
		this.step = step;
		this.status = status;
		this.errorType = errorType;
		this.requestJson = requestJson;
		this.responseJson = responseJson;
		this.errorMessage = errorMessage;
		this.executionTimeMs = executionTimeMs;
		this.createdAt = LocalDateTime.now();
	}

	// Getters and Setters

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	public WorkflowRun getRun() {
		return run;
	}

	public void setRun(WorkflowRun run) {
		this.run = run;
	}

	public WorkflowStep getStep() {
		return step;
	}

	public void setStep(WorkflowStep step) {
		this.step = step;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getErrorType() {
		return errorType;
	}

	public void setErrorType(String errorType) {
		this.errorType = errorType;
	}

	public String getRequestJson() {
		return requestJson;
	}

	public void setRequestJson(String requestJson) {
		this.requestJson = requestJson;
	}

	public String getResponseJson() {
		return responseJson;
	}

	public void setResponseJson(String responseJson) {
		this.responseJson = responseJson;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public Long getExecutionTimeMs() {
		return executionTimeMs;
	}

	public void setExecutionTimeMs(Long executionTimeMs) {
		this.executionTimeMs = executionTimeMs;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
}
