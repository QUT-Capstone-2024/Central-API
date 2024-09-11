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

import java.util.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.core.ParameterizedTypeReference;
import java.io.IOException;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.stream.Collectors;

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

    public URI uploadImage(Long userId, Long collectionId, MultipartFile file, ImageTags tag, String customTag, String description, int instanceNumber) throws IOException {
        // Fetch the User object
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Fetch the existing Collection object by collectionId
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found"));

        // Validate the instance number to ensure it matches the property specs (e.g., number of bathrooms)
        if (!validateInstanceNumber(collection, tag, instanceNumber)) {
            throw new IllegalArgumentException("Invalid instance number for the given tag.");
        }

        // Safeguard against null values for the collection ID
        String key = (collectionId != null ? collectionId.toString() : "unknown") + "/" + file.getOriginalFilename();

        // Upload the image to S3
        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(BUCKET_NAME)
                        .key(key)
                        .build(),
                software.amazon.awssdk.core.sync.RequestBody.fromBytes(file.getBytes()));

        // Construct the S3 image URL
        String imageUrl = "https://" + BUCKET_NAME + ".s3.amazonaws.com/" + key;

        // Create the new Image object
        Image image = new Image(
                imageUrl,  // image URL
                ZonedDateTime.now(),  // upload time
                tag,  // image tag
                instanceNumber,
                generateImageId(),  // image ID
                Status.PENDING,  // image status
                customTag,  // rejection reason
                collection
        );

        // Save the new image to the repository
        imageRepository.save(image);

        // Add the new image to the collection’s images list
        collection.getImages().add(image);
        collectionRepository.save(collection);  // Save the collection with the new image

        // Update the collection status based on the new image
        autoUpdateCollectionStatus(collection.getId());

        // Return the URI for the newly uploaded image
        return URI.create(imageUrl);
    }



    private boolean validateInstanceNumber(Collection collection, ImageTags tag, int instanceNumber) {
        System.out.println("Collection Bathrooms: " + collection.getBathrooms());
        System.out.println("Collection Bedrooms: " + collection.getBedrooms());
        System.out.println("Instance Number: " + instanceNumber);
        System.out.println("Tag: " + tag);

        // For example, if the collection has 3 bathrooms, instanceNumber for bathrooms must be between 1 and 3.
        switch (tag) {
            case BATHROOM:
                return instanceNumber >= 1 && instanceNumber <= collection.getBathrooms();
            case BEDROOM:
                return instanceNumber >= 1 && instanceNumber <= collection.getBedrooms();
            default:
                return true;
        }
    }




    private ResponseEntity<Map<String, Object>> sendImageToFlask(String imageUrl) {
        String flaskApiUrl = "http://localhost:5000/api/image/classify";

        // Prepare the JSON payload with the image URL
        Map<String, String> payload = new HashMap<>();
        payload.put("url", imageUrl);

        // Set up the headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create the HTTP entity containing the headers and the payload
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(payload, headers);

        // Create a RestTemplate instance to send the request
        RestTemplate restTemplate = new RestTemplate();

        // Send the POST request to the Flask API and expect a Map in response
        return restTemplate.exchange(flaskApiUrl, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    private Collection createNewCollection(User user, String propertyAddress) {
        // Validate propertyAddress
        if (propertyAddress == null || propertyAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("Property address must not be null or empty");
        }

        Collection collection = new Collection(
                null, // id (will be auto-generated)
                "Default Description", // propertyDescription
                propertyAddress, // propertyAddress
                "default-collection-id", // collectionId (default or generated)
                0, // propertySize (default)
                user.getId(), // propertyOwnerId
                0, // bedrooms (default)
                0, // bathrooms (default)
                0, // parkingSpaces (default)
                Status.PENDING, // approvalStatus (default)
                "unknown", // propertyType (default)
                new ArrayList<>() // image list (initially empty)
        );
        collection.setUser(user); // Ensure user is associated with the collection
        return collectionRepository.save(collection);
    }

    public List<Collection> getCollectionsByUserId(Long userId) {
        // No need to fetch the user entity, just pass the userId directly
        return collectionRepository.findByUserId(userId);
    }

    public Collection getCollectionById(Long collectionId) {
        return collectionRepository.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found."));
    }

    public List<Image> getImagesByCollectionId(Long collectionId) {
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found"));

        // Fetch images and sort them by their instance_number
        return imageRepository.findByCollectionId(collectionId)
                .stream()
                .sorted(Comparator.comparingInt(Image::getInstanceNumber))  // Sort by instance number
                .collect(Collectors.toList());
    }


    public boolean isCollectionOwnedByUser(Long userId, Long collectionId) {
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found"));

        Long collectionOwnerId = collection.getId();
        return collectionOwnerId != null && collectionOwnerId.equals(userId);
    }

    public boolean isUserAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check if the user is either CL_ADMIN or HARBINGER
        return user.getUserType().equals(UserType.CL_ADMIN) || user.getUserType().equals(UserType.HARBINGER);
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

        // Remove the image from the collection's images list
        collection.getImages().remove(image);
        collectionRepository.save(collection); // Save the updated collection

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
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found"));

        // Find all images associated with the collection
        List<Image> images = collection.getImages(); // Get the images directly from the collection

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
