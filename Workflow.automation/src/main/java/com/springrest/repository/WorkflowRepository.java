package com.springrest.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.springrest.Entities.User;
import com.springrest.Entities.Workflow;

public interface WorkflowRepository extends JpaRepository<Workflow, Long> {

	@Query("""
			SELECT w
			FROM Workflow w
			WHERE w.active = true
			AND w.locked = false
			AND (
			    w.nextPollTime IS NULL
			    OR w.nextPollTime <= CURRENT_TIMESTAMP
			)
						""")
	List<Workflow> findReadyWorkflows(Pageable pageable);

	List<Workflow> findByUser(User user);
}
