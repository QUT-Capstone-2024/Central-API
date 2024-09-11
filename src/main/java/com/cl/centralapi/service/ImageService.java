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
import java.util.Map;
import java.util.HashMap;
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

<<<<<<< Updated upstream
    public Map<String, Object> uploadImageAndClassify(Long userId, String address, MultipartFile file, ImageTags tag, String customTag, String description) throws IOException {
        // Get the user and collection
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        Collection collection = collectionRepository.findByUserAndPropertyAddress(user, address)
                .orElseGet(() -> createNewCollection(user, address));
=======
    public Map<String, Object> uploadImage(Long userId, Long collectionId, MultipartFile file, ImageTags tag, String customTag, String description) throws IOException {
        // Fetch the User object
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Fetch the existing Collection object by collectionId
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found"));
>>>>>>> Stashed changes

        // Safeguard against null values for the collection ID
        Long collectionId = collection.getId();
        String key = (collectionId != null ? collectionId.toString() : "unknown") + "/" + file.getOriginalFilename();

        // Upload the image to S3
        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(BUCKET_NAME)
                        .key(key)
                        .build(),
                software.amazon.awssdk.core.sync.RequestBody.fromBytes(file.getBytes()));

        String imageUrl = "https://" + BUCKET_NAME + ".s3.ap-southeast-2.amazonaws.com/" + key;

<<<<<<< Updated upstream
        // Call the Flask API to classify the image and get a JSON response as a Map
        ResponseEntity<Map<String, Object>> response = sendImageToFlask(imageUrl);
=======
        // Log the image URL for debugging
        System.out.println("Image URL: " + imageUrl);

        // Create the new Image object with initial status as PENDING
        Image image = new Image(
                imageUrl,  // image URL
                ZonedDateTime.now(),  // upload time
                tag,  // image tag
                generateImageId(),  // image ID
                Status.PENDING,  // image status (initially pending)
                customTag,  // rejection reason (if any)
                collection  // associate with the collection
        );
>>>>>>> Stashed changes

        // Combine the URL and the classification result into a single map
        Map<String, Object> result = new HashMap<>();
        result.put("image_url", imageUrl);
        result.put("classification_result", response.getBody());  // Store the JSON object directly

<<<<<<< Updated upstream
        return result;
=======
        // Add the new image to the collection’s images list
        collection.getImages().add(image);
        collectionRepository.save(collection);  // Save the collection with the new image

        // Call the Flask API to classify the image and get the confidence levels
        ResponseEntity<Map<String, Object>> flaskResponse = sendImageToFlask(imageUrl);

        if (!flaskResponse.getStatusCode().is2xxSuccessful()) {
            // Handle the case where the Flask API fails to classify the image
            throw new IOException("Flask API classification failed with status: " + flaskResponse.getStatusCode());
        }

        // Log the Flask API response
        System.out.println("Flask API response: " + flaskResponse.getBody());

        // Extract the main response body
        Map<String, Object> responseBody = flaskResponse.getBody();

        // Extract nested confidence scores
        @SuppressWarnings("unchecked")
        Map<String, Object> confidenceScores = (Map<String, Object>) ((Map<String, Object>) responseBody.get("confidence_scores"));

        // Log the confidence scores for debugging
        System.out.println("Confidence Scores: " + confidenceScores);

        // Initialize variables to track the highest confidence and associated tag
        String highestConfidenceTag = null;
        double highestConfidenceScore = 0.0;

        // Iterate over the confidence scores to find the highest score
        for (Map.Entry<String, Object> entry : confidenceScores.entrySet()) {
            String classifiedTag = entry.getKey();  // e.g., "Bathroom"
            Object value = entry.getValue();

            // Check if the value is a number (Double)
            if (value instanceof Number) {
                double confidence = ((Number) value).doubleValue();

                // Log the confidence score for debugging
                System.out.println("Classified Tag: " + classifiedTag + ", Confidence Score: " + confidence);

                // Track the highest confidence score and its tag
                if (confidence > highestConfidenceScore) {
                    highestConfidenceScore = confidence;
                    highestConfidenceTag = classifiedTag;
                }
            }
        }

        // If the highest confidence score is above 0.8, check if it matches the uploaded tag
        if (highestConfidenceScore >= 0.8) {
            if (highestConfidenceTag.equalsIgnoreCase(tag.toString())) {
                // The confidence is high and matches the uploaded tag, so approve the image
                image.setImageStatus(Status.APPROVED);
            } else {
                // The confidence is high but does not match the uploaded tag, so update the tag and set the status to pending
                image.setImageTag(ImageTags.valueOf(highestConfidenceTag.toUpperCase()));  // Update to the correct tag
                image.setImageStatus(Status.PENDING);
                image.setRejectionReason("Model confident in another tag, thus updated tag");
            }
        } else {
            System.out.print(highestConfidenceScore);
            // If no confidence score is above 0.8, the image remains rejected
            image.setImageStatus(Status.REJECTED);
            image.setRejectionReason("No confidence score above 0.8");
        }

        // Save the updated image status and tag
        imageRepository.save(image);

        // Update the collection status based on the new image
        autoUpdateCollectionStatus(collection.getId());

        // Prepare the response to include both the image URL, confidence levels, and status
        Map<String, Object> response = new HashMap<>();
        response.put("imageUrl", imageUrl);
        response.put("confidenceLevels", confidenceScores);
        response.put("imageStatus", image.getImageStatus());

        // Return the response map
        return response;
>>>>>>> Stashed changes
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
                new ArrayList<>(), // imageUrls (default empty list)
                "default-collection-id", // collectionId (default or generated)
                0, // propertySize (default)
                user.getId(), // propertyOwnerId
                0, // bedrooms (default)
                0, // bathrooms (default)
                0, // parkingSpaces (default)
                Status.PENDING, // approvalStatus (default)
                "unknown" // propertyType (default)
        );
        collection.setUser(user); // Ensure user is associated with the collection
        return collectionRepository.save(collection);
    }


    public List<Collection> getCollectionsByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
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

    public boolean isCollectionOwnedByUser(Long userId, Long collectionId) {
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found"));

        Long collectionOwnerId = collection.getPropertyOwnerId();
        return collectionOwnerId != null && collectionOwnerId.equals(userId);
    }

    public boolean isUserAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return user.getUserType().equals(UserType.CL_ADMIN);
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
