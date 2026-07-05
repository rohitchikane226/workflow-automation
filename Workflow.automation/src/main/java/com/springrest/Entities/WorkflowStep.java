package com.springrest.Entities;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

//@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
//@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "workflow_steps")
public class WorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer stepOrder; 
    @Enumerated(EnumType.STRING)
    private StepType stepType;

    private Long nextStepId;
    private Long trueStepId;
    private Long falseStepId;

    @Column(columnDefinition = "TEXT")
    private String conditionJson;

    @ManyToOne
    @JoinColumn(name = "workflow_id", nullable = false)
    @JsonBackReference
    @JsonIdentityReference(alwaysAsId = false)
    private Workflow workflow;
    @ManyToOne
    @JoinColumn(name = "trigger_id")
    private Trigger trigger;

    @ManyToOne
    @JoinColumn(name = "action_id")
    private Action action;

    @ManyToOne
    @JoinColumn(name = "connection_id")
    @JsonBackReference
    private Connection connection;


    public WorkflowStep() {}

    public WorkflowStep(Integer stepOrder, Workflow workflow,
                        Trigger trigger, Action action, Connection connection) {

        this.stepOrder = stepOrder;
        this.workflow = workflow;
        this.trigger = trigger;
        this.action = action;
        this.connection = connection;
    }



    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getStepOrder() {
        return stepOrder;
    }

    public void setStepOrder(Integer stepOrder) {
        this.stepOrder = stepOrder;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    public Trigger getTrigger() {
        return trigger;
    }

    public void setTrigger(Trigger trigger) {
        this.trigger = trigger;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }
    public StepType getStepType() {
        return stepType;
    }

    public void setStepType(StepType stepType) {
        this.stepType = stepType;
    }

    public Long getNextStepId() {
        return nextStepId;
    }

    public void setNextStepId(Long nextStepId) {
        this.nextStepId = nextStepId;
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

    public String getConditionJson() {
        return conditionJson;
    }

    public void setConditionJson(String conditionJson) {
        this.conditionJson = conditionJson;
    }
}
