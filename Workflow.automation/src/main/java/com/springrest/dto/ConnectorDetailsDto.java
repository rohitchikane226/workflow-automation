
package com.springrest.dto;
import java.util.List;

public class ConnectorDetailsDto {

    private Long id;
    private String name;
    private String appKey;
    private String logoUrl;
    private String description;
    private String authType;
    private List<AuthFieldDto> authFields;

    public ConnectorDetailsDto() {
    }

    public ConnectorDetailsDto(Long id,
                               String name,
                               String appKey,
                               String logoUrl,
                               String description,
                               String authType,
                               List<AuthFieldDto> authFields) {
        this.id = id;
        this.name = name;
        this.appKey = appKey;
        this.logoUrl = logoUrl;
        this.description = description;
        this.authType = authType;
        this.authFields = authFields;
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

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public List<AuthFieldDto> getAuthFields() {
        return authFields;
    }

    public void setAuthFields(List<AuthFieldDto> authFields) {
        this.authFields = authFields;
    }
}