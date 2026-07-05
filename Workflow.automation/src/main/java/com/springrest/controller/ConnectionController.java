package com.springrest.controller;

import com.springrest.Entities.*;
import com.springrest.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("api/connections")
public class ConnectionController {

	@Autowired
	private ConnectionRepository connectionRepository;

	@Autowired
	private ConnectorRepository connectorRepository;

	@Autowired
	private AuthFieldRepository authFieldRepository;

	@PostMapping("/{connectorId}")
	public ResponseEntity<?> createConnection(@PathVariable Long connectorId, @RequestBody Map<String, Object> body) {

		try {
			Optional<Connector> connectorOpt = connectorRepository.findById(connectorId);
			if (connectorOpt.isEmpty())
				return ResponseEntity.badRequest().body("Connector not found");

			Connector connector = connectorOpt.get();

			String connectionName = (String) body.get("name");
			List<Map<String, String>> authValues = (List<Map<String, String>>) body.get("authData");

			if (connectionName == null || connectionName.isEmpty())
				return ResponseEntity.badRequest().body("Name is required");

			if (connectionRepository.existsByName(connectionName))
				return ResponseEntity.badRequest().body("Connection name already exists");
			List<AuthField> expectedFields = authFieldRepository.findByConnectorId(connectorId);

			Map<String, AuthField> expectedMap = new HashMap<>();
			for (AuthField f : expectedFields) {
				expectedMap.put(f.getKeyName(), f);
			}
			Set<String> providedKeys = new HashSet<>();
			if (authValues != null) {
				for (Map<String, String> field : authValues) {
					providedKeys.add(field.get("key"));
				}
			}

			for (AuthField field : expectedFields) {
				if (field.isRequired() && !providedKeys.contains(field.getKeyName())) {
					return ResponseEntity.badRequest().body("Missing required field: " + field.getKeyName());
				}
			}
			Connection connection = new Connection();
			connection.setName(connectionName);
			connection.setConnector(connector);
			List<AuthData> authList = new ArrayList<>();

			if (authValues != null) {
				for (Map<String, String> field : authValues) {

					String key = field.get("key");
					String value = field.get("value");

					if (!expectedMap.containsKey(key))
						return ResponseEntity.badRequest().body("Invalid auth field: " + key);

					AuthData authData = new AuthData();
					authData.setConnection(connection);
					authData.setKeyName(key);
					authData.setValue(value);

					authList.add(authData);
				}
			}

			connection.setAuthDataList(authList);
			Connection savedConnection = connectionRepository.save(connection);
			Map<String, Object> response = new LinkedHashMap<>();
			response.put("id", savedConnection.getId());

			response.put("name", savedConnection.getName());

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().body("Something went wrong");
		}
	}

	@GetMapping("/connector/{connectorId}")
	public ResponseEntity<?> getConnectionsByConnector(@PathVariable Long connectorId) {
		return ResponseEntity.ok(connectionRepository.findByConnectorId(connectorId));
	}

	@GetMapping("/{connectionId}")
	public ResponseEntity<?> getConnection(@PathVariable Long connectionId) {

		Optional<Connection> connectionOpt = connectionRepository.findById(connectionId);
		if (connectionOpt.isEmpty())
			return ResponseEntity.badRequest().body("Connection not found");

		Connection connection = connectionOpt.get();

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("id", connection.getId());
		response.put("name", connection.getName());
		response.put("connectorId", connection.getConnector().getId());
		response.put("authData", connection.getAuthDataList());

		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{connectionId}")
	public ResponseEntity<?> deleteConnection(@PathVariable Long connectionId) {

		if (!connectionRepository.existsById(connectionId))
			return ResponseEntity.badRequest().body("Connection not found");

		connectionRepository.deleteById(connectionId);

		return ResponseEntity.ok("Deleted successfully");
	}

	@GetMapping
	public ResponseEntity<?> getAllConnections() {
		try {
			List<Connection> connections = connectionRepository.findAll();

			List<Map<String, Object>> response = new ArrayList<>();

			for (Connection c : connections) {
				Map<String, Object> obj = new LinkedHashMap<>();
				obj.put("id", c.getId());
				obj.put("name", c.getName());
				obj.put("connectorId", c.getConnector().getId());
				obj.put("connectorName", c.getConnector().getName());
				obj.put("authData", c.getAuthDataList());
				response.add(obj);
			}

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().body("Error fetching connections");
		}
	}
}
