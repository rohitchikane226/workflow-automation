package com.springrest.repository;

import com.springrest.Entities.AuthData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthDataRepository extends JpaRepository<AuthData, Long> {

    List<AuthData> findByConnectionId(Long connectionId);
    AuthData findByConnectionIdAndKeyName(Long connectionId,String key);

    void deleteByConnectionId(Long connectionId);
}
