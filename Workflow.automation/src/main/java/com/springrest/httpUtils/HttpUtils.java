package com.springrest.httpUtils;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

@Component
public class HttpUtils {

	private final RestTemplate restTemplate;

	public HttpUtils() {
		this.restTemplate = new RestTemplate();
		this.restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
		    @Override
		    public void handleError(ClientHttpResponse response) throws IOException {
		      
		    }
		});
	}
	public Map<String, Object> callApi(String url, String method, Map<String, Object> inputs,
			Map<String, String> headers) throws IOException {

		if (method == null)
			method = "GET";
		method = method.toUpperCase();
		url = replacePathParams(url, inputs);

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);
		if (headers != null)
			headers.forEach(httpHeaders::set);

		HttpEntity<?> entity;
		URI uri;

		if (method.equals("GET")) {
			uri = URI.create(buildUrlWithQueryParams(url, inputs));
			entity = new HttpEntity<>(httpHeaders);
		} else {
			uri = URI.create(url);
			entity = new HttpEntity<>(inputs, httpHeaders);
		}

		ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.valueOf(method), entity, String.class);

		Map<String, Object> result = new HashMap<>();
		result.put("statusCode", response.getStatusCodeValue());
		result.put("body", response.getBody());

		return result;
	}
	public Map<String, Object> callApiWithPlacement(String url, String method, Map<String, String> headers,
			Map<String, String> queryParams, String finalBody) throws IOException {

		if (method == null)
			method = "GET";
		method = method.toUpperCase();

		String finalUrl = buildUrlWithQueryParams(url, queryParams);

	
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);
		if (headers != null)
			headers.forEach(httpHeaders::set);

		HttpEntity<?> entity;
		URI uri = URI.create(finalUrl);

		if (method.equals("GET")) {
			entity = new HttpEntity<>(httpHeaders);
		} else {
			entity = new HttpEntity<>(finalBody, httpHeaders);
		}

		ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.valueOf(method), entity, String.class);


		Map<String, Object> result = new HashMap<>();
		result.put("statusCode", response.getStatusCodeValue());
		result.put("body", response.getBody());

		return result;
	}
	private String replacePathParams(String url, Map<String, ?> inputs) {
		if (inputs == null)
			return url;
		for (Map.Entry<String, ?> entry : inputs.entrySet()) {
			String placeholder = "{" + entry.getKey() + "}";
			if (url.contains(placeholder)) {
				url = url.replace(placeholder, String.valueOf(entry.getValue()));
			}
		}
		return url;
	}
	private String buildUrlWithQueryParams(String url, Map<String, ?> params) {
		if (params == null || params.isEmpty())
			return url;

		StringBuilder sb = new StringBuilder(url);
		if (!url.contains("?"))
			sb.append("?");

		boolean first = !url.contains("?");
		for (Map.Entry<String, ?> entry : params.entrySet()) {
			if (!first)
				sb.append("&");
			sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
			sb.append("=");
			sb.append(URLEncoder.encode(String.valueOf(entry.getValue()), StandardCharsets.UTF_8));
			first = false;
		}
		return sb.toString();
	}
}
