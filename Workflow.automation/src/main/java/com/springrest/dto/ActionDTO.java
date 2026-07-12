package com.springrest.dto;

public class ActionDTO {

    private Long id;
    private String label;
    private Boolean hasDynamicFields;
    private Boolean isHidden;
    private String key;

    public ActionDTO() {
    }

    public ActionDTO(Long id, String label, Boolean hasDynamicFields,
                     Boolean isHidden, String key) {
        this.id = id;
        this.label = label;
        this.hasDynamicFields = hasDynamicFields;
        this.isHidden = isHidden;
        this.key = key;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Boolean getHasDynamicFields() {
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

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}