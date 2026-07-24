package com.example.usermanagement.event;

import com.example.usermanagement.model.AppRole;
import com.example.usermanagement.model.User;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public record UserCreatedEvent(
    UUID userId,
    String username,
    String email,
    Set<AppRole> roles,
    Instant createdAt
) {

    public static UserCreatedEvent from(User user) {
        return new UserCreatedEvent(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            new LinkedHashSet<>(user.getRoles()),
            user.getCreatedAt()
        );
    }
}
