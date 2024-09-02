package com.cl.centralapi.controller;

import com.cl.centralapi.model.Collection;
import com.cl.centralapi.service.CollectionService;
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

import java.util.Collections;
import java.util.Optional;

@RestController
@RequestMapping("/api/collections")
@Tag(name = "Collection Management", description = "Endpoints for managing collections")
public class CollectionController {

    @Autowired
    private CollectionService collectionService;

    @Operation(summary = "Create a new collection", description = "This endpoint allows you to create a new collection in the system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Collection created successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Collection.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content)
    })
    @PostMapping
    public ResponseEntity<?> createCollection(@RequestBody Collection collection) {
        Collection savedCollection = collectionService.saveCollection(collection);
        return ResponseEntity.ok(Collections.singletonMap("message", "Collection created successfully"));
    }

    @Operation(summary = "Get collection by ID", description = "This endpoint allows you to retrieve a collection by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Collection retrieved successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Collection.class))}),
            @ApiResponse(responseCode = "404", description = "Collection not found",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content)
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_INTERNAL') and principal.userType == T(com.cl.centralapi.enums.UserType).CL_ADMIN")
    public ResponseEntity<Collection> getCollectionById(@PathVariable Long id) {
        Optional<Collection> collection = collectionService.findById(id);
        return collection.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Update collection", description = "This endpoint allows you to update an existing collection.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Collection updated successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Collection.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Collection not found",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content)
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_INTERNAL') and principal.userType == T(com.cl.centralapi.enums.UserType).CL_ADMIN")
    public ResponseEntity<?> updateCollection(@PathVariable Long id, @RequestBody Collection updatedCollection) {
        try {
            Collection savedCollection = collectionService.updateCollection(id, updatedCollection);
            return ResponseEntity.ok(savedCollection);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body("Collection not found: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error updating collection: " + e.getMessage());
        }
    }

    @Operation(summary = "Delete collection", description = "This endpoint allows you to delete a collection by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Collection deleted successfully",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Collection not found",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content)
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_INTERNAL') and principal.userType == T(com.cl.centralapi.enums.UserType).CL_ADMIN")
    public ResponseEntity<?> deleteCollection(@PathVariable Long id) {
        try {
            collectionService.deleteCollectionById(id);
            return ResponseEntity.status(200).body("Collection deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error deleting collection: " + e.getMessage());
        }
    }
}
