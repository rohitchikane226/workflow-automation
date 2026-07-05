package com.springrest.controller;

import com.springrest.repository.UserRepository;
import com.springrest.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.HashMap;
import java.util.Map;
import com.springrest.Entities.*;
import com.springrest.exception.ApiResponse;
import com.springrest.exception.CustomException;

@RestController
@RequestMapping("/auth")
@CrossOrigin("*")
public class AuthController {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private JwtUtil jwtUtil;

    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<User>> register(@RequestBody User user) {

        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            throw new CustomException("Email is required", 400);
        }
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            throw new CustomException("Password is required", 400);
        }
        User existingUser = userRepo.findByEmail(user.getEmail());
        if (existingUser != null) {
            throw new CustomException("Email already registered", 400);
        }
        user.setPassword(encoder.encode(user.getPassword()));
        user.setRole("USER");

        User savedUser = userRepo.save(user);

        return ResponseEntity.ok(
                new ApiResponse<>(true, savedUser, "User registered successfully")
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(@RequestBody User user) {

        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            throw new CustomException("Email is required", 400);
        }

        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            throw new CustomException("Password is required", 400);
        }

        User dbUser = userRepo.findByEmail(user.getEmail());

        if (dbUser == null) {
            throw new CustomException("User not found", 404);
        }

        if (!encoder.matches(user.getPassword(), dbUser.getPassword())) {
            throw new CustomException("Invalid password", 401);
        }

        String token = jwtUtil.generateToken(dbUser.getEmail());

        Map<String, String> data = new HashMap<>();
        data.put("token", token);
        data.put("email",dbUser.getEmail());
        return ResponseEntity.ok(
                new ApiResponse<>(true, data, "Login successful")
        );
    }
}