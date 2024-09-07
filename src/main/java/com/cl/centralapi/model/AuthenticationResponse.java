package com.cl.centralapi.model;

import com.cl.centralapi.enums.UserRole;
import com.cl.centralapi.enums.UserType;

public class AuthenticationResponse {
    private String token;
    private String email;
    private String name;
    private String role;
    private String userType;
    private Long id;

    public AuthenticationResponse(String token, String email, String name, UserRole role, UserType userType, Long id) {
        this.token = token;
        this.email = email;
        this.name = name;
        this.role = String.valueOf(role);
        this.userType = String.valueOf(userType);
        this.id = id;
    }

    // Getters and Setters

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public Long getId() {
        return id;
    }
}
