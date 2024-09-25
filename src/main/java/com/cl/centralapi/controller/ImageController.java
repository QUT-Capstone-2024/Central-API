package com.cl.centralapi.controller;

import com.cl.centralapi.enums.ImageTags;
import com.cl.centralapi.enums.Status;
import com.cl.centralapi.model.Collection;
import com.cl.centralapi.model.Image;
import com.cl.centralapi.security.CustomUserDetails;
import com.cl.centralapi.service.ImageService;
import com.cl.centralapi.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.*;

@RestController
@RequestMapping("/api/images")
@Tag(name = "Image management", description = "Upload and management of images and image collections")
public class ImageController {

    @Autowired
    private ImageService imageService;

    @Autowired
    private UserService userService; // Inject UserService

    @Operation(summary = "Upload images to a collection", description = "This endpoint facilitates the upload of images to image collections.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Upload successful",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content) })
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("userId") Long userId,
                                         @RequestParam("collectionId") Long collectionId,
                                         @RequestParam("file") MultipartFile file,
                                         @RequestParam("tag") ImageTags tag,
                                         @RequestParam(value = "customTag", required = false) String customTag,
                                         @RequestParam(value = "description", required = false) String description,
                                         @RequestParam(value = "descriptionSummary", required = false) String descriptionSummary,
                                         @RequestParam(value = "instanceNumber", required = false, defaultValue = "1") int instanceNumber,
                                         @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        // Validate that the user is allowed to upload
        if (!userService.isAdminOrHarbinger(customUserDetails.getId()) &&
                imageService.isCollectionOwnedByUser(customUserDetails.getId(), collectionId)) {
            return ResponseEntity.status(403).body("You do not have permission to upload images to this collection.");
        }

        try {
            Map<String, Object> response = imageService.uploadImage(userId, collectionId, file, tag, customTag, description, descriptionSummary, instanceNumber);
            return ResponseEntity.created(null).body(response);  // Return full response (URL, confidenceLevels, status)
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error uploading and classifying image", "message", e.getMessage()));
        }
    }


    @Operation(summary = "Get image collections by user ID", description = "This endpoint allows you to retrieve all image collections owned by the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Collections retrieved successfully",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Collection.class)) }),
            @ApiResponse(responseCode = "404", description = "Collections not found",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content) })
    @GetMapping("/collections")
    public ResponseEntity<?> getCollectionsByUserId(@RequestParam("userId") Long userId,
                                                    @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        // Validate that the user can access collections
        if (!userService.isAdminOrHarbinger(customUserDetails.getId()) &&
                imageService.isCollectionOwnedByUser(customUserDetails.getId(), userId)) {
            return ResponseEntity.status(403).body("You do not have permission to access these collections.");
        }

        List<Collection> collections = imageService.getCollectionsByUserId(userId);
        return ResponseEntity.ok(collections);
    }

    @Operation(summary = "Get all images in a collection", description = "Allows all images to be retrieved if the collection is owned or accessible to the current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Images retrieved successfully",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Image.class)) }),
            @ApiResponse(responseCode = "404", description = "Collection not found",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content)})
    @GetMapping("/collections/{collectionId}/images")
    public ResponseEntity<?> getImagesByCollectionId(@PathVariable Long collectionId,
                                                     @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        // Validate access to the collection
        if (!userService.isAdminOrHarbinger(customUserDetails.getId()) &&
                imageService.isCollectionOwnedByUser(customUserDetails.getId(), collectionId)) {
            return ResponseEntity.status(403).body("You do not have permission to access this collection.");
        }

        try {
            List<Image> images = imageService.getImagesByCollectionId(collectionId);
            return ResponseEntity.ok(images);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body("Collection not found");
        }
    }

    @Operation(summary = "Update image details", description = "This endpoint allows you to update the metadata associated with an image")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Update successful",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class)) }),
            @ApiResponse(responseCode = "404", description = "Image not found",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content) })
    @PutMapping("/collections/{collectionId}/images/{imageId}")
    public ResponseEntity<?> updateImage(@PathVariable Long collectionId,
                                         @PathVariable Long imageId,
                                         @RequestBody Image updatedImage,
                                         @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        // Validate that the user can update the image
        if (!userService.isAdminOrHarbinger(customUserDetails.getId()) &&
                imageService.isCollectionOwnedByUser(customUserDetails.getId(), collectionId)) {
            return ResponseEntity.status(403).body("You do not have permission to update this image.");
        }

        try {
            imageService.updateImage(imageId, updatedImage);
            return ResponseEntity.ok("Image updated successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body("Image not found");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error updating image: " + e.getMessage());
        }
    }

    @Operation(summary = "Update collection status", description = "Update the status of a collection manually.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Collection status updated successfully",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class)) }),
            @ApiResponse(responseCode = "404", description = "Collection not found",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content) })
    @PutMapping("/collections/{collectionId}/status")
    public ResponseEntity<?> updateCollectionStatus(@PathVariable Long collectionId,
                                                    @RequestParam Status status,
                                                    @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        // Validate that the user can update the collection status
        if (!userService.isAdminOrHarbinger(customUserDetails.getId()) &&
                imageService.isCollectionOwnedByUser(customUserDetails.getId(), collectionId)) {
            return ResponseEntity.status(403).body("You do not have permission to update this collection's status.");
        }

        try {
            imageService.updateCollectionStatus(collectionId, status);
            return ResponseEntity.ok("Collection status updated successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body("Collection not found");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error updating collection status: " + e.getMessage());
        }
    }


    @Operation(summary = "Delete an image", description = "This endpoint allows you to delete an individual image from a collection.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Image deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Image not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/collections/{collectionId}/images/{imageId}")
    public ResponseEntity<?> deleteImageById(@PathVariable Long collectionId,
                                             @PathVariable Long imageId,
                                             @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        // Validate that the user can delete the image
        if (!userService.isHarbinger(customUserDetails.getId())) {
            return ResponseEntity.status(403).body("You do not have permission to delete this image.");
        }
        try {
            imageService.deleteImage(collectionId, imageId);
            return ResponseEntity.status(204).build(); // No content for successful deletion
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body("Image not found");
        }
    }

    // Archive an image by ID
    @Operation(summary = "Archive an image", description = "This endpoint allows you to archive an individual image in a collection.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Image archived successfully"),
            @ApiResponse(responseCode = "404", description = "Image or Collection not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PatchMapping("/collections/{collectionId}/images/{imageId}/archive")
    public ResponseEntity<?> archiveImageById(@PathVariable Long collectionId,
                                              @PathVariable Long imageId,
                                              @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        if (!userService.isAdminOrHarbinger(customUserDetails.getId()) &&
                !imageService.isCollectionOwnedByUser(customUserDetails.getId(), collectionId)) {
            return ResponseEntity.status(403).body("You do not have permission to archive this image.");
        }


        try {
            // Archive the image by its imageId
            imageService.archiveImageById(imageId);
            return ResponseEntity.status(204).build();  // Return 204 No Content on success
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body("Image or Collection not found");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Internal server error");
        }
    }

    // Un-Archive an image by ID
    @Operation(summary = "Restore an image", description = "This endpoint allows you to restore an individual image in a collection.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Image archived successfully"),
            @ApiResponse(responseCode = "404", description = "Image or Collection not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PatchMapping("/collections/{collectionId}/images/{imageId}/reactivate")
    public ResponseEntity<?> reactivateImageById(@PathVariable Long collectionId,
                                              @PathVariable Long imageId,
                                              @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        if (!userService.isAdminOrHarbinger(customUserDetails.getId()) &&
                !imageService.isCollectionOwnedByUser(customUserDetails.getId(), collectionId)) {
            return ResponseEntity.status(403).body("You do not have permission to archive this image.");
        }

        try {
            imageService.reactivateImageById(imageId);
            return ResponseEntity.status(204).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body("Image or Collection not found");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Internal server error");
        }
    }
}
