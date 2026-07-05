//package com.springrest.dto;
//
//import java.util.Map;
//
//public class WorkflowStepRequestDTO {
//
//	private Integer stepOrder;
//	private Long triggerId;
//	private Long actionId;
//	private Long connectionId;
//
//	public WorkflowStepRequestDTO() {
//	}
//
//	public Integer getStepOrder() {
//		return stepOrder;
//	}
//
//	public void setStepOrder(Integer stepOrder) {
//		this.stepOrder = stepOrder;
//	}
//
//	public Long getTriggerId() {
//		return triggerId;
//	}
//
//	public void setTriggerId(Long triggerId) {
//		this.triggerId = triggerId;
//	}
//
//	public Long getActionId() {
//		return actionId;
//	}
//
//	public void setActionId(Long actionId) {
//		this.actionId = actionId;
//	}
//
//	public Long getConnectionId() {
//		return connectionId;
//	}
//
//	public void setConnectionId(Long connectionId) {
//		this.connectionId = connectionId;
//	}
//
//}
package com.springrest.dto;

import com.springrest.Entities.StepType;

public class WorkflowStepRequestDTO {

    private Integer stepOrder;
    private Long triggerId;
    private Long actionId;
    private Long connectionId;

    // ✅ NEW FIELDS (IMPORTANT)
    private StepType stepType;        // trigger / action / condition
    private String conditionJson;   // {"left":"x","operator":"==","right":"y"}
    private Long trueStepId;
    private Long falseStepId;

    public WorkflowStepRequestDTO() {
    }

    public Integer getStepOrder() {
        return stepOrder;
    }

    public void setStepOrder(Integer stepOrder) {
        this.stepOrder = stepOrder;
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

    public Long getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(Long connectionId) {
        this.connectionId = connectionId;
    }

    // =========================
    // ✅ NEW GETTERS / SETTERS
    // =========================

    public StepType getStepType() {
        return stepType;
    }

    public void setStepType(StepType stepType) {
        this.stepType = stepType;
    }

    public String getConditionJson() {
        return conditionJson;
    }

    public void setConditionJson(String conditionJson) {
        this.conditionJson = conditionJson;
    }

    public Long getTrueStepId() {
        return trueStepId;
    }

    public void setTrueStepId(Long trueStepId) {
        this.trueStepId = trueStepId;
    }

    public Long getFalseStepId() {
        return falseStepId;
    }

    public void setFalseStepId(Long falseStepId) {
        this.falseStepId = falseStepId;
    }
}