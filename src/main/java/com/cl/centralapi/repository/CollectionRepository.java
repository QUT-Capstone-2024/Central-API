package com.cl.centralapi.repository;

import com.cl.centralapi.model.Collection;
import com.cl.centralapi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollectionRepository extends JpaRepository<Collection, Long> {
    Optional<Collection> findByUserAndName(User user, String name);

    List<Collection> findByUser(User user);
}
