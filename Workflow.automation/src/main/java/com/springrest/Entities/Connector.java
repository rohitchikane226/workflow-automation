package com.springrest.Entities;

import jakarta.persistence.*;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "connectors")
public class Connector {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name; 
	private String appKey; 
	@Column(length = 500)
	private String logoUrl; 
	private String description; 

	private String authType; 

	private String authUrl; 
	private String tokenUrl; 
	private String clientId;
	private String clientSecret;
	private String scopes; 
	@Column(columnDefinition = "TEXT")
	private String authMapping;

	private String authTokenPlacement;

	@OneToMany(mappedBy = "connector", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Action> actions;

	@OneToMany(mappedBy = "connector", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Trigger> triggers;

	@OneToMany(mappedBy = "connector", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Connection> connections;
	@OneToMany(mappedBy = "connector", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<AuthField> authFields;

	public Connector() {
	}

	public Connector(String name, String appKey, String logoUrl, String description, String authType, String authUrl,
			String tokenUrl, String clientId, String clientSecret, String scopes, String authMapping,
			String authTokenPlacement) {

		this.name = name;
		this.appKey = appKey;
		
		this.logoUrl = logoUrl;
		this.description = description;

		this.authType = authType;
		this.authUrl = authUrl;
		this.tokenUrl = tokenUrl;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.scopes = scopes;

		this.authMapping = authMapping;
		this.authTokenPlacement = authTokenPlacement;
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

	public String getAuthUrl() {
		return authUrl;
	}

	public void setAuthUrl(String authUrl) {
		this.authUrl = authUrl;
	}

	public String getTokenUrl() {
		return tokenUrl;
	}

	public void setTokenUrl(String tokenUrl) {
		this.tokenUrl = tokenUrl;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	public String getScopes() {
		return scopes;
	}

	public void setScopes(String scopes) {
		this.scopes = scopes;
	}

	public String getAuthMapping() {
		return authMapping;
	}

	public void setAuthMapping(String authMapping) {
		this.authMapping = authMapping;
	}

	public String getAuthTokenPlacement() {
		return authTokenPlacement;
	}

	public void setAuthTokenPlacement(String authTokenPlacement) {
		this.authTokenPlacement = authTokenPlacement;
	}

	public List<Action> getActions() {
		return actions;
	}

	public void setActions(List<Action> actions) {
		this.actions = actions;
	}

	public List<Trigger> getTriggers() {
		return triggers;
	}

	public void setTriggers(List<Trigger> triggers) {
		this.triggers = triggers;
	}

	public List<Connection> getConnections() {
		return connections;
	}

	public void setConnections(List<Connection> connections) {
		this.connections = connections;
	}
	public List<AuthField> getAuthFields() {
	    return authFields;
	}

	public void setAuthFields(List<AuthField> authFields) {
	    this.authFields = authFields;
	}
}
