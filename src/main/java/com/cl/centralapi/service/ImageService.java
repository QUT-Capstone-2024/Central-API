package com.cl.centralapi.service;

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

    public URI uploadImage(Long userId, String collectionName, MultipartFile file) throws IOException {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        Collection collection = collectionRepository.findByUserAndName(user, collectionName)
                .orElseGet(() -> createNewCollection(user, collectionName));

        String key = collection.getId() + "/" + file.getOriginalFilename();
        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(BUCKET_NAME)
                        .key(key)
                        .build(),
                software.amazon.awssdk.core.sync.RequestBody.fromBytes(file.getBytes()));

        Image image = new Image("https://" + BUCKET_NAME + ".s3.amazonaws.com/" + key, collection);
        imageRepository.save(image);

        return URI.create(image.getUrl());
    }

    private Collection createNewCollection(User user, String collectionName) {
        Collection collection = new Collection(collectionName, user);
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
}
