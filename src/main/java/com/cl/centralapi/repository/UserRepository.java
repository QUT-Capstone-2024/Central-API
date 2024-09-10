package com.cl.centralapi.repository;

import com.cl.centralapi.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.cl.centralapi.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByIdAndStatus(Long id, UserStatus status);

    List<User> findAllByStatus(UserStatus status);
}

