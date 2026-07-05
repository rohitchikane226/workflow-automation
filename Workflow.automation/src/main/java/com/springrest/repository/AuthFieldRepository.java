package com.springrest.repository;

import com.springrest.Entities.AuthField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthFieldRepository extends JpaRepository<AuthField, Long> {

    List<AuthField> findByConnectorId(Long connectorId);

    AuthField findByConnectorIdAndKeyName(Long connectorId, String keyName);
}
