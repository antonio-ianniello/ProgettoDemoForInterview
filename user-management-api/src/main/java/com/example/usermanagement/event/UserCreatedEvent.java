package com.example.usermanagement.event;

import com.example.usermanagement.model.AppRole;
import com.example.usermanagement.model.User;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

public record UserCreatedEvent(
    Long userId,
    String username,
    String email,
    Set<AppRole> roles,
    Instant createdAt
) {

    public static UserCreatedEvent from(User user) {
        Set<AppRole> appRoles = user.getRoles().stream()
            .map(role -> role.getName())
            .collect(Collectors.toSet());
        return new UserCreatedEvent(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            appRoles,
            user.getCreatedAt()
        );
    }
}
