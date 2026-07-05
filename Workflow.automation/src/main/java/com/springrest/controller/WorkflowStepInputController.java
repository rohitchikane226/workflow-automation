package com.springrest.controller;

//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.springrest.Entities.ActionField;
import com.springrest.Entities.TriggerField;
import com.springrest.Entities.WorkflowStep;
import com.springrest.Entities.WorkflowStepInput;
import com.springrest.dto.WorkflowStepInputDto;
import com.springrest.repository.WorkflowStepInputRepository;
import com.springrest.repository.WorkflowStepRepository;
import com.springrest.services.ActionService;
import com.springrest.services.TriggerService;

import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Set;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/workflow-steps/{stepId}/inputs")
public class WorkflowStepInputController {

	private final WorkflowStepInputRepository inputRepo;
    private final WorkflowStepRepository stepRepo;
    private final ActionService actionservice;
    private final TriggerService triggerService;

    public WorkflowStepInputController(WorkflowStepInputRepository inputRepo, WorkflowStepRepository stepRepo,ActionService actionservice,TriggerService triggerService) {
        this.inputRepo = inputRepo;
        this.stepRepo = stepRepo;
        this.actionservice=actionservice;
        this.triggerService=triggerService;
    }

    @GetMapping
    public List<WorkflowStepInput> getStepInputs(@PathVariable Long stepId) {
    	System.out.println(inputRepo.findByStepId(stepId));
        return inputRepo.findByStepId(stepId);
    }

    @PostMapping(consumes = "application/json", produces = "application/json")
    @Transactional
    public WorkflowStepInput addOrUpdateStepInput(
            @PathVariable Long stepId,
            @RequestBody WorkflowStepInputDto dto
    ) {
        WorkflowStep step = stepRepo.findById(stepId)
                .orElseThrow(() -> new RuntimeException("Step not found"));

        String stepKey = dto.getStepKey();

        String fieldType = null;

        if (step.getAction() != null) {

            ActionField field = step.getAction().getFields().stream()
                    .filter(f -> f.getKey().equals(stepKey))
                    .findFirst()
                    .orElse(null);

            if (field != null) {
                fieldType = field.getFieldDataType();
            }
        }

        if (fieldType == null && step.getTrigger() != null) {

            TriggerField field = step.getTrigger().getFields().stream()
                    .filter(f -> f.getKey().equals(stepKey))
                    .findFirst()
                    .orElse(null);

            if (field != null) {
                fieldType = field.getFieldDataType();
            }
        }

        if ("dynamic_dropdown".equalsIgnoreCase(fieldType)) {

            if (step.getAction() != null) {
                actionservice.clearDependentStepInputs(stepId, stepKey);
            }

            else if (step.getTrigger() != null) {
                triggerService.clearDependentInputs(stepId, stepKey);
            }

            inputRepo.deleteByStepIdAndStepKeyIn(stepId, Set.of(stepKey));
        }

        WorkflowStepInput input =
                inputRepo.findByStepIdAndStepKey(stepId, stepKey);

        if (input == null) {
            input = new WorkflowStepInput();
            input.setStep(step);
            input.setStepKey(stepKey);
            input.setLabel(stepKey);
            input.setIsRequired(false);
        }

        input.setValue(dto.getValue());

        return inputRepo.save(input);
    }
    @PutMapping("/{inputId}")
    public ResponseEntity<WorkflowStepInput> updateInput(
            @PathVariable Long stepId,
            @PathVariable Long inputId,
            @RequestBody WorkflowStepInput input) {

        return inputRepo.findById(inputId)
                .map(existing -> {
                    existing.setStepKey(input.getStepKey());
                    existing.setLabel(input.getLabel());
                    existing.setIsRequired(input.getIsRequired());
                    existing.setValue(input.getValue());
                    return ResponseEntity.ok(inputRepo.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    @DeleteMapping("/{inputId}")
    public ResponseEntity<Void> deleteInput(@PathVariable Long stepId, @PathVariable Long inputId) {
        return inputRepo.findById(inputId)
                .map(existing -> {
                    inputRepo.delete(existing);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
