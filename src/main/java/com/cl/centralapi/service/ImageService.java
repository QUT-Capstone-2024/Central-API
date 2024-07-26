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
        Image image = new Image("https://" + BUCKET_NAME + ".s3.amazonaws.com/" + key, collection, tag, status, customTag, description);
        imageRepository.save(image);

        return URI.create(image.getUrl());
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
                new ArrayList<>() // extraFeatures
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
    }
}
