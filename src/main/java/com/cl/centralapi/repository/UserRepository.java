package com.cl.centralapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.cl.centralapi.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Add custom queries if needed
}

