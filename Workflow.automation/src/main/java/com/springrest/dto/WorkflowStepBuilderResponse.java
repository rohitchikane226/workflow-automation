package com.springrest.dto;

public class WorkflowStepBuilderResponse {

	private Long id;
	private int stepOrder;

	private TriggerSummaryResponse trigger;
	private ActionSummaryResponse action;
	private Long connectionId;
	private String connectionName;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public int getStepOrder() {
		return stepOrder;
	}

	public void setStepOrder(int stepOrder) {
		this.stepOrder = stepOrder;
	}

	public TriggerSummaryResponse getTrigger() {
		return trigger;
	}

	public void setTrigger(TriggerSummaryResponse trigger) {
		this.trigger = trigger;
	}

	public ActionSummaryResponse getAction() {
		return action;
	}

	public void setAction(ActionSummaryResponse action) {
		this.action = action;
	}

	public Long getConnectionId() {
		return connectionId;
	}

	public void setConnectionId(Long connectionId) {
		this.connectionId = connectionId;
	}

	public String getConnectionName() {
		return connectionName;
	}

	public void setConnectionName(String connectionName) {
		this.connectionName = connectionName;
	}
}
