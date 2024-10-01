package com.cl.centralapi.repository;

import com.cl.centralapi.model.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollectionRepository extends JpaRepository<Collection, Long> {

    List<Collection> findByUserId(Long userId);

    List<Collection> findByPropertyAddressContainingIgnoreCase(String addressQuery);

    List<Collection> findByUserIdAndStatus(Long userId, String status);
}

