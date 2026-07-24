package com.example.usermanagement.controller;

import com.example.usermanagement.config.JwtRoleExtractor;
import com.example.usermanagement.dto.CreateUserRequest;
import com.example.usermanagement.dto.UpdateUserRequest;
import com.example.usermanagement.dto.UserResponse;
import com.example.usermanagement.dto.UserSummaryResponse;
import com.example.usermanagement.service.UserService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final JwtRoleExtractor jwtRoleExtractor;

    public UserController(UserService userService, JwtRoleExtractor jwtRoleExtractor) {
        this.userService = userService;
        this.jwtRoleExtractor = jwtRoleExtractor;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'USER')")
    public Page<UserSummaryResponse> getUsers(
        @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
        Authentication authentication
    ) {
        return userService.getUsers(pageable, jwtRoleExtractor.extract(authentication));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'USER')")
    public UserResponse getUser(@PathVariable UUID id, Authentication authentication) {
        return userService.getUser(id, jwtRoleExtractor.extract(authentication));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<UserResponse> createUser(
        @Valid @RequestBody CreateUserRequest request,
        Authentication authentication
    ) {
        UserResponse response = userService.createUser(request, jwtRoleExtractor.extract(authentication));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public UserResponse updateUser(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateUserRequest request,
        Authentication authentication
    ) {
        return userService.updateUser(id, request, jwtRoleExtractor.extract(authentication));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
