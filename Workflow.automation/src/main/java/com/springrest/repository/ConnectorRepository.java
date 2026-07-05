package com.springrest.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.springrest.Entities.Connector;

public interface ConnectorRepository extends JpaRepository<Connector, Long> {
	Optional<Connector> findByNameIgnoreCase(String name);
	Optional<Connector> findByAppKeyIgnoreCase(String appKey);

}


/*
public interface ConnectorRepository extends JpaRepository<Connector, Long> {

Optional<Connector> findByAppKeyIgnoreCase(String appKey);

// optional if still needed elsewhere
Optional<Connector> findByNameIgnoreCase(String name);
}
*/