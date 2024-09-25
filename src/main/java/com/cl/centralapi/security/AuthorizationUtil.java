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
}
