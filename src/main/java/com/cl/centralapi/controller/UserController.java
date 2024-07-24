package com.cl.centralapi.controller;

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
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user) {
        User savedUser = userService.saveUser(user);
        return ResponseEntity.ok(savedUser);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_INTERNAL') and principal.userType == T(com.cl.centralapi.enums.UserType).CL_ADMIN or #id == principal.id")
    public ResponseEntity<User> getUserById(@PathVariable Long id, @AuthenticationPrincipal UserDetails currentUser) {
        // Check that the user exists
        return userService.findById(id)
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasAuthority('ROLE_INTERNAL') and principal.userType == T(com.cl.centralapi.enums.UserType).CL_ADMIN")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        // Fetch the user by email
        return userService.findByEmail(email)
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }

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
        existingUser.setPropertyIds(user.getPropertyIds());

        User updatedUser = userService.saveUser(existingUser);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('INTERNAL') and principal.userType == T(com.cl.centralapi.enums.UserType).CL_ADMIN")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }
}
