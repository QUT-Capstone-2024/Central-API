package com.cl.centralapi.service;

import com.cl.centralapi.model.Collection;
import com.cl.centralapi.model.User;
import com.cl.centralapi.repository.CollectionRepository;
import com.cl.centralapi.repository.UserRepository;
import com.cl.centralapi.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CollectionRepository collectionRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));

        // Return CustomUserDetails which includes the userType
        return new CustomUserDetails(user);
    }

    // Check if the user is the owner of the collection
    public boolean isUserOwnerOfCollection(CustomUserDetails userDetails, Long collectionId) {
        Optional<Collection> collection = collectionRepository.findById(collectionId);
        return collection.map(col -> col.getId().equals(userDetails.getId())).orElse(false);
    }
}
