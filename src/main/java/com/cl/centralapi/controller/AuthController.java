package com.cl.centralapi.controller;

import com.cl.centralapi.model.AuthRequest;
import com.cl.centralapi.model.AuthenticationResponse;
import com.cl.centralapi.model.User;
import com.cl.centralapi.service.CustomUserDetailsService;
import com.cl.centralapi.service.UserService;
import com.cl.centralapi.security.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "User Authorisation", description = "User auth and login management via JWT")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Operation(summary = "Login a registered user", description = "This endpoint allows registered users to login to the system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User logged in successfully",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = User.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content) })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {
        logger.info("Attempting to authenticate user: {}", authRequest.getEmail());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword())
            );
            logger.info("Authentication successful for user: {}", authRequest.getEmail());
        } catch (AuthenticationException e) {
            logger.error("Authentication failed for user: {}", authRequest.getEmail(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
        }

        // Fetch UserDetails and User object
        final UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getEmail());
        User user = userService.findByEmail(authRequest.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Create a map of extra claims
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userType", user.getUserType().name());  // Add userType
        extraClaims.put("userRole", user.getUserRole().name());  // Add userRole
        extraClaims.put("userId", user.getUserRole().name());  // Add userId

        // Generate JWT with extra claims
        final String jwt = jwtUtil.generateToken(userDetails.getUsername(), extraClaims);

        // Create the authentication response object
        AuthenticationResponse response = new AuthenticationResponse(jwt, user.getEmail(), user.getName(), user.getUserRole(), user.getUserType(), user.getId());

        logger.info("Generated JWT token for user: {}", authRequest.getEmail());
        return ResponseEntity.ok(response);
    }

}
