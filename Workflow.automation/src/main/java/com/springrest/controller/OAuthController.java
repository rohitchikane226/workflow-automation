package com.springrest.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springrest.Entities.AuthData;
import com.springrest.Entities.Connection;
import com.springrest.Entities.Connector;
import com.springrest.repository.ConnectionRepository;
import com.springrest.repository.ConnectorRepository;
import com.springrest.services.WorkflowService;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/oauth")
public class OAuthController {

	@Autowired
	private ConnectorRepository connectorRepository;
	@Autowired
	private WorkflowService workflowService;
     

	@Autowired
	private ConnectionRepository connectionRepository;

	private final RestTemplate rest = new RestTemplate();
	private final ObjectMapper mapper = new ObjectMapper();

	private Map<Long, String> redirectStore = new HashMap<>();  // connectorId → redirect URL

	@GetMapping("/authorize/{connectionId}")
	public ResponseEntity<?> startOAuth(
	        @PathVariable Long connectionId,
	        @RequestParam Long workflowId,
	        @RequestParam Long stepId,
	        @RequestParam("redirect_uri") String frontendRedirect
	) throws Exception {

	    Connection connection = connectionRepository.findById(connectionId)
	            .orElseThrow(() -> new RuntimeException("Connection not found"));

	    Connector connector = connection.getConnector();
      boolean prod=true;
      String callbackUrl;
      if(!prod) {
	    callbackUrl =
	            "http://localhost:8080/api/oauth/callback/" +
	            connector.getAppKey().toLowerCase();
      }else {
    	  callbackUrl =
  	            "https://workflow-automation-production-4a67.up.railway.app/api/oauth/callback/" +
  	            connector.getAppKey().toLowerCase();
      }

	    // 🔐 BACKEND-GENERATED STATE (CRITICAL)
	    Map<String, Object> statePayload = new HashMap<>();
	    statePayload.put("connectionId", connectionId);
	    statePayload.put("workflowId", workflowId);
	    statePayload.put("stepId", stepId);
	    statePayload.put("redirect", frontendRedirect);

	    String state = URLEncoder.encode(
	            mapper.writeValueAsString(statePayload),
	            StandardCharsets.UTF_8
	    );

	    String finalUrl =
	            connector.getAuthUrl()
	            + "?client_id=" + connector.getClientId()
	            + "&redirect_uri=" + callbackUrl
	            + "&response_type=code"
	            + "&access_type=offline"
	            + "&prompt=consent"
	            + (connector.getScopes() != null ? "&scope=" + connector.getScopes() : "")
	            + "&state=" + state; // 🔥 REQUIRED

	    return ResponseEntity.ok(Map.of("redirectUrl", finalUrl));
	}

	@GetMapping("/callback/{connectorName}")
	public void oauthCallback(
	        @PathVariable String connectorName,
	        @RequestParam String code,
	        @RequestParam("state") String state,
	        HttpServletResponse response
	) {
	    try {
	        // ======================================================
	        // 1️⃣ Decode state (sent from frontend)
	        // ======================================================
	        String decodedState = URLDecoder.decode(state, StandardCharsets.UTF_8);
	        JsonNode stateJson = mapper.readTree(decodedState);

	        Long connectionId = stateJson.get("connectionId").asLong();
	        Long workflowId   = stateJson.get("workflowId").asLong();
	        Long stepId       = stateJson.get("stepId").asLong();
	        String redirect   = stateJson.get("redirect").asText();

	        // ======================================================
	        // 2️⃣ Load connection & connector
	        // ======================================================
	        Connection connection = connectionRepository.findById(connectionId)
	                .orElseThrow(() -> new RuntimeException("Connection not found"));

	        Connector connector = connection.getConnector();

	        // ======================================================
	        // 3️⃣ Exchange authorization code for tokens
	        // ======================================================
	        String redirectUrl = "http://localhost:8080/api/oauth/callback/" + connectorName;

	        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
	        body.add("client_id", connector.getClientId());
	        body.add("client_secret", connector.getClientSecret());
	        body.add("grant_type", "authorization_code");
	        body.add("code", code);
	        body.add("redirect_uri", redirectUrl);

	        HttpHeaders headers = new HttpHeaders();
	        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

	        HttpEntity<MultiValueMap<String, String>> request =
	                new HttpEntity<>(body, headers);

	        ResponseEntity<String> tokenResponse =
	                rest.postForEntity(connector.getTokenUrl(), request, String.class);

	        if (!tokenResponse.getStatusCode().is2xxSuccessful()) {
	            throw new RuntimeException("Token exchange failed: " + tokenResponse.getBody());
	        }

	        // ======================================================
	        // 4️⃣ Parse token response
	        // ======================================================
	        JsonNode tokenJson = mapper.readTree(tokenResponse.getBody());

	        String accessToken  = tokenJson.has("access_token")
	                ? tokenJson.get("access_token").asText() : null;

	        String refreshToken = tokenJson.has("refresh_token")
	                ? tokenJson.get("refresh_token").asText() : null;

	        int expiresIn = tokenJson.has("expires_in")
	                ? tokenJson.get("expires_in").asInt() : 3600;

	        LocalDateTime expirationTime = LocalDateTime.now().plusSeconds(expiresIn);

	        // ======================================================
	        // 5️⃣ Store auth data
	        // ======================================================
	        List<AuthData> authList = connection.getAuthDataList();
	        if (authList == null) {
	            authList = new ArrayList<>();
	            connection.setAuthDataList(authList);
	        } else {
	            authList.clear();
	        }

	        AuthData access = new AuthData();
	        access.setConnection(connection);
	        access.setKeyName("access_token");
	        access.setValue(accessToken);
	        authList.add(access);

	        if (refreshToken != null) {
	            AuthData refresh = new AuthData();
	            refresh.setConnection(connection);
	            refresh.setKeyName("refresh_token");
	            refresh.setValue(refreshToken);
	            authList.add(refresh);
	        }

	        AuthData exp = new AuthData();
	        exp.setConnection(connection);
	        exp.setKeyName("expiredAt");
	        exp.setValue(expirationTime.toString());
	        authList.add(exp);

	        connectionRepository.save(connection);
	        workflowService.assignConnectionToStep(stepId, connectionId);
	        response.sendRedirect(redirect + "?connected=true");

	    } catch (Exception e) {
	        e.printStackTrace();
	        try {
	            response.sendRedirect("http://localhost:4200/error");
	        } catch (Exception ignored) {}
	    }
	}
	@PostMapping("/create-empty/{connectorId}")
	public ResponseEntity<?> createEmptyConnection(@PathVariable Long connectorId) {

	    Optional<Connector> connectorOpt = connectorRepository.findById(connectorId);
	    if (connectorOpt.isEmpty()) {
	        return ResponseEntity.badRequest().body("Connector not found");
	    }

	    Connector connector = connectorOpt.get();

	    Connection con = new Connection();
	    con.setName(connector.getName() + " OAuth Connection");
	    con.setConnector(connector);

	    connectionRepository.save(con);

	    return ResponseEntity.ok(con);
	}


}
