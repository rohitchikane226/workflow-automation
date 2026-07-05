package com.springrest.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springrest.Entities.Connection;
import com.springrest.Entities.Trigger;
import com.springrest.Entities.Workflow;
import com.springrest.Entities.WorkflowTrigger;
import com.springrest.repository.WorkflowTriggerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class WebhookService {

    @Autowired
    private ScriptEngineService scriptEngineService;

    @Autowired
    private WorkflowTriggerRepository workflowTriggerRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Transactional
    public WorkflowTrigger subscribe(
            Workflow workflow,
            Trigger trigger,
            Connection connection,
            Map<String, Object> triggerFields
    ) throws JsonProcessingException {

        Optional<WorkflowTrigger> existing =
                workflowTriggerRepository
                        .findByWorkflowAndTrigger(workflow, trigger);
        if (existing.isPresent()
                && "ENABLED".equals(existing.get().getStatus())) {

            unsubscribe(existing.get(), connection);
        }

        String uuid = UUID.randomUUID().toString(); 
//        String webhookUrl =  
//        		"http://localhost:8080/webhooks/" + uuid;
        String webhookUrl="https://unseditiously-contrarious-melody.ngrok-free.dev/webhooks/"+uuid;

        Map<String, Object> input = new HashMap<>();

     // keep webhook url separate
     input.put("WebhookUrl", webhookUrl);

     // convert only trigger fields to RawBody
     String rawBodyJson = objectMapper.writeValueAsString(triggerFields);

     input.put("RawBody", rawBodyJson);
     System.out.println("Webhook request object: " + input.toString());
     Map<String, Object> jsResponse =
             scriptEngineService.executeWebhookScript(
                     trigger.getScript(),
                     "subscribe",
                     input,
                     connection
             );


        if (!"true".equalsIgnoreCase(
                String.valueOf(jsResponse.get("Success")))) {
            throw new RuntimeException("Webhook subscribe failed");
        }

        WorkflowTrigger wt =
                existing.orElse(new WorkflowTrigger());

        wt.setWorkflow(workflow);
        wt.setTrigger(trigger);
        wt.setWebhookUuid(uuid);
        wt.setExternalWebhookId(
                String.valueOf(jsResponse.get("WebhookId"))
        );
        wt.setSubscribeResponse(toJson(jsResponse));
        wt.setStatus("ENABLED");

        return workflowTriggerRepository.save(wt);
    }
    @Transactional
    public void unsubscribe(
            WorkflowTrigger workflowTrigger,
            Connection connection
    ) {

        if (!"ENABLED".equals(workflowTrigger.getStatus())) {
            return;
        }

        scriptEngineService.executeWebhookScript(
                workflowTrigger.getTrigger().getScript(),
                "unsubscribe",
                Map.of(
                        "subscribeWebhook",
                        workflowTrigger.getSubscribeResponse()
                ),
                connection
        );

        workflowTrigger.setStatus("DISABLED");
        workflowTriggerRepository.save(workflowTrigger);
    }

    /* =========================================================
       Helper
       ========================================================= */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON serialize error", e);
        }
    }
}
