package com.springrest.Entities;

import jakarta.persistence.*;

//import java.util.List;
@Entity
@Table(name = "workflow_step_inputs")
public class WorkflowStepInput {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String stepKey;
	private String label;
	private boolean isRequired;
	@Column(name = "value", columnDefinition = "TEXT")
	private String value;

	@ManyToOne
	@JoinColumn(name = "step_id", nullable = false)
	private WorkflowStep step;

	public WorkflowStepInput() {
	}

	public WorkflowStepInput(String stepKey, String label, boolean isRequired, String value, WorkflowStep step) {
		this.stepKey = stepKey;
		this.label = label;
		this.isRequired = isRequired;
		this.value = value;
		this.step = step;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getStepKey() {
		return stepKey;
	}

	public void setStepKey(String stepKey) {
		this.stepKey = stepKey;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public boolean getIsRequired() {
		return isRequired;
	}

	public void setIsRequired(boolean isRequired) {
		this.isRequired = isRequired;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public WorkflowStep getStep() {
		return step;
	}

	public void setStep(WorkflowStep step) {
		this.step = step;
	}
}
