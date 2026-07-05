package com.springrest.repository;

import com.springrest.Entities.WebhookPayload;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookPayloadRepository extends JpaRepository<WebhookPayload, Long> {

    WebhookPayload findTopByWorkflowIdOrderByCreatedAtDesc(Long workflowId);

}