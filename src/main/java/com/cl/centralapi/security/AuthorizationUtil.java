package com.cl.centralapi.security;

import com.cl.centralapi.service.CollectionService;
import com.cl.centralapi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationUtil {

    @Autowired
    private UserService userService;

    @Autowired
    private CollectionService collectionService;

    // Check if the user is an admin or Harbinger
    public boolean isAdminOrHarbinger(Long userId) {
        return userService.isAdminOrHarbinger(userId);
    }

    // Check if the user owns the collection
    public boolean isCollectionOwner(Long userId, Long collectionId) {
        return collectionService.isCollectionOwnedByUser(userId, collectionId);
    }
}
