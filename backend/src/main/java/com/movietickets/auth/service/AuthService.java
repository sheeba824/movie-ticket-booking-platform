package com.movietickets.auth.service;

import com.movietickets.auth.dto.LoginRequest;
import com.movietickets.auth.dto.LoginResponse;
import com.movietickets.auth.dto.RegisterRequest;
import com.movietickets.auth.entity.User;
import com.movietickets.auth.entity.Role;
import com.movietickets.auth.repository.UserRepository;
import com.movietickets.auth.repository.RoleRepository;
import com.movietickets.auth.security.JwtTokenProvider;
import com.movietickets.common.exception.CustomException;
import com.movietickets.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Authentication Service
 * Handles user registration, login, and JWT token management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    /**
     * Register a new user
     *
     * @param request User registration details
     * @return Registered user
     */
    @Transactional
    public User register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("User already exists with email: {}", request.getEmail());
            throw new CustomException(
                ErrorCode.USER_ALREADY_EXISTS,
                "User already exists with this email"
            );
        }

        // Create new user
        User user = User.builder()
            .id(UUID.randomUUID())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .phone(request.getPhone())
            .userType(request.getUserType())
            .status("ACTIVE")
            .kycStatus("PENDING")
            .build();

        // Assign default role based on user type
        Role customerRole = roleRepository.findByName("CUSTOMER")
            .orElseThrow(() -> new CustomException(
                ErrorCode.ROLE_NOT_FOUND,
                "Default role not found"
            ));

        user.setRoles(new HashSet<>(Arrays.asList(customerRole)));

        user = userRepository.save(user);
        log.info("User registered successfully with email: {}", request.getEmail());

        return user;
    }

    /**
     * Authenticate user and generate JWT tokens
     *
     * @param request Login credentials
     * @return Login response with tokens
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> {
                log.warn("Login failed: User not found with email: {}", request.getEmail());
                return new CustomException(
                    ErrorCode.INVALID_CREDENTIALS,
                    "Invalid email or password"
                );
            });

        // Check if user is active
        if (!user.getStatus().equals("ACTIVE")) {
            log.warn("Login failed: User account is not active. Email: {}", request.getEmail());
            throw new CustomException(
                ErrorCode.USER_INACTIVE,
                "User account is not active"
            );
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed: Invalid password for email: {}", request.getEmail());
            throw new CustomException(
                ErrorCode.INVALID_CREDENTIALS,
                "Invalid email or password"
            );
        }

        // Generate JWT tokens
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        log.info("User logged in successfully: {}", request.getEmail());

        return LoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(jwtTokenProvider.getAccessTokenExpiryInSeconds())
            .tokenType("Bearer")
            .user(user)
            .build();
    }

    /**
     * Validate JWT token
     *
     * @param token JWT token
     * @return True if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            jwtTokenProvider.validateToken(token);
            return true;
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Refresh access token
     *
     * @param refreshToken Refresh token
     * @return New access token
     */
    @Transactional
    public String refreshToken(String refreshToken) {
        log.info("Refreshing access token");

        // Validate refresh token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new CustomException(
                ErrorCode.INVALID_TOKEN,
                "Invalid or expired refresh token"
            );
        }

        // Extract user from refresh token
        String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow(() -> new CustomException(
                ErrorCode.USER_NOT_FOUND,
                "User not found"
            ));

        // Generate new access token
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        log.info("Access token refreshed successfully for user: {}", userId);

        return accessToken;
    }

    /**
     * Logout user (add token to blacklist)
     *
     * @param token JWT token
     */
    @Transactional
    public void logout(String token) {
        log.info("Logging out user with token");
        // Add token to blacklist (implementation depends on TokenBlacklistService)
        // jwtTokenProvider.revokeToken(token);
    }

    /**
     * Get user permissions
     *
     * @param userId User ID
     * @return Set of permissions
     */
    public Set<String> getUserPermissions(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(
                ErrorCode.USER_NOT_FOUND,
                "User not found"
            ));

        Set<String> permissions = new HashSet<>();
        user.getRoles().forEach(role -> 
            permissions.addAll(role.getPermissions())
        );

        return permissions;
    }
}
