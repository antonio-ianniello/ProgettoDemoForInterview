package com.example.usermanagement.service;

import com.example.usermanagement.config.KeycloakRole;
import com.example.usermanagement.dto.CreateUserRequest;
import com.example.usermanagement.dto.UpdateUserRequest;
import com.example.usermanagement.dto.UserResponse;
import com.example.usermanagement.dto.UserSummaryResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    Page<UserSummaryResponse> getUsers(Pageable pageable, KeycloakRole viewerRole);

    UserResponse getUser(UUID id, KeycloakRole viewerRole);

    UserResponse createUser(CreateUserRequest request, KeycloakRole viewerRole);

    UserResponse updateUser(UUID id, UpdateUserRequest request, KeycloakRole viewerRole);

    void deleteUser(UUID id);
}
