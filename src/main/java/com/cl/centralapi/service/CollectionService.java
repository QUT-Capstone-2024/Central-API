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

        // Only update fields if they are provided (not null)
        if (updatedCollection.getPropertyDescription() != null) {
            existingCollection.setPropertyDescription(updatedCollection.getPropertyDescription());
        }
        if (updatedCollection.getPropertyAddress() != null) {
            existingCollection.setPropertyAddress(updatedCollection.getPropertyAddress());
        }
        if (updatedCollection.getCollectionId() != null) {
            existingCollection.setCollectionId(updatedCollection.getCollectionId());
        }
        if (updatedCollection.getPropertySize() != null) {
            existingCollection.setPropertySize(updatedCollection.getPropertySize());
        }
        if (updatedCollection.getId() != null) {
            existingCollection.setId(updatedCollection.getId());
        }
        if (updatedCollection.getBedrooms() != null) {
            existingCollection.setBedrooms(updatedCollection.getBedrooms());
        }
        if (updatedCollection.getBathrooms() != null) {
            existingCollection.setBathrooms(updatedCollection.getBathrooms());
        }
        if (updatedCollection.getParkingSpaces() != null) {
            existingCollection.setParkingSpaces(updatedCollection.getParkingSpaces());
        }
        if (updatedCollection.getApprovalStatus() != null) {
            existingCollection.setApprovalStatus(updatedCollection.getApprovalStatus());
        }
        if (updatedCollection.getPropertyType() != null) {
            existingCollection.setPropertyType(updatedCollection.getPropertyType());
        }
        if (updatedCollection.getUser() != null) {
            existingCollection.setUser(updatedCollection.getUser()); // Set the user (owner)
        }

        // Handle image updates if necessary
        if (updatedCollection.getImages() != null) {
            existingCollection.setImages(updatedCollection.getImages());
        }

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

    // Check if the collection is owned by the user
    public boolean isCollectionOwnedByUser(Long userId, Long collectionId) {
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found"));

        return collection.getUser().getId().equals(userId);
    }

    // Service method for searching by address
    public List<Collection> searchCollectionsByAddress(String addressQuery) {
        // Search by property address using a case-insensitive match
        return collectionRepository.findByPropertyAddressContainingIgnoreCase(addressQuery);
    }

    // Archive a collection
    public void archiveCollectionById(Long collectionId) {
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found"));
        collection.setStatus("ARCHIVED");
        collectionRepository.save(collection);
    }

    // Un-archive a collection
    public void reactivateCollectionById(Long collectionId) {
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found"));
        collection.setStatus("ACTIVE");
        collectionRepository.save(collection);
    }
}
