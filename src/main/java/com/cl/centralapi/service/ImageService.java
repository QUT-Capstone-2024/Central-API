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

    public Map<String, Object> uploadImageAndClassify(Long userId, String address, MultipartFile file, ImageTags tag, String customTag, String description) throws IOException {
        // Get the user and collection
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        Collection collection = collectionRepository.findByUserAndAddress(user, address)
                .orElseGet(() -> createNewCollection(user, address));

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

        // Call the Flask API to classify the image and get a JSON response as a Map
        ResponseEntity<Map<String, Object>> response = sendImageToFlask(imageUrl);

        // Combine the URL and the classification result into a single map
        Map<String, Object> result = new HashMap<>();
        result.put("image_url", imageUrl);
        result.put("classification_result", response.getBody());  // Store the JSON object directly

        return result;
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

    private Collection createNewCollection(User user, String address) {
        Collection collection = new Collection(
                user, // user
                null, // images
                null, // status
                address, // address
                0.0, // price
                "default description", // description
                0, // internalSize
                0, // bedrooms
                0, // bathrooms
                null, // aircon
                null, // heating
                null, // parking
                null, // externalSize
                null, // levels
                null, // pool
                new ArrayList<>(), // extraFeatures
                0.0 // approvedPercentage
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

    public boolean isCollectionOwnedByUser(Long userId, Long collectionId) {
        Collection collection = collectionRepository.findById(collectionId).orElseThrow(() -> new IllegalArgumentException("Collection not found"));
        User user = collection.getUser();
        return user != null && user.getId().equals(userId);
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

        if (updatedImage.getUrl() != null) {
            existingImage.setUrl(updatedImage.getUrl());
        }
        if (updatedImage.getTag() != null) {
            existingImage.setTag(updatedImage.getTag());
        }
        if (updatedImage.getCustomTag() != null) {
            existingImage.setCustomTag(updatedImage.getCustomTag());
        }
        if (updatedImage.getDescription() != null) {
            existingImage.setDescription(updatedImage.getDescription());
        }
        if (updatedImage.getStatus() != null) {
            existingImage.setStatus(updatedImage.getStatus());
        }

        imageRepository.save(existingImage);

        // Automatically update the collection status when image status changes
        autoUpdateCollectionStatus(existingImage.getCollection().getId());
    }

    public void updateImageStatus(Long imageId, Status newStatus) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));

        image.setStatus(newStatus);

        // Automatically update the image status when all images in a collection = APPROVED
        autoUpdateCollectionStatus(image.getCollection().getId());
    }

    private void autoUpdateCollectionStatus(Long collectionId) {
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found"));

        List<Image> images = collection.getImages();

        // Counters
        double imageCount = images.size();
        double approvedImageCount = 0;
        boolean hasRejectedImages = false;

        // Update counters based on image status
        for (Image image : images) {
            if (image.getStatus() == Status.APPROVED) {
                approvedImageCount++;
            } else if (image.getStatus() == Status.REJECTED) {
                hasRejectedImages = true;
            }
        }

        // Adjust collection status based on image statuses
        if (hasRejectedImages) {
            collection.setStatus(Status.REJECTED);
        } else if (approvedImageCount == imageCount) {
            collection.setStatus(Status.APPROVED);
        } else {
            collection.setStatus(Status.PENDING);
        }

        // Update the approved % and save
        collection.setApprovedPercentage((approvedImageCount / imageCount) * 100);
        collectionRepository.save(collection);
    }

    public void updateCollection(Long collectionId, Collection updatedCollection) {
        Collection existingCollection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found"));

        if (updatedCollection.getStatus() != null) {
            existingCollection.setStatus(updatedCollection.getStatus());
        }
        if (updatedCollection.getDescription() != null) {
            existingCollection.setDescription(updatedCollection.getDescription());
        }
        // Add other properties as needed

        collectionRepository.save(existingCollection);
    }

    public void updateCollectionStatus(Long collectionId, Status status) {
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found"));

        collection.setStatus(status);
        collectionRepository.save(collection);
    }
}
