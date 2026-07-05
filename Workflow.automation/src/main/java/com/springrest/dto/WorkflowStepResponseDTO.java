package com.springrest.dto;

import com.springrest.Entities.Action;
import com.springrest.Entities.Connection;
import com.springrest.Entities.Trigger;
import com.springrest.Entities.WorkflowStep;

public class WorkflowStepResponseDTO {

	private Long id;
	private Integer stepOrder;
	private Action action;
	private Trigger trigger;
	private Connection connection;

	public WorkflowStepResponseDTO(WorkflowStep step) {
		this.id = step.getId();
		this.stepOrder = step.getStepOrder();
		this.action = step.getAction();
		this.trigger = step.getTrigger();
		this.connection = step.getConnection();
	}

	public Long getId() {
		return id;
	}

	public Integer getStepOrder() {
		return stepOrder;
	}

	public Action getAction() {
		return action;
	}

	public Trigger getTrigger() {
		return trigger;
	}

	public Connection getConnection() {
		return connection;
	}
}
