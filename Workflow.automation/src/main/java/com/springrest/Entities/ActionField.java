package com.springrest.Entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
@Table(name = "action_fields")
public class ActionField {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String actionKey;
	private String label;
	private boolean isRequired;

	private String placement;
	private String fieldType;

	private String fieldDataType;

	private Long dynamicDropdownActionId;
	private String dynamicDropdownKey;
	private String placeholder;

	@Column(columnDefinition = "TEXT")
	private String options;
	@ManyToOne
	@JoinColumn(name = "action_id")
	@JsonBackReference
	private Action action;
	private boolean refresh;

	public ActionField() {
	}

	public ActionField(String key, String label, boolean isRequired, String placement, String fieldType, Action action,
			boolean refresh) {
		this.actionKey = key;
		this.label = label;
		this.isRequired = isRequired;
		this.placement = placement;
		this.fieldType = fieldType;
		this.action = action;
		this.refresh = refresh;
	}

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

	public boolean isRefresh() {
		return refresh;
	}

	public void setRefresh(boolean refresh) {
		this.refresh = refresh;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public boolean isRequired() {
		return isRequired;
	}

	public void setRequired(boolean required) {
		isRequired = required;
	}

	public String getPlacement() {
		return placement;
	}

	public void setPlacement(String placement) {
		this.placement = placement;
	}

	public String getFieldType() {
		return fieldType;
	}

	public void setFieldType(String fieldType) {
		this.fieldType = fieldType;
	}

	public Action getAction() {
		return action;
	}

	public void setAction(Action action) {
		this.action = action;
	}

	public String getFieldDataType() {
		return fieldDataType;
	}

	public void setFieldDataType(String fieldDataType) {
		this.fieldDataType = fieldDataType;
	}

	public String getOptions() {
		return options;
	}

	public void setOptions(String options) {
		this.options = options;
	}

	public Long getDynamicDropdownActionId() {
		return dynamicDropdownActionId;
	}

	public void setDynamicDropdownActionId(Long dynamicDropdownActionId) {
		this.dynamicDropdownActionId = dynamicDropdownActionId;
	}

	public String getDynamicDropdownKey() {
		return dynamicDropdownKey;
	}

	public void setDynamicDropdownKey(String dynamicDropdownKey) {
		this.dynamicDropdownKey = dynamicDropdownKey;
	}

	public String getPlaceholder() {
		return placeholder;
	}

	public void setPlaceholder(String placeholder) {
		this.placeholder = placeholder;
	}

}
