package com.springrest.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springrest.Entities.*;
import com.springrest.repository.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class AuthGenerationService {

	private final AuthDataRepository authDataRepository;
	private final AuthFieldRepository authFieldRepository;
	private final ConnectionRepository connectionRepository;
	private final ObjectMapper mapper = new ObjectMapper();

	private final RestTemplate restTemplate = new RestTemplate();

	public AuthGenerationService(AuthDataRepository authDataRepository, AuthFieldRepository authFieldRepository,
			ConnectionRepository connectionRepository) {
		this.authDataRepository = authDataRepository;
		this.authFieldRepository = authFieldRepository;
		this.connectionRepository = connectionRepository;
	}
	public AuthResult applyAuth(Long connectionId, String url) {
		//findWithConnector replace by findById
		Connection connection = connectionRepository.findWithConnector(connectionId)
				.orElseThrow(() -> new RuntimeException("Connection not found"));

		String authType = connection.getConnector().getAuthType();

		if ("oauth2".equalsIgnoreCase(authType)) {
			return applyOAuth(connection, url);
		}
		return applyNormalAuth(connection, url);
	}
	private AuthResult applyNormalAuth(Connection connection, String url) {

		String authMappingString = connection.getConnector().getAuthMapping();
		String placement = connection.getConnector().getAuthTokenPlacement(); // header, query, url

		System.out.println("placement "+placement);
		if (placement == null || placement.trim().isEmpty()) {
	        AuthResult result = new AuthResult();
	        result.setHeaders(new HashMap<>());
	        result.setQueryParams(new HashMap<>());
	        result.setFinalUrl(url);
	        return result;
	    }
		Map<String, String> mapping = parseMapping(authMappingString);
		Map<String, String> authValues = loadAuthValues(connection.getId());

		validateRequiredKeys(mapping, connection.getConnector().getId());

		Map<String, String> replaced = new HashMap<>();
		for (String key : mapping.keySet()) {
			replaced.put(key, applyTemplate(mapping.get(key), authValues));
		}

		AuthResult result = new AuthResult();
		result.setHeaders(new HashMap<>());
		result.setQueryParams(new HashMap<>());
		result.setFinalUrl(url);

		switch (placement.toLowerCase()) {
		case "header":
			result.getHeaders().putAll(replaced);
			break;
		case "query":
			result.setFinalUrl(applyQueryParams(url, replaced));
			break;
		case "url":
			result.setFinalUrl(applyUrlReplacement(url, replaced));
			break;
		default:
			result.getHeaders().putAll(replaced);
		}
   System.out.println("result "+result.headers);
		return result;
	}

	private AuthResult applyOAuth(Connection connection, String url) {

		Connector connector = connection.getConnector();
		Map<String, String> authValues = loadAuthValues(connection.getId());
  System.out.println("authValues  "+authValues.toString());
		String accessToken = authValues.get("access_token");
		String refreshToken = authValues.get("refresh_token");
		String tokenUrl     = connector.getTokenUrl();
		String clientId     = connector.getClientId();
		String clientSecret = connector.getClientSecret();

		long expiresAt = 0;
		try {
			String expiredAtStr =  authValues.get("expiredAt");
			System.out.println("expiresAt1 " + expiredAtStr);
			if (expiredAtStr != null && !expiredAtStr.isEmpty()) {

		        if (expiredAtStr.matches("\\d+")) {
		            expiresAt = Long.parseLong(expiredAtStr);
		        }
		        else {
		      
		            LocalDateTime ldt = LocalDateTime.parse(expiredAtStr);
		            expiresAt = ldt.atZone(ZoneId.systemDefault()).toEpochSecond();
		        }
		    }
		} catch (Exception ignored) {
		}

		long now = Instant.now().getEpochSecond();
		System.out.println("now  "+now);
		System.out.println("expiresAt  "+expiresAt);
		if (expiresAt != 0 && now > expiresAt - 30) {

			try {
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

				String body = "grant_type=refresh_token" + "&refresh_token="
						+ URLEncoder.encode(refreshToken, StandardCharsets.UTF_8) + "&client_id="
						+ URLEncoder.encode(clientId, StandardCharsets.UTF_8) + "&client_secret="
						+ URLEncoder.encode(clientSecret, StandardCharsets.UTF_8);

				HttpEntity<String> req = new HttpEntity<>(body, headers);

				ResponseEntity<Map> resp = restTemplate.exchange(tokenUrl, HttpMethod.POST, req, Map.class);

				if (resp.getStatusCode().is2xxSuccessful()) {

					Map<String, Object> tokenData = resp.getBody();

					String newAccess = String.valueOf(tokenData.get("access_token"));
					String newRefresh = String.valueOf(tokenData.getOrDefault("refresh_token", refreshToken));
					int expiresIn = Integer.parseInt(tokenData.get("expires_in").toString());

					long newExpiry = Instant.now().getEpochSecond() + expiresIn;
					saveAuthValue(connection, "access_token", newAccess);
					saveAuthValue(connection, "refresh_token", newRefresh);
					saveAuthValue(connection, "expiredAt", String.valueOf(newExpiry));

					accessToken = newAccess;
				}

			} catch (Exception e) {
				throw new RuntimeException("OAuth token refresh failed", e);
			}
		}

		AuthResult result = new AuthResult();
		result.setHeaders(new HashMap<>());
		result.setQueryParams(new HashMap<>());
		result.getHeaders().put("Authorization", "Bearer " + accessToken);
		result.setFinalUrl(url);
		return result;
	}
	private void saveAuthValue(Connection connection, String key, String value) {
		AuthData data = authDataRepository.findByConnectionIdAndKeyName(connection.getId(), key);
		if (data == null) {
			data = new AuthData();
			data.setConnection(connection);
			data.setKeyName(key);
		}
		data.setValue(value);
		authDataRepository.save(data);
	}
	private Map<String, String> parseMapping(String json) {
		try {
			if (json == null || json.trim().isEmpty())
				return new HashMap<>();
			return mapper.readValue(json, Map.class);
		} catch (Exception e) {
			throw new RuntimeException("Invalid authMapping JSON");
		}
	}

	private Map<String, String> loadAuthValues(Long connectionId) {
		List<AuthData> list = authDataRepository.findByConnectionId(connectionId);
		Map<String, String> map = new HashMap<>();
		for (AuthData ad : list) {
			map.put(ad.getKeyName(), ad.getValue());
		}
		return map;
	}

	private void validateRequiredKeys(Map<String, String> mapping, Long connectorId) {
		List<AuthField> fields = authFieldRepository.findByConnectorId(connectorId);
		Set<String> allowedKeys = new HashSet<>();

		for (AuthField f : fields) {
			allowedKeys.add(f.getKeyName());
		}

		for (String value : mapping.values()) {
			for (String placeholder : extractPlaceholders(value)) {
				if (!allowedKeys.contains(placeholder)) {
					throw new RuntimeException("Missing authentication field: " + placeholder);
				}
			}
		}
	}
	private List<String> extractPlaceholders(String str) {
		List<String> placeholders = new ArrayList<>();

		int start = 0;
		while ((start = str.indexOf("{{", start)) != -1) {
			int end = str.indexOf("}}", start);
			if (end != -1) {
				String key = str.substring(start + 2, end).trim();
				placeholders.add(key);
				start = end + 2;
			} else {
				break;
			}
		}
		return placeholders;
	}

	private String applyTemplate(String template, Map<String, String> values) {
		String out = template;
		for (String key : values.keySet()) {
			out = out.replace("{{" + key + "}}", values.get(key));
		}
		return out;
	}

	private String applyQueryParams(String url, Map<String, String> params) {
		StringBuilder sb = new StringBuilder(url);
		sb.append(url.contains("?") ? "&" : "?");

		for (var e : params.entrySet()) {
			sb.append(e.getKey()).append("=").append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
					.append("&");
		}
		return sb.substring(0, sb.length() - 1);
	}
	private String applyUrlReplacement(String url, Map<String, String> values) {
		String out = url;
		for (var e : values.entrySet()) {
			out = out.replace("{{" + e.getKey() + "}}", e.getValue());
		}
		return out;
	}
	public static class AuthResult {
		private Map<String, String> headers;
		private Map<String, String> queryParams;
		private String finalUrl;

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

		public String getFinalUrl() {
			return finalUrl;
		}

		public void setFinalUrl(String finalUrl) {
			this.finalUrl = finalUrl;
		}
	}
}
