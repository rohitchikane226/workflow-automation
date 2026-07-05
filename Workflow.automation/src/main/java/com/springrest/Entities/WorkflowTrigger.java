package com.springrest.Entities;

import jakarta.persistence.*;

@Entity
@Table(name = "workflow_triggers")
public class WorkflowTrigger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @ManyToOne
    @JoinColumn(name = "trigger_id", nullable = false)
    private Trigger trigger;

    @Column(name = "webhook_uuid", unique = true)
    private String webhookUuid;

    @Column(name = "external_webhook_id")
    private String externalWebhookId;

    @Lob
    @Column(name = "subscribe_response", columnDefinition = "TEXT")
    private String subscribeResponse;

    @Column(nullable = false)
    private String status;


    public WorkflowTrigger() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getWebhookUuid() {
        return webhookUuid;
    }

    public void setWebhookUuid(String webhookUuid) {
        this.webhookUuid = webhookUuid;
    }

    public String getExternalWebhookId() {
        return externalWebhookId;
    }

    public void setExternalWebhookId(String externalWebhookId) {
        this.externalWebhookId = externalWebhookId;
    }

    public String getSubscribeResponse() {
        return subscribeResponse;
    }

    public void setSubscribeResponse(String subscribeResponse) {
        this.subscribeResponse = subscribeResponse;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
