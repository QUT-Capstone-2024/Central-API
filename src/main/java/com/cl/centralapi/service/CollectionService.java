package com.cl.centralapi.service;

import com.cl.centralapi.model.Collection;
import com.cl.centralapi.repository.CollectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CollectionService {

    @Autowired
    private CollectionRepository collectionRepository;

    // Save a new collection
    public Collection saveCollection(Collection collection) {
        return collectionRepository.save(collection);
    }

    // Update an existing collection
    public Collection updateCollection(Long id, Collection updatedCollection) {
        Collection existingCollection = collectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found"));

        existingCollection.setPropertyDescription(updatedCollection.getPropertyDescription());
        existingCollection.setPropertyAddress(updatedCollection.getPropertyAddress());
        existingCollection.setImageUrls(updatedCollection.getImageUrls());
        existingCollection.setCollectionId(updatedCollection.getCollectionId());
        existingCollection.setPropertySize(updatedCollection.getPropertySize());
        existingCollection.setPropertyOwnerId(updatedCollection.getPropertyOwnerId());
        existingCollection.setBedrooms(updatedCollection.getBedrooms());
        existingCollection.setBathrooms(updatedCollection.getBathrooms());
        existingCollection.setParkingSpaces(updatedCollection.getParkingSpaces());
        existingCollection.setApprovalStatus(updatedCollection.getApprovalStatus());
        existingCollection.setPropertyType(updatedCollection.getPropertyType());

        return collectionRepository.save(existingCollection);
    }

    // Find a collection by ID
    public Optional<Collection> findById(Long id) {
        return collectionRepository.findById(id);
    }

    // Delete a collection by ID
    public void deleteCollectionById(Long id) {
        collectionRepository.deleteById(id);
    }

    // Find collections by user ID
    public List<Collection> findCollectionsByUserId(Long userId) {
        return collectionRepository.findByUserId(userId);
    }

    // Find all collections (Admin only)
    public List<Collection> findAllCollections() {
        return collectionRepository.findAll();
    }
}
