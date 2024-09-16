package com.cl.centralapi.service;

import com.cl.centralapi.model.Collection;
import com.cl.centralapi.repository.CollectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class RecommendedImagesService {

    @Autowired
    private CollectionRepository collectionRepository;

    // This method generates the list of recommended image titles based on the collectionId
    public List<String> getRecommendedImages(Long collectionId) {
        // Fetch the collection using collectionId
        Collection collection = collectionRepository.findById(collectionId).orElse(null);

        if (collection == null) {
            // If the collection doesn't exist, return an empty list or throw an exception
            return new ArrayList<>();
        }

        // Generate the recommended images
        List<String> recommendedImages = new ArrayList<>();

        // Add "Hero Image"
        recommendedImages.add("Hero Image");

        // Add "Bedroom x" for each bedroom
        int numberOfBedrooms = collection.getBedrooms();
        for (int i = 1; i <= numberOfBedrooms; i++) {
            recommendedImages.add("Bedroom " + i);
        }

        // Add "Bathroom y" for each bathroom
        int numberOfBathrooms = collection.getBathrooms();
        for (int i = 1; i <= numberOfBathrooms; i++) {
            recommendedImages.add("Bathroom " + i);
        }

        // Add "Kitchen"
        recommendedImages.add("Kitchen");

        return recommendedImages;
    }
}
