package com.springrest.Entities;
import jakarta.persistence.*;

@Entity
@Table(name = "workflow_step_outputs")
public class WorkflowStepOutput {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String actionKey;
	@Column(length = 700, columnDefinition = "LONGTEXT")

	private String actionValue; // actual output value from API response

	@ManyToOne
	@JoinColumn(name = "step_id", nullable = false)
	private WorkflowStep step;

	public WorkflowStepOutput() {
	}
	public WorkflowStepOutput(String key, String value, WorkflowStep step) {
		this.actionKey = key;
		this.actionValue = value;
		this.step = step;
	}
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getKey() {
		return actionKey;
	}

	public void setKey(String key) {
		this.actionKey = key;
	}

	public String getValue() {
		return actionValue;
	}

	public void setValue(String value) {
		this.actionValue = value;
	}

	public WorkflowStep getStep() {
		return step;
	}

	public void setStep(WorkflowStep step) {
		this.step = step;
	}
}
