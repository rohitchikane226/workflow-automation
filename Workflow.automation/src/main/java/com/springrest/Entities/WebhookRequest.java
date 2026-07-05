package com.springrest.Entities;

import java.util.Map;

public class WebhookRequest {

    private String apiBase;                
    private String webhookUrl;              
    private String webhookId;               
    private String eventName;               

    private Map<String, String> headers;    
    private Map<String, String> queryParams;
    private Map<String, Object> bodyParams; 


    public WebhookRequest() {}

    public WebhookRequest(String apiBase, String webhookUrl, String webhookId,
                          String eventName, Map<String, String> headers,
                          Map<String, String> queryParams,
                          Map<String, Object> bodyParams) {

        this.apiBase = apiBase;
        this.webhookUrl = webhookUrl;
        this.webhookId = webhookId;
        this.eventName = eventName;
        this.headers = headers;
        this.queryParams = queryParams;
        this.bodyParams = bodyParams;
    }

    // ----------- Getters & Setters -----------

    public String getApiBase() {
        return apiBase;
    }

    public void setApiBase(String apiBase) {
        this.apiBase = apiBase;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getWebhookId() {
        return webhookId;
    }

    public void setWebhookId(String webhookId) {
        this.webhookId = webhookId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(Map<String, String> queryParams) {
        this.queryParams = queryParams;
    }

    public Map<String, Object> getBodyParams() {
        return bodyParams;
    }

    public void setBodyParams(Map<String, Object> bodyParams) {
        this.bodyParams = bodyParams;
    }
}
