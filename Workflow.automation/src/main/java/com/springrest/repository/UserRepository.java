package com.springrest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.springrest.Entities.*;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByEmail(String email);
}