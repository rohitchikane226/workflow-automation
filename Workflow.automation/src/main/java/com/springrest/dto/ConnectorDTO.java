package com.springrest.dto;

public class ConnectorDTO {

    private Long id;
    private String name;
    private String appKey;
    private String logoUrl;
    private String description;

    public ConnectorDTO() {
    }

    public ConnectorDTO(Long id, String name, String appKey, String logoUrl, String description) {
        this.id = id;
        this.name = name;
        this.appKey = appKey;
        this.logoUrl = logoUrl;
        this.description = description;
    }

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

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}