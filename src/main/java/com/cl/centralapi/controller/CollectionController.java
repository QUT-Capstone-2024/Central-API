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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/collections")
@Tag(name = "Collection Management", description = "Endpoints for managing collections")
public class CollectionController {
    private static final Logger logger = LoggerFactory.getLogger(CollectionController.class);

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
        if (!userService.isAdminOrHarbinger(customUserDetails.getId()) && !customUserDetails.getId().equals(userId)) {
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
        if (!userService.isAdminOrHarbinger(customUserDetails.getId())) {
            return ResponseEntity.status(403).build();
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
        Long userId = customUserDetails.getId();
        User user = userService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        collection.setUser(user);  // Link the collection to the authenticated user
        Collection savedCollection = collectionService.saveCollection(collection);

        return ResponseEntity.status(201).body(savedCollection);
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
            Collection col = collection.get();
            if (!userService.isAdminOrHarbinger(customUserDetails.getId()) &&
                    !collectionService.isCollectionOwnedByUser(customUserDetails.getId(), id)) {
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
        Optional<Collection> collectionOpt = collectionService.findById(id);

        if (collectionOpt.isPresent()) {
            Collection collection = collectionOpt.get();

            // Set the authenticated user as the new owner
            collection.setUser(userService.findById(customUserDetails.getId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found")));

            // Perform the update
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
            Collection col = collection.get();
            if (!userService.isAdminOrHarbinger(customUserDetails.getId()) &&
                    !collectionService.isCollectionOwnedByUser(customUserDetails.getId(), id)) {
                return ResponseEntity.status(403).body("You do not have permission to delete this collection.");
            }

            collectionService.deleteCollectionById(id);
            return ResponseEntity.status(204).build();
        } else {
            return ResponseEntity.status(404).body("Collection not found");
        }
    }

    // Archive collection by ID
    @Operation(summary = "Archive an image collection", description = "This endpoint allows you to archive an image collection along with all images in it.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Collection archived successfully"),
            @ApiResponse(responseCode = "404", description = "Collection not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PatchMapping("/{collectionId}/archive")
    public ResponseEntity<?> archiveCollectionById(@PathVariable Long collectionId,
                                                   @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        if (!userService.isAdminOrHarbinger(customUserDetails.getId()) &&
                !collectionService.isCollectionOwnedByUser(customUserDetails.getId(), collectionId)) {
            return ResponseEntity.status(403).body("You do not have permission to archive this collection.");
        }
        try {
            collectionService.archiveCollectionById(collectionId);
            return ResponseEntity.status(204).build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Internal server error");
        }
    }

    // un-Archive collection by ID
    @Operation(summary = "Archive an image collection", description = "This endpoint allows you to archive an image collection along with all images in it.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Collection archived successfully"),
            @ApiResponse(responseCode = "404", description = "Collection not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PatchMapping("/{collectionId}/reactivate")
    public ResponseEntity<?> reactivateCollectionById(@PathVariable Long collectionId,
                                                   @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        if (!userService.isAdminOrHarbinger(customUserDetails.getId()) &&
                !collectionService.isCollectionOwnedByUser(customUserDetails.getId(), collectionId)) {
            return ResponseEntity.status(403).body("You do not have permission to archive this collection.");
        }
        try {
            collectionService.reactivateCollectionById(collectionId);
            return ResponseEntity.status(204).build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Internal server error");
        }
    }

    // Find a collection by address
    @Operation(summary = "Search collections", description = "Search collections based on a search query")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Collections retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Collection.class))),
            @ApiResponse(responseCode = "404", description = "No collections found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @GetMapping("/search")
    public ResponseEntity<List<Collection>> searchCollections(@RequestParam("address") String address) {
        List<Collection> collections = collectionService.searchCollectionsByAddress(address);
        if (collections.isEmpty()) {
            return ResponseEntity.status(404).body(Collections.emptyList());
        }
        return ResponseEntity.ok(collections);
    }
}

