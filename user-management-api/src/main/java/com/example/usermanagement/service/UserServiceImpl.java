package com.example.usermanagement.service;

import com.example.usermanagement.config.KeycloakRole;
import com.example.usermanagement.dto.CreateUserRequest;
import com.example.usermanagement.dto.UpdateUserRequest;
import com.example.usermanagement.dto.UserResponse;
import com.example.usermanagement.dto.UserSummaryResponse;
import com.example.usermanagement.event.UserCreatedEvent;
import com.example.usermanagement.exception.DuplicateEmailException;
import com.example.usermanagement.exception.ResourceNotFoundException;
import com.example.usermanagement.mapper.UserMapper;
import com.example.usermanagement.model.User;
import com.example.usermanagement.repository.UserRepository;
import java.util.Locale;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    public UserServiceImpl(
        UserRepository userRepository,
        UserMapper userMapper,
        ApplicationEventPublisher applicationEventPublisher
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public Page<UserSummaryResponse> getUsers(Pageable pageable, KeycloakRole viewerRole) {
        return userRepository.findAll(pageable)
            .map(user -> userMapper.toSummaryResponse(user, viewerRole));
    }

    @Override
    public UserResponse getUser(UUID id, KeycloakRole viewerRole) {
        User user = findUserOrThrow(id);
        return userMapper.toResponse(user, viewerRole);
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request, KeycloakRole viewerRole) {
        String normalizedEmail = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateEmailException(normalizedEmail);
        }

        User user = userMapper.toEntity(request);
        User savedUser = userRepository.save(user);
        applicationEventPublisher.publishEvent(UserCreatedEvent.from(savedUser));
        return userMapper.toResponse(savedUser, viewerRole);
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request, KeycloakRole viewerRole) {
        User existingUser = findUserOrThrow(id);
        userMapper.updateUser(existingUser, request);
        User savedUser = userRepository.save(existingUser);
        return userMapper.toResponse(savedUser, viewerRole);
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        User user = findUserOrThrow(id);
        userRepository.delete(user);
    }

    private User findUserOrThrow(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
