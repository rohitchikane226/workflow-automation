package com.springrest.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.springrest.Entities.*;
import com.springrest.dto.ActionDTO;
import com.springrest.dto.AuthFieldDto;
import com.springrest.dto.ConnectorDTO;
import com.springrest.dto.ConnectorDetailsDto;
import com.springrest.dto.TriggerDTO;
import com.springrest.repository.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/connectors")
public class ConnectorController {

    private final ConnectorRepository connectorRepo;
    private final ActionRepository actionRepo;
    private final TriggerRepository triggerRepo;
   private final AuthFieldRepository authFieldRepo;

    public ConnectorController(ConnectorRepository connectorRepo,AuthFieldRepository authFieldRepo, ActionRepository actionRepo, TriggerRepository triggerRepo) {
        this.connectorRepo = connectorRepo;
        this.actionRepo = actionRepo;
        this.triggerRepo = triggerRepo;
        this.authFieldRepo=authFieldRepo;
    }

    @GetMapping
    public List<ConnectorDTO> getAllConnectors() {

        return connectorRepo.findAll()
                .stream()
                .map(connector -> new ConnectorDTO(
                        connector.getId(),
                        connector.getName(),
                        connector.getAppKey(),
                        connector.getLogoUrl(),
                        connector.getDescription()
                ))
                .toList();
    }
  @PostMapping
  public Connector createConnector(@RequestBody Connector connector){
	  return connectorRepo.save(connector);
  }
    // Get one connector
  @GetMapping("/{id}")
  public ResponseEntity<ConnectorDetailsDto> getConnector(@PathVariable Long id) {

      return connectorRepo.findById(id)
              .map(connector -> {

                  List<AuthFieldDto> authFields = connector.getAuthFields()
                          .stream()
                          .map(field -> new AuthFieldDto(
                                  field.getId(),
                                  field.getKeyName(),
                                  field.getLabel(),
                                  field.getType(),
                                  field.isRequired(),
                                  field.getPlaceholder(),
                                  field.getExampleValue()
                          ))
                          .toList();

                  ConnectorDetailsDto dto = new ConnectorDetailsDto(
                          connector.getId(),
                          connector.getName(),
                          connector.getAppKey(),
                          connector.getLogoUrl(),
                          connector.getDescription(),
                          connector.getAuthType(),
                          authFields
                  );

                  return ResponseEntity.ok(dto);
              })
              .orElse(ResponseEntity.notFound().build());
  }
    
    @GetMapping("/{id}/actions")
    public List<ActionDTO> getConnectorActions(@PathVariable Long id) {

        return actionRepo.findByConnectorId(id)
                .stream()
                .map(action -> new ActionDTO(
                        action.getId(),
                        action.getLabel(),
                        action.isHasDynamicFields(),
                        action.getIsHidden(),
                        action.getKey()
                ))
                .toList();
    }
    @GetMapping("/{id}/triggers")
    public List<TriggerDTO> getConnectorTriggers(@PathVariable Long id) {

        return triggerRepo.findByConnectorId(id)
                .stream()
                .map(trigger -> new TriggerDTO(
                        trigger.getId(),
                        trigger.getLabel(),
                        trigger.getHasDynamicFields(),
                        trigger.getTriggerType(),
                        trigger.getKey()
                ))
                .toList();
    }
    @PostMapping("/{connectorId}/authfields")
    public ResponseEntity<AuthField> addAuthField(
            @PathVariable Long connectorId,
            @RequestBody AuthField authField) {

        return connectorRepo.findById(connectorId)
                .map(connector -> {
                    authField.setConnector(connector);
                    connector.getAuthFields().add(authField);
                    connectorRepo.save(connector);
                    return ResponseEntity.ok(authField);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/{id}/auth-fields")
    public ResponseEntity<?> getAuthFields(@PathVariable Long id) {
        List<AuthField> fields = authFieldRepo.findByConnectorId(id);
        return ResponseEntity.ok(fields);
    }
    @PutMapping("/authfields/{authFieldId}")
    public ResponseEntity<AuthField> updateAuthField(
            @PathVariable Long authFieldId,
            @RequestBody AuthField updated) {

        return authFieldRepo.findById(authFieldId)
                .map(existing -> {
                    existing.setKeyName(updated.getKeyName());
                    existing.setLabel(updated.getLabel());
                    existing.setType(updated.getType());
                    existing.setRequired(updated.isRequired());
                    existing.setPlaceholder(updated.getPlaceholder());
                    return ResponseEntity.ok(authFieldRepo.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    @DeleteMapping("/authfields/{authFieldId}")
    public ResponseEntity<?> deleteAuthField(@PathVariable Long authFieldId) {
        return authFieldRepo.findById(authFieldId)
                .map(authField -> {
                    authFieldRepo.delete(authField);
                    return ResponseEntity.ok("Deleted");
                })
                .orElse(ResponseEntity.notFound().build());
    }
}