package com.springrest.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.springrest.Entities.*;
import com.springrest.repository.*;

@Service
public class WorkflowStepOutputService {

	@Autowired
    private final WorkflowStepOutputRepository outputRepo;
	@Autowired
    private final WorkflowStepRepository stepRepo;

    public WorkflowStepOutputService(WorkflowStepOutputRepository outputRepo, WorkflowStepRepository stepRepo) {
        this.outputRepo = outputRepo;
        this.stepRepo = stepRepo;
    }

    public List<WorkflowStepOutput> getOutputsByStep(Long stepId) {
        return outputRepo.findByStepId(stepId);
    }

    public WorkflowStepOutput addOutput(Long stepId, WorkflowStepOutput output) {
        WorkflowStep step = stepRepo.findById(stepId).orElseThrow();
        output.setStep(step);
        return outputRepo.save(output);
    }

    public WorkflowStepOutput updateOutput(Long outputId, WorkflowStepOutput output) {
        WorkflowStepOutput existing = outputRepo.findById(outputId).orElseThrow();
        output.setId(existing.getId());
        output.setStep(existing.getStep());
        return outputRepo.save(output);
    }

    public void deleteOutput(Long outputId) {
        outputRepo.deleteById(outputId);
    }
}
