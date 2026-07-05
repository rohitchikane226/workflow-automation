package com.springrest.controller;

import com.springrest.Entities.Trigger;
import com.springrest.Entities.TriggerField;
import com.springrest.dto.RefreshDropdownRequest;
import com.springrest.repository.TriggerFieldRepository;
import com.springrest.repository.TriggerRepository;
import com.springrest.services.TriggerService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/triggers")
public class TriggerFieldController {

    @Autowired
    private TriggerFieldRepository triggerFieldRepository;

    @Autowired
    private TriggerRepository triggerRepository;
    @Autowired
    private TriggerService triggerService;

    @GetMapping("/{triggerId}/fields")
    public ResponseEntity<List<TriggerField>> getFields(
            @PathVariable Long triggerId,
            @RequestParam Long workflowId,
            @RequestParam Long stepId
    ) {

        List<TriggerField> fields =
                triggerFieldRepository.findByTriggerId(triggerId);

        for (TriggerField field : fields) {
            triggerService.populateDynamicDropdown(
                    field,
                    triggerId,
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

        triggerService.refreshDependentFields(
                request.getStepId(),
                request.getFieldKey()
        );

        return ResponseEntity.ok().build();
    
}
    @PostMapping("/{triggerId}/fields")
    public ResponseEntity<TriggerField> createFieldForTrigger(@PathVariable Long triggerId, @RequestBody TriggerField field) {
        Optional<Trigger> triggerOptional = triggerRepository.findById(triggerId);
        if (!triggerOptional.isPresent()) {
            return ResponseEntity.badRequest().build();
        }

        field.setTrigger(triggerOptional.get());
        triggerFieldRepository.save(field);
        return ResponseEntity.ok(field);
    }
    @PutMapping("/fields/{fieldId}")
    public ResponseEntity<TriggerField> updateField(@PathVariable Long fieldId, @RequestBody TriggerField fieldDetails) {
        Optional<TriggerField> optionalField = triggerFieldRepository.findById(fieldId);
        if (!optionalField.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        TriggerField field = optionalField.get();
        field.setKey(fieldDetails.getKey());
        field.setLabel(fieldDetails.getLabel());
        field.setRequired(fieldDetails.isRequired());
        if (fieldDetails.getTrigger() != null) {
            Optional<Trigger> triggerOptional = triggerRepository.findById(fieldDetails.getTrigger().getId());
            triggerOptional.ifPresent(field::setTrigger);
        }

        triggerFieldRepository.save(field);
        return ResponseEntity.ok(field);
    }

    @DeleteMapping("/fields/{fieldId}")
    public ResponseEntity<Void> deleteField(@PathVariable Long fieldId) {
        if (!triggerFieldRepository.existsById(fieldId)) {
            return ResponseEntity.notFound().build();
        }
        triggerFieldRepository.deleteById(fieldId);
        return ResponseEntity.noContent().build();
    }
}
