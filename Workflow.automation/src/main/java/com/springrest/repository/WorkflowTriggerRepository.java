package com.springrest.repository;

import com.springrest.Entities.Connection;
import com.springrest.Entities.Trigger;
import com.springrest.Entities.Workflow;
import com.springrest.Entities.WorkflowTrigger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowTriggerRepository
        extends JpaRepository<WorkflowTrigger, Long> {

    /* =========================================================
       Core lookups
       ========================================================= */

    // One workflow + one trigger = one webhook
    Optional<WorkflowTrigger> findByWorkflowAndTrigger(
            Workflow workflow,
            Trigger trigger
    );

    // Used by webhook receiver endpoint
    Optional<WorkflowTrigger> findByWebhookUuid(
            String webhookUuid
    );

    /* =========================================================
       Cleanup / lifecycle operations
       ========================================================= */

    // When a connection/account is deleted
    List<WorkflowTrigger> findByWorkflow_Steps_Connection(
            Connection connection
    );

    // Get all triggers for a workflow
    List<WorkflowTrigger> findByWorkflow(
            Workflow workflow
    );

    /* =========================================================
       Status-based queries (optional but useful)
       ========================================================= */

    List<WorkflowTrigger> findByStatus(String status);

    List<WorkflowTrigger> findByWorkflowAndStatus(
            Workflow workflow,
            String status
    );
}
