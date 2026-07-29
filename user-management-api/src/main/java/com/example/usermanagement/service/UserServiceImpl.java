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
import com.example.usermanagement.model.Role;
import com.example.usermanagement.model.User;
import com.example.usermanagement.repository.RoleRepository;
import com.example.usermanagement.repository.UserRepository;
import java.util.Locale;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    public UserServiceImpl(
        UserRepository userRepository,
        RoleRepository roleRepository,
        UserMapper userMapper,
        ApplicationEventPublisher applicationEventPublisher
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public Page<UserSummaryResponse> getUsers(Pageable pageable, KeycloakRole viewerRole) {
        return userRepository.findAll(pageable)
            .map(user -> userMapper.toSummaryResponse(user, viewerRole));
    }

    @Override
    public UserResponse getUser(Long id, KeycloakRole viewerRole) {
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

        Set<Role> resolvedRoles = roleRepository.findByNameIn(request.roles());
        User user = userMapper.toEntity(request);
        user.setRoles(resolvedRoles);

        User savedUser = userRepository.save(user);
        applicationEventPublisher.publishEvent(UserCreatedEvent.from(savedUser));
        return userMapper.toResponse(savedUser, viewerRole);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request, KeycloakRole viewerRole) {
        User existingUser = findUserOrThrow(id);
        Set<Role> resolvedRoles = roleRepository.findByNameIn(request.roles());
        userMapper.updateUser(existingUser, request);
        existingUser.setRoles(resolvedRoles);

        User savedUser = userRepository.save(existingUser);
        return userMapper.toResponse(savedUser, viewerRole);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = findUserOrThrow(id);
        userRepository.delete(user);
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
