package com.springrest.Entities;

import jakarta.persistence.*;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;


//commented on 31 jan to solve issue

@JsonIdentityInfo(
	    generator = ObjectIdGenerators.PropertyGenerator.class,
	    property = "id"
	)
@Entity
@Table(name = "actions")
public class Action {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String actionKey;   // e.g. sendEmail
    private String label;       // e.g. "Send Email"
    private String apiEndpoint; // e.g. REST endpoint URL
    private String httpMethod;  // GET, POST, PUT, DELETE
    @Column(name = "has_dynamic_fields", nullable = false)
    private Boolean hasDynamicFields; 
    @Column(name = "is_hidden", nullable = false)
    private Boolean isHidden = false;
//    @Column(name = "is_test_connection")
//    private boolean testConnection = false;

    @ManyToOne
    @JoinColumn(name = "connector_id")
   // @JsonBackReference
    @JsonIdentityReference(alwaysAsId = false)
    //mew added
    @JsonIgnore
    private Connector connector;

    @OneToMany(mappedBy = "action", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @JsonIdentityReference(alwaysAsId = false)
    private List<ActionField> fields;

    @Lob
    @Column(name = "script", columnDefinition = "TEXT")
    private String script; // 👉 JS script for pre/post execution using Rhino

    // ---------- Constructors ----------
    public Action() {
    }

    public Action(String key, String label, String apiEndpoint, String httpMethod, Connector connector) {
        this.actionKey = key;
        this.label = label;
        this.apiEndpoint = apiEndpoint;
        this.httpMethod = httpMethod;
        this.connector = connector;
    }

    public Action(Long id, String key, String label, String apiEndpoint, String httpMethod, Connector connector) {
        this.id = id;
        this.actionKey = key;
        this.label = label;
        this.apiEndpoint = apiEndpoint;
        this.httpMethod = httpMethod;
        this.connector = connector;
    }

    // ---------- Getters & Setters ----------
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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
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

    public Connector getConnector() {
        return connector;
    }

    public void setConnector(Connector connector) {
        this.connector = connector;
    }

    public List<ActionField> getFields() {
        return fields;
    }
//    public boolean isTestConnection() {
//        return testConnection;
//    }
//
//    public void setTestConnection(boolean testConnection) {
//        this.testConnection = testConnection;
//    }

    public void setFields(List<ActionField> fields) {
        this.fields = fields;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }
    public Boolean isHasDynamicFields() {
        return hasDynamicFields;
    }

    public void setHasDynamicFields(Boolean hasDynamicFields) {
        this.hasDynamicFields = hasDynamicFields;
    }
    public Boolean getIsHidden() {
        return isHidden;
    }
    public void setIsHidden(Boolean isHidden) {
        this.isHidden = isHidden;
    }
}