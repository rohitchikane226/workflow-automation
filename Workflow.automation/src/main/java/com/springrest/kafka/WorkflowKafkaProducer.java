//package com.springrest.kafka;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.stereotype.Service;
//
//import java.util.Map;
//
//@Service
//public class WorkflowKafkaProducer {
//
//    private static final String TOPIC = "workflow-topic";
//
//    @Autowired
//    private KafkaTemplate<String, WorkflowEvent> kafkaTemplate;
//
//    public void sendWorkflowEvent(Long workflowId, Map<String, Object> payload) {
//        WorkflowEvent event = new WorkflowEvent(workflowId, payload);
//
//        System.out.println("📤 Sending event to Kafka: " + event.getWorkflowId());
//
//        kafkaTemplate.send(TOPIC, event);
//    }
//}