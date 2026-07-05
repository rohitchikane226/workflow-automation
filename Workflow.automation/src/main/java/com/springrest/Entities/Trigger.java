package com.springrest.Entities;

import jakarta.persistence.*;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

@JsonIdentityInfo(
    generator = ObjectIdGenerators.PropertyGenerator.class,
    property = "id"
)
@Entity
@Table(name = "triggers")
public class Trigger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trigger_key", nullable = false)
    private String triggerKey;


    @Column(nullable = false)
    private String label;
    @Column(name = "is_has_dynamic_fields")
    private Boolean hasDynamicFields;

  
    @Column(name = "trigger_type", nullable = false)
    private String triggerType;

   
    @Column(name = "api_endpoint")
    private String apiEndpoint;

    @Column(name = "http_method")
    private String httpMethod;

    @Column(name = "record_identifier_key")
    private String recordIdentifierKey;
    @Lob
    @Column(name = "script", columnDefinition = "TEXT")
    private String script;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connector_id", nullable = false)
 
    @JsonIgnore
    private Connector connector;

    @OneToMany(
        mappedBy = "trigger",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<TriggerField> fields;
    @OneToMany(mappedBy = "trigger", fetch = FetchType.LAZY)
    private List<WorkflowTrigger> workflowTriggers;



    public Trigger() {
    }



    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return triggerKey;
    }

    public void setKey(String triggerKey) {
        this.triggerKey = triggerKey;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getApiEndpoint() {
        return apiEndpoint;
    }

    public void setApiEndpoint(String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getRecordIdentifierKey() {
        return recordIdentifierKey;
    }

    public void setRecordIdentifierKey(String recordIdentifierKey) {
        this.recordIdentifierKey = recordIdentifierKey;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public Connector getConnector() {
        return connector;
    }
    public Boolean getHasDynamicFields() {
        return hasDynamicFields;
    }

    public void setHasDynamicFields(Boolean hasDynamicFields) {
        this.hasDynamicFields = hasDynamicFields;
    }

    public void setConnector(Connector connector) {
        this.connector = connector;
    }

    public List<TriggerField> getFields() {
        return fields;
    }

    public void setFields(List<TriggerField> fields) {
        this.fields = fields;
    }
    public List<WorkflowTrigger> getWorkflowTriggers() {
        return workflowTriggers;
    }

    public void setWorkflowTriggers(List<WorkflowTrigger> workflowTriggers) {
        this.workflowTriggers = workflowTriggers;
    }
}
