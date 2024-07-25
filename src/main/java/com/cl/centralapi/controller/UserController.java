package com.cl.centralapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.cl.centralapi.model.User;
import com.cl.centralapi.service.UserService;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "Endpoints for managing users")
public class UserController {

    @Autowired
    private UserService userService;

    @Operation(summary = "Create a new user", description = "This endpoint allows you to create a new user in the system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = User.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content) })
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user) {
        User savedUser = userService.saveUser(user);
        return ResponseEntity.ok(savedUser);
    }

    @Operation(summary = "Get user by ID", description = "This endpoint allows you to retrieve a user by their ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User retrieved successfully",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = User.class)) }),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content) })
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_INTERNAL') and principal.userType == T(com.cl.centralapi.enums.UserType).CL_ADMIN or #id == principal.id")
    public ResponseEntity<User> getUserById(@PathVariable Long id, @AuthenticationPrincipal UserDetails currentUser) {
        // Check that the user exists
        return userService.findById(id)
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get user by email", description = "This endpoint allows you to retrieve a user by their email address.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User retrieved successfully",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = User.class)) }),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content) })

    @GetMapping("/email/{email}")
    @PreAuthorize("hasAuthority('ROLE_INTERNAL') and principal.userType == T(com.cl.centralapi.enums.UserType).CL_ADMIN")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        // Fetch the user by email
        return userService.findByEmail(email)
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Update user", description = "This endpoint allows you to update an existing user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = User.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content) })
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_INTERNAL') and principal.userType == T(com.cl.centralapi.enums.UserType).CL_ADMIN or #id == principal.id")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User user, @AuthenticationPrincipal UserDetails currentUser) {
        // Check if the user exists
        User existingUser = userService.findById(id).orElse(null);
        if (existingUser == null) {
            return ResponseEntity.notFound().build();
        }

        // Update fields
        existingUser.setName(user.getName());
        existingUser.setEmail(user.getEmail());
        existingUser.setPassword(user.getPassword());
        existingUser.setPhoneNumber(user.getPhoneNumber());
        existingUser.setUserType(user.getUserType());
        existingUser.setUserRole(user.getUserRole());

        User updatedUser = userService.saveUser(existingUser);
        return ResponseEntity.ok(updatedUser);
    }

    @Operation(summary = "Delete user", description = "This endpoint allows you to delete a user by their ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content) })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('INTERNAL') and principal.userType == T(com.cl.centralapi.enums.UserType).CL_ADMIN")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }
}
