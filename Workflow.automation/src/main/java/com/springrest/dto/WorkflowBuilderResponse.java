package com.springrest.dto;

import java.util.List;

public class WorkflowBuilderResponse {

	private Long id;
	private String name;
	private boolean active;
	private List<WorkflowStepBuilderResponse> steps;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public List<WorkflowStepBuilderResponse> getSteps() {
		return steps;
	}

	public void setSteps(List<WorkflowStepBuilderResponse> steps) {
		this.steps = steps;
	}
}