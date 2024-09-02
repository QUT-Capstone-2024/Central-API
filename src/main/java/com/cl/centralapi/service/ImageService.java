package com.cl.centralapi.service;

import com.cl.centralapi.enums.ImageTags;
import com.cl.centralapi.enums.Status;
import com.cl.centralapi.enums.UserType;
import com.cl.centralapi.model.Collection;
import com.cl.centralapi.model.Image;
import com.cl.centralapi.model.User;
import com.cl.centralapi.repository.CollectionRepository;
import com.cl.centralapi.repository.ImageRepository;
import com.cl.centralapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ImageService {

    @Autowired
    private S3Client s3Client;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CollectionRepository collectionRepository;

    @Autowired
    private ImageRepository imageRepository;

    private final String BUCKET_NAME = "visioncore-image-bucket";

    public URI uploadImage(Long userId, String address, MultipartFile file, ImageTags tag, String customTag, String description) throws IOException {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        Collection collection = collectionRepository.findByUserAndAddress(user, address)
                .orElseGet(() -> createNewCollection(user, address));

        String key = collection.getId() + "/" + file.getOriginalFilename();
        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(BUCKET_NAME)
                        .key(key)
                        .build(),
                software.amazon.awssdk.core.sync.RequestBody.fromBytes(file.getBytes()));

        // Ensure a tag is provided (either from the enum or a custom tag)
        if (tag == null && (customTag == null || customTag.isEmpty())) {
            throw new IllegalArgumentException("An image must have either a tag or a custom tag.");
    }

    // Default status is PENDING
    Status status = Status.PENDING;

        // Create the Image instance with the new constructor
        Image image = new Image(
                "https://" + BUCKET_NAME + ".s3.amazonaws.com/" + key, // imageUrl
                ZonedDateTime.now(), // uploadTime
                tag, // imageTag
                generateImageId(), // imageId
                status, // imageStatus
                customTag, // rejectionReason (if you want to use customTag as rejectionReason, otherwise set it to null)
                collection // collection
        );

        // Save the image to the repository

        imageRepository.save(image);

    // Recalculate collection status
        autoUpdateCollectionStatus(collection.getId());

        return URI.create(image.getImageUrl());
    }

    private Collection createNewCollection(User user, String propertyAddress) {
        Collection collection = new Collection(
                null, // id (will be auto-generated)
                "default description", // propertyDescription
                propertyAddress, // propertyAddress
                new ArrayList<>(), // imageUrls (default empty list)
                "default-collection-id", // collectionId (default or generated)
                0, // propertySize (default)
                user.getId(), // propertyOwnerId (assuming the user's ID is used here)
                0, // bedrooms (default)
                0, // bathrooms (default)
                0, // parkingSpaces (default)
                Status.PENDING, // approvalStatus (default)
                "unknown" // propertyType (default, change as needed)
        );
        return collectionRepository.save(collection);
    }

    public List<Collection> getCollectionsByUserId(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        return collectionRepository.findByUser(user);
    }

    public Collection getCollectionById(Long collectionId) {
        return collectionRepository.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found."));
    }

    public List<Image> getImagesByCollectionId(Long collectionId) {
        // Ensure the collection exists
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found"));

        // Fetch and return all images for this collection
        return imageRepository.findByCollectionId(collectionId);
    }

    public boolean isCollectionOwnedByUser(String userId, Long collectionId) {
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found"));

        Long collectionOwnerId = collection.getPropertyOwnerId();
        return collectionOwnerId != null && collectionOwnerId.equals(userId);
    }

    public boolean isUserAdmin(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        return user != null && user.getUserType().equals(UserType.CL_ADMIN);
    }

    public void deleteCollectionById(Long collectionId) {
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found"));
        collectionRepository.delete(collection);
    }

    public void deleteImage(Long collectionId, Long imageId) {
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found"));
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));
        if (!image.getCollection().getId().equals(collectionId)) {
            throw new IllegalArgumentException("Image is not owned by collection");
        }
        imageRepository.delete(image);

        // Recalculate collection status
        autoUpdateCollectionStatus(collection.getId());
    }

    public void updateImage(Long imageId, Image updatedImage) {
        Image existingImage = imageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));

        // Update fields if provided
        if (updatedImage.getImageUrl() != null) {
            existingImage.setImageUrl(updatedImage.getImageUrl());
        }
        if (updatedImage.getImageTag() != null) {
            existingImage.setImageTag(updatedImage.getImageTag());
        }
        if (updatedImage.getRejectionReason() != null) {
            existingImage.setRejectionReason(updatedImage.getRejectionReason());
        }
        if (updatedImage.getImageStatus() != null) {
            existingImage.setImageStatus(updatedImage.getImageStatus());
        }

        imageRepository.save(existingImage);

        // Automatically update the collection status when image status changes
        autoUpdateCollectionStatus(existingImage.getCollection().getId());
    }

    public void updateImageStatus(Long imageId, Status newStatus) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));

        image.setImageStatus(newStatus);

        // Automatically update the image status when all images in a collection = APPROVED
        autoUpdateCollectionStatus(image.getCollection().getId());
    }

    private void autoUpdateCollectionStatus(Long collectionId) {
        // Find the collection by ID
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found"));

        // Find all images associated with the collection
        List<Image> images = imageRepository.findByCollectionId(collectionId);

        // Counters
        double imageCount = images.size();
        double approvedImageCount = 0;
        boolean hasRejectedImages = false;

        // Update counters based on image status
        for (Image image : images) {
            if (image.getImageStatus() == Status.APPROVED) {
                approvedImageCount++;
            } else if (image.getImageStatus() == Status.REJECTED) {
                hasRejectedImages = true;
            }
        }

        // Adjust collection status based on image statuses
        if (hasRejectedImages) {
            collection.setApprovalStatus(Status.REJECTED);
        } else if (approvedImageCount == imageCount) {
            collection.setApprovalStatus(Status.APPROVED);
        } else {
            collection.setApprovalStatus(Status.PENDING);
        }

        // Save the updated collection
        collectionRepository.save(collection);
    }

    public void updateCollection(Long collectionId, Collection updatedCollection) {
        Collection existingCollection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found"));

        if (updatedCollection.getApprovalStatus() != null) {
            existingCollection.setApprovalStatus(updatedCollection.getApprovalStatus());
        }
        if (updatedCollection.getPropertyDescription() != null) {
            existingCollection.setPropertyDescription(updatedCollection.getPropertyDescription());
        }
        // Add other properties as needed

        collectionRepository.save(existingCollection);
    }

    public void updateCollectionStatus(Long collectionId, Status status) {
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found"));

        collection.setApprovalStatus(status);
        collectionRepository.save(collection);
    }

    private String generateImageId() {
        // Generate a unique image ID, implementation can vary
        return "img" + System.currentTimeMillis(); // Example implementation
    }
}
