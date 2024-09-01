package com.cl.centralapi.service;

import com.cl.centralapi.exceptions.EmailAlreadyUsedException;
import com.cl.centralapi.model.User;
import com.cl.centralapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User saveUser(User user) {
        // Check that the email is unique
        Optional<User> existingUser = userRepository.findByEmail(user.getEmail());
        if (existingUser.isPresent()) {
            throw new EmailAlreadyUsedException("Email already in use");
        }
        // Encode the user's password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public User updateUser(Long id, User user) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check if the email is being changed to one that already exists
        if (!existingUser.getEmail().equals(user.getEmail())) {
            Optional<User> userWithEmail = userRepository.findByEmail(user.getEmail());
            if (userWithEmail.isPresent()) {
                throw new RuntimeException("Email already in use");
            }
        }

        existingUser.setName(user.getName());
        existingUser.setEmail(user.getEmail());
        if (!user.getPassword().equals(existingUser.getPassword())) {
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        existingUser.setPhoneNumber(user.getPhoneNumber());
        existingUser.setUserType(user.getUserType());
        existingUser.setUserRole(user.getUserRole());

        return userRepository.save(existingUser);
    }


    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }


    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public void deleteUserById(Long id) {
        userRepository.deleteById(id);
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

}
