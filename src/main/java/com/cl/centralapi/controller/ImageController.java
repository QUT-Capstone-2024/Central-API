package com.cl.centralapi.controller;

import com.cl.centralapi.enums.ImageTags;
import com.cl.centralapi.enums.Status;
import com.cl.centralapi.model.Collection;
import com.cl.centralapi.model.Image;
import com.cl.centralapi.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/images")
@Tag(name = "Image management", description = "Upload and management of images and image collections")
public class ImageController {

    @Autowired
    private ImageService imageService;

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
    public ResponseEntity<?> uploadImage(
            @RequestParam("userId") Long userId,
            @RequestParam("address") String address,
            @RequestParam("file") MultipartFile file,
            @RequestParam("tag") ImageTags tag,
            @RequestParam(value = "customTag", required = false) String customTag,
            @RequestParam(value = "description", required = false) String description) {
        try {
            URI location = imageService.uploadImage(userId, address, file, tag, customTag, description);
            return ResponseEntity.created(location).body("Image uploaded successfully: " + location.toString());
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error uploading image: " + e.getMessage());
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
    @PreAuthorize("@imageService.isUserAdmin(principal.userType) or @imageService.isCollectionOwnedByUser(#collectionId, T(com.cl.centralapi.security.CustomUserDetails).id)")
    public ResponseEntity<?> getCollectionsByUserId(@RequestParam("userId") Long userId) {
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
    @PreAuthorize("@imageService.isUserAdmin(principal.userType) or @imageService.isCollectionOwnedByUser(#collectionId, T(com.cl.centralapi.security.CustomUserDetails).id)")
    public ResponseEntity<?> getImagesByCollectionId(@PathVariable Long collectionId) {
        try {
            List<Image> images = imageService.getImagesByCollectionId(collectionId);
            return ResponseEntity.ok(images);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body("Collection not found");
        }
    }

    @Operation(summary = "Delete an image collection", description = "This endpoint allows you to delete an image collection along with all images in it.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Collection deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Collection not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/collections/{collectionId}")
    @PreAuthorize("@imageService.isUserAdmin(principal.userType) or @imageService.isCollectionOwnedByUser(#collectionId, T(com.cl.centralapi.security.CustomUserDetails).id)")
    public ResponseEntity<?> deleteCollectionById(@PathVariable Long collectionId) {
        try {
            imageService.deleteCollectionById(collectionId);
            return ResponseEntity.status(204).build(); // No content for successful deletion
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body("Collection not found");
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
    @PreAuthorize("@imageService.isUserAdmin(principal.userType) or @imageService.isCollectionOwnedByUser(#collectionId, T(com.cl.centralapi.security.CustomUserDetails).id)")
    public ResponseEntity<?> deleteImageById(@PathVariable Long collectionId, @PathVariable Long imageId) {
        try {
            imageService.deleteImage(collectionId, imageId);
            return ResponseEntity.status(204).build(); // No content for successful deletion
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body("Image not found");
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
    @PreAuthorize("@imageService.isUserAdmin(principal.userType) or @imageService.isCollectionOwnedByUser(#collectionId, T(com.cl.centralapi.security.CustomUserDetails).id)")
    public ResponseEntity<?> updateImage(@PathVariable Long collectionId, @PathVariable Long imageId, @RequestBody Image updatedImage) {
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
    @PreAuthorize("principal.userType == T(com.cl.centralapi.enums.UserType).CL_ADMIN or @imageService.isCollectionOwnedByUser(#collectionId, principal.id)")
    public ResponseEntity<?> updateCollectionStatus(@PathVariable Long collectionId, @RequestParam Status status) {
        try {
            imageService.updateCollectionStatus(collectionId, status);
            return ResponseEntity.ok("Collection status updated successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body("Collection not found");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error updating collection status: " + e.getMessage());
        }
    }
}