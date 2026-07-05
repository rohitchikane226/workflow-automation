package com.springrest.dto;

public class TriggerSummaryResponse {

	private Long id;
	private String name;
	private ConnectorSummaryResponse connector;

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

	public ConnectorSummaryResponse getConnector() {
		return connector;
	}

	public void setConnector(ConnectorSummaryResponse connector) {
		this.connector = connector;
	}
}
