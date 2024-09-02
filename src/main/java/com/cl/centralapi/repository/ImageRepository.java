package com.cl.centralapi.repository;

import com.cl.centralapi.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findByCollectionId(Long collectionId);
}
