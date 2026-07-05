package com.springrest.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.springrest.Entities.TriggerField;

import java.util.List;

public interface TriggerFieldRepository extends JpaRepository<TriggerField, Long> {
    List<TriggerField> findByTriggerId(Long triggerId);
}
