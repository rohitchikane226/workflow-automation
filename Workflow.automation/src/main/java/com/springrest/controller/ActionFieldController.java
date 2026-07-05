package com.springrest.controller;

import com.springrest.Entities.ActionField;
import com.springrest.dto.RefreshDropdownRequest;
import com.springrest.Entities.Action;
import com.springrest.repository.ActionFieldRepository;
import com.springrest.repository.ActionRepository;
import com.springrest.services.ActionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/actions")
public class ActionFieldController {
    @Autowired
    private ActionFieldRepository actionFieldRepository;

    @Autowired
    private ActionRepository actionRepository;
    @Autowired
    private ActionService actionService;
    @GetMapping("/{actionId}/fields")
    public ResponseEntity<List<ActionField>> getFieldsByAction(
            @PathVariable Long actionId,
            @RequestParam Long workflowId,
            @RequestParam Long stepId
    ) {
        List<ActionField> fields =
                actionFieldRepository.findByActionId(actionId);

        for (ActionField field : fields) {
            actionService.populateDynamicDropdown(
                    field,
                    actionId,
                    workflowId,
                    stepId
            );
        }

        return ResponseEntity.ok(fields);
    }


    @PostMapping("/refresh-dependent-dropdowns")
    public ResponseEntity<Void> refreshDependentDropdowns(
            @RequestBody RefreshDropdownRequest request
    ) {
        actionService.refreshBelowDynamicDropdowns(
                request.getStepId(),
                request.getFieldKey()
        );
        return ResponseEntity.ok().build();
    }
    @PostMapping("/steps/{stepId}/refresh-dynamic-fields")
    public ResponseEntity<String> refreshDynamicInputFields(
            @PathVariable Long stepId, @RequestParam(required = false) String changedFieldKey
    ) {
        String json =
                actionService.populateDynamicInputFields(stepId,changedFieldKey);

        return ResponseEntity.ok(json);
    }

    @PutMapping("/fields/{fieldId}")
    public ResponseEntity<ActionField> updateField(@PathVariable Long fieldId, @RequestBody ActionField fieldDetails) {
        Optional<ActionField> optionalField = actionFieldRepository.findById(fieldId);
        if (!optionalField.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        ActionField field = optionalField.get();
        field.setKey(fieldDetails.getKey());
        field.setLabel(fieldDetails.getLabel());
        field.setRequired(fieldDetails.isRequired());
        if (fieldDetails.getAction() != null) {
            Optional<Action> actionOptional = actionRepository.findById(fieldDetails.getAction().getId());
            actionOptional.ifPresent(field::setAction);
        }

        actionFieldRepository.save(field);
        return ResponseEntity.ok(field);
    }
    @DeleteMapping("/fields/{fieldId}")
    public ResponseEntity<Void> deleteField(@PathVariable Long fieldId) {
        if (!actionFieldRepository.existsById(fieldId)) {
            return ResponseEntity.notFound().build();
        }
        actionFieldRepository.deleteById(fieldId);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/steps/{stepId}/refresh-dynamic-output-fields")
    public ResponseEntity<String> refreshDynamicOutputFields(
            @PathVariable Long stepId,
            @RequestParam(required = false) String changedFieldKey
    ) {

        String json = actionService.refreshDynamicOutputFields(stepId, changedFieldKey);

        return ResponseEntity.ok(json);
    }
}
