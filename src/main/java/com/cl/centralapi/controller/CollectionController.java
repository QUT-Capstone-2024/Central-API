package com.cl.centralapi.controller;

import com.cl.centralapi.enums.UserType;
import com.cl.centralapi.model.Collection;
import com.cl.centralapi.model.User;
import com.cl.centralapi.security.CustomUserDetails;
import com.cl.centralapi.service.CollectionService;
import com.cl.centralapi.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/collections")
@Tag(name = "Collection Management", description = "Endpoints for managing collections")
public class CollectionController {

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private UserService userService;

    // Get collections for a specific user
    @Operation(summary = "Get collections by user ID", description = "Retrieve all collections for a specific user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Collections retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Collection.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Collection>> getCollectionsByUserId(@PathVariable Long userId, @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        // Only allow admin or the user to retrieve their collections
        if (!customUserDetails.getUserType().equals(UserType.CL_ADMIN) && !customUserDetails.getId().equals(userId)) {
            return ResponseEntity.status(403).body(Collections.emptyList());
        }

        List<Collection> collections = collectionService.findCollectionsByUserId(userId);
        if (collections.isEmpty()) {
            return ResponseEntity.status(404).body(Collections.emptyList());
        }
        return ResponseEntity.ok(collections);
    }

    // Get all collections (Admin only)
    @Operation(summary = "Get all collections", description = "Retrieve all collections, accessible only by admin users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Collections retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Collection.class))),
            @ApiResponse(responseCode = "403", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @GetMapping("/all")
    public ResponseEntity<List<Collection>> getAllCollections(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        if (!customUserDetails.getUserType().equals(UserType.CL_ADMIN)) {
            return ResponseEntity.status(403).build();  // Only allow admins
        }

        List<Collection> collections = collectionService.findAllCollections();
        return ResponseEntity.ok(collections);
    }

    // Create a new collection
    @Operation(summary = "Create a new collection", description = "This endpoint allows you to create a new collection in the system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Collection created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Collection.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PostMapping
    public ResponseEntity<Collection> createCollection(@RequestBody Collection collection,
                                                       @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        // Get the user ID from the authenticated user's details
        Long userId = customUserDetails.getId();

        // Fetch the User object based on the userId
        User user = userService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Set the User object in the Collection entity
        collection.setUser(user);  // Link the collection to the authenticated user

        // Save the collection
        Collection savedCollection = collectionService.saveCollection(collection);

        return ResponseEntity.status(201).body(savedCollection); // Return the created collection
    }


    // Get collection by ID
    @Operation(summary = "Get collection by ID", description = "This endpoint allows you to retrieve a collection by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Collection retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Collection.class))),
            @ApiResponse(responseCode = "404", description = "Collection not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<Collection> getCollectionById(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Optional<Collection> collection = collectionService.findById(id);

        if (collection.isPresent()) {
            // Only allow access if the user is an admin or the owner of the collection
            Collection col = collection.get();
            if (!customUserDetails.getUserType().equals(UserType.CL_ADMIN) && !col.getId().equals(customUserDetails.getId())) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.ok(col);
        }

        return ResponseEntity.notFound().build();
    }

    // Update an existing collection
    @Operation(summary = "Update collection", description = "This endpoint allows you to update an existing collection.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Collection updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Collection.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
            @ApiResponse(responseCode = "404", description = "Collection not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCollection(@PathVariable Long id, @RequestBody Collection updatedCollection, @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Optional<Collection> collection = collectionService.findById(id);

        if (collection.isPresent()) {
            // Only allow admins or the owner to update the collection
            Collection col = collection.get();
            if (!customUserDetails.getUserType().equals(UserType.CL_ADMIN) && !col.getId().equals(customUserDetails.getId())) {
                return ResponseEntity.status(403).body("You do not have permission to update this collection.");
            }

            Collection savedCollection = collectionService.updateCollection(id, updatedCollection);
            return ResponseEntity.ok(savedCollection);
        } else {
            return ResponseEntity.status(404).body("Collection not found");
        }
    }

    // Delete collection by ID
    @Operation(summary = "Delete collection", description = "This endpoint allows you to delete a collection by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Collection deleted successfully", content = @Content),
            @ApiResponse(responseCode = "404", description = "Collection not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCollection(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Optional<Collection> collection = collectionService.findById(id);

        if (collection.isPresent()) {
            // Only allow admins or the owner to delete the collection
            Collection col = collection.get();
            if (!customUserDetails.getUserType().equals(UserType.CL_ADMIN) && !col.getId().equals(customUserDetails.getId())) {
                return ResponseEntity.status(403).body("You do not have permission to delete this collection.");
            }

            collectionService.deleteCollectionById(id);
            return ResponseEntity.status(204).build();
        } else {
            return ResponseEntity.status(404).body("Collection not found");
        }
    }
}
