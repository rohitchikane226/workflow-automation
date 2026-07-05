package com.springrest.repository;

import com.springrest.Entities.Connection;
import com.springrest.Entities.Connector;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectionRepository extends JpaRepository<Connection, Long> {

    List<Connection> findByConnectorId(Long connectorId);
    @Query("SELECT c FROM Connection c WHERE c.connector.id = :connectorId ORDER BY c.id DESC LIMIT 1")
    Optional<Connection> findLatestConnectionByConnectorId(Long connectorId);

    @Query("SELECT c FROM Connector c WHERE c.id = :id")
    Connector findConnectorById(Long id);
    boolean existsByName(String name);
    @Query("""
    	    SELECT c FROM Connection c
    	    JOIN FETCH c.connector conn
    	    WHERE c.id = :id
    	""")
    	Optional<Connection> findWithConnector(Long id);
    // Get connection by user + connector (optional if you add userId)
    // List<Connection> findByUserId(Long userId);
}
