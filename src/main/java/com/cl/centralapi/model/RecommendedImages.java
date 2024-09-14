package com.cl.centralapi.model;

import java.util.ArrayList;
import java.util.List;

public class RecommendedImages {

    // This method takes a Collection object and generates a list of recommended image titles
    public static List<String> getRecommendedImages(Collection collection) {
        List<String> recommendedImages = new ArrayList<>();

        // Add "Hero Image" to the list
        recommendedImages.add("Hero Image");

        // Add "Bedroom x" where x is the number of bedrooms from the collection
        int numberOfBedrooms = collection.getBedrooms();
        for (int i = 1; i <= numberOfBedrooms; i++) {
            recommendedImages.add("Bedroom " + i);
        }

        // Add "Bathroom y" where y is the number of bathrooms from the collection
        int numberOfBathrooms = collection.getBathrooms();
        for (int i = 1; i <= numberOfBathrooms; i++) {
            recommendedImages.add("Bathroom " + i);
        }

        // Add "Kitchen" to the list
        recommendedImages.add("Kitchen");

        return recommendedImages;
    }
}
