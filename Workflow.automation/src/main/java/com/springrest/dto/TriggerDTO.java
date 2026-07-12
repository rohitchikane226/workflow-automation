package com.springrest.dto;

public class TriggerDTO {

    private Long id;
    private String label;
    private Boolean hasDynamicFields;
    private String triggerType;
    private String key;

    public TriggerDTO() {
    }

    public TriggerDTO(Long id, String label, Boolean hasDynamicFields,
                      String triggerType, String key) {
        this.id = id;
        this.label = label;
        this.hasDynamicFields = hasDynamicFields;
        this.triggerType = triggerType;
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

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}