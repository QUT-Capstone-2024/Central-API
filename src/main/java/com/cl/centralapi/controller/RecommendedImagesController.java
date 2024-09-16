package com.cl.centralapi.controller;

import com.cl.centralapi.service.RecommendedImagesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommended-images")
public class RecommendedImagesController {

    @Autowired
    private RecommendedImagesService recommendedImagesService;

    // API endpoint to get recommended images by collectionId
    @GetMapping("/{collectionId}")
    public List<String> getRecommendedImages(@PathVariable Long collectionId) {
        return recommendedImagesService.getRecommendedImages(collectionId);
    }
}
