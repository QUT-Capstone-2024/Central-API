package com.cl.centralapi.service;

import com.cl.centralapi.enums.Status;
import com.cl.centralapi.model.Collection;
import com.cl.centralapi.repository.CollectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CollectionService {

    @Autowired
    private CollectionRepository collectionRepository;

    // Method to save a new Collection
    public Collection saveCollection(Collection collection) {
        // Additional checks or modifications can be added here if needed
        return collectionRepository.save(collection);
    }

    // Method to update an existing Collection
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

    // Method to find a Collection by ID
    public Optional<Collection> findById(Long id) {
        return collectionRepository.findById(id);
    }

    // Method to delete a Collection by ID
    public void deleteCollectionById(Long id) {
        collectionRepository.deleteById(id);
    }
}
