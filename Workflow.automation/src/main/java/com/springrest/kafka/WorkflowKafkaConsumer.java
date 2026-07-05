package com.springrest.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.springrest.services.WorkflowExecutionService;

@Service
public class WorkflowKafkaConsumer {

    @Autowired
    private WorkflowExecutionService executionService;

    @KafkaListener(
        topics = "workflow-topic",
        groupId = "workflow-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(WorkflowEvent event) {
        System.out.println("📥 Received event from Kafka: " + event.getWorkflowId());

        try {
            executionService.executeWorkflow(
                event.getWorkflowId(),
                event.getPayload()
            );
        } catch (Exception e) {
            System.out.println("❌ Error processing workflow: " + e.getMessage());
            e.printStackTrace();
        }
    }
}