package com.springrest.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.springrest.Entities.Trigger;

import java.util.List;

public interface TriggerRepository extends JpaRepository<Trigger, Long> {
    List<Trigger> findByConnectorId(Long connectorId);
}
