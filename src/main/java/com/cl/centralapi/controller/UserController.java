package com.cl.centralapi.controller;

import com.cl.centralapi.enums.UserStatus;
import com.cl.centralapi.enums.UserType;
import com.cl.centralapi.exceptions.EmailAlreadyUsedException;
import com.cl.centralapi.exceptions.UserNotFoundException;
import com.cl.centralapi.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.cl.centralapi.model.User;
import com.cl.centralapi.service.UserService;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "Endpoints for managing users")
public class UserController {

    @Autowired
    private UserService userService;

    // No restriction on creating a user
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
        return ResponseEntity.ok(Collections.singletonMap("message", "User created successfully"));
    }

    // Get user by ID: CL_ADMIN can view any user, others can view only their own profile
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
    public ResponseEntity<User> getUserById(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        if (!customUserDetails.getUserType().equals(UserType.CL_ADMIN) && !customUserDetails.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        return userService.findById(id)
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }

    // Get user by email: Only CL_ADMIN can view users by email
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
    public ResponseEntity<User> getUserByEmail(@PathVariable String email, @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        if (!customUserDetails.getUserType().equals(UserType.CL_ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        return userService.findByEmail(email)
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }

    // Update user: CL_ADMIN can update any user, others can update only their own profile
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
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User user, @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        if (!customUserDetails.getUserType().equals(UserType.CL_ADMIN) && !customUserDetails.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You do not have permission to update this user.");
        }
        try {
            User updatedUser = userService.updateUser(id, user);
            return ResponseEntity.ok(updatedUser);
        } catch (EmailAlreadyUsedException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error updating user: " + e.getMessage());
        }
    }

    // Delete user: Only HARBINGER can delete a user
    @Operation(summary = "Delete user", description = "This endpoint allows you to delete a user by their ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully", content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        if (!customUserDetails.getUserType().equals(UserType.HARBINGER)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You do not have permission to delete this user.");
        }
        userService.deleteUserById(id);
        return ResponseEntity.status(200).body("User deleted successfully");
    }

    // Archive user: Only CL_ADMIN can archive users
    @Operation(summary = "Archive user", description = "This endpoint allows you to archive a user by their ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User archived successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PatchMapping("/archive/{id}")
    public ResponseEntity<?> archiveUser(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        if (!customUserDetails.getUserType().equals(UserType.CL_ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You do not have permission to archive this user.");
        }
        try {
            userService.updateUserStatus(id, UserStatus.ARCHIVED);
            return ResponseEntity.ok("User archived successfully");
        } catch (UserNotFoundException ex) {
            return ResponseEntity.status(404).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Internal server error");
        }
    }

    // Reactivate user: Only CL_ADMIN can reactivate users
    @Operation(summary = "Reactivate user", description = "This endpoint allows you to reactivate an archived user by their ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User reactivated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PatchMapping("/reactivate/{id}")
    public ResponseEntity<?> reactivateUser(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        if (!customUserDetails.getUserType().equals(UserType.CL_ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You do not have permission to reactivate this user.");
        }
        try {
            userService.updateUserStatus(id, UserStatus.ACTIVE);
            return ResponseEntity.ok("User reactivated successfully");
        } catch (UserNotFoundException ex) {
            return ResponseEntity.status(404).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Internal server error");
        }
    }
}
