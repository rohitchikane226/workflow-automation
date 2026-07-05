package com.springrest.services;

import com.springrest.Entities.WorkflowHistory;
import com.springrest.dto.WorkflowHistoryDTO;
import com.springrest.repository.WorkflowHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkflowHistoryService {

    private final WorkflowHistoryRepository historyRepo;

    public WorkflowHistoryService(WorkflowHistoryRepository historyRepo) {
        this.historyRepo = historyRepo;
    }

    public List<WorkflowHistoryDTO> getWorkflowHistory(Long workflowId) {
        return historyRepo.findByStepWorkflowIdOrderByCreatedAtDesc(workflowId).stream()
        		        .map(h -> new WorkflowHistoryDTO(
        		                h.getId(),
        		                h.getRun() != null ? h.getRun().getId() : null,
        		                h.getStep().getId(),
        		                h.getStatus(),
        		                h.getRequestJson(),
        		                h.getResponseJson(),
        		                h.getCreatedAt()
        		        ))
        		        .toList();
    }

    public List<WorkflowHistory> getStepHistory(Long stepId) {
        return historyRepo.findByStepIdOrderByCreatedAtDesc(stepId);
    }

    public List<WorkflowHistory> getRecentHistory() {
        return historyRepo.findTop50ByOrderByCreatedAtDesc();
    }
}