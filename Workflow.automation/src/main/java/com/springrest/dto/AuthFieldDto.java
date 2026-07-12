
package com.springrest.dto; 

public class AuthFieldDto {

    private Long id;
    private String keyName;
    private String label;
    private String type;
    private Boolean required;
    private String placeholder;
    private String exampleValue;

    public AuthFieldDto() {
    }

    public AuthFieldDto(Long id,
                        String keyName,
                        String label,
                        String type,
                        Boolean required,
                        String placeholder,
                        String exampleValue) {
        this.id = id;
        this.keyName = keyName;
        this.label = label;
        this.type = type;
        this.required = required;
        this.placeholder = placeholder;
        this.exampleValue = exampleValue;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public String getExampleValue() {
        return exampleValue;
    }

    public void setExampleValue(String exampleValue) {
        this.exampleValue = exampleValue;
    }
}