package com.springrest.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.springrest.Entities.ActionField;

import java.util.List;

public interface ActionFieldRepository extends JpaRepository<ActionField, Long> {
    List<ActionField> findByActionId(Long actionId);
}
