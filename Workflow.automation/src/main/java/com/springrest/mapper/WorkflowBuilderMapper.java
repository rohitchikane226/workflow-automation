package com.springrest.mapper;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.springrest.Entities.*;
import com.springrest.dto.*;

public class WorkflowBuilderMapper {

    private WorkflowBuilderMapper() {
        
    }
    public static WorkflowBuilderResponse toDto(Workflow workflow) {

        WorkflowBuilderResponse dto = new WorkflowBuilderResponse();
        dto.setId(workflow.getId());
        dto.setName(workflow.getName());
        dto.setActive(workflow.isActive());

        List<WorkflowStepBuilderResponse> stepDtos = new ArrayList<>();

        if (workflow.getSteps() != null) {
            workflow.getSteps().stream()
                .sorted(Comparator.comparingInt(WorkflowStep::getStepOrder))
                .forEach(step -> stepDtos.add(toStepDto(step)));
        }

        dto.setSteps(stepDtos);
        return dto;
    }
    private static WorkflowStepBuilderResponse toStepDto(WorkflowStep step) {
        WorkflowStepBuilderResponse dto = new WorkflowStepBuilderResponse();
        dto.setId(step.getId());
        dto.setStepOrder(step.getStepOrder());
        if (step.getTrigger() != null) {
            dto.setTrigger(toTriggerDto(step.getTrigger()));
        }

        if (step.getAction() != null) {
            dto.setAction(toActionDto(step.getAction()));
        }
        if (step.getConnection() != null) {
            dto.setConnectionId(step.getConnection().getId());
            dto.setConnectionName(step.getConnection().getName());
        }

        return dto;
    }
    private static TriggerSummaryResponse toTriggerDto(Trigger trigger) {

        TriggerSummaryResponse dto = new TriggerSummaryResponse();
        dto.setId(trigger.getId());
        dto.setName(trigger.getKey());

        if (trigger.getConnector() != null) {
            dto.setConnector(toConnectorDto(trigger.getConnector()));
        }

        return dto;
    }
    private static ActionSummaryResponse toActionDto(Action action) {

        ActionSummaryResponse dto = new ActionSummaryResponse();
        dto.setId(action.getId());
        dto.setName(action.getKey());

        if (action.getConnector() != null) {
            dto.setConnector(toConnectorDto(action.getConnector()));
        }

        return dto;
    }
    private static ConnectorSummaryResponse toConnectorDto(Connector connector) {

        ConnectorSummaryResponse dto = new ConnectorSummaryResponse();
        dto.setId(connector.getId());
        dto.setName(connector.getName());
        dto.setLogoUrl(connector.getLogoUrl()); 

        return dto;
    }
}

