package com.cl.centralapi.repository;

import com.cl.centralapi.model.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollectionRepository extends JpaRepository<Collection, Long> {

    // Find collections by userId (instead of passing the entire User object)
    List<Collection> findByUserId(Long userId);

    Optional<Collection> findByUserIdAndPropertyAddress(Long userId, String propertyAddress);
}
