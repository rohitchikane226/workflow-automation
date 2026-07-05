package com.springrest.repository;

import java.util.List;
import java.util.Optional;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.springrest.Entities.Action;

//import ch.qos.logback.core.joran.action.Action;
@Repository
public interface ActionRepository extends JpaRepository<Action, Long> {
    List<Action> findByConnectorId(Long connectorId);

	Optional<Action> findByActionKey(String actionKey);
}
