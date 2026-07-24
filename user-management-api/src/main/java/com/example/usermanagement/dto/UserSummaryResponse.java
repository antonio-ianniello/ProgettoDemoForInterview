package com.example.usermanagement.dto;

import com.example.usermanagement.model.AppRole;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserSummaryResponse(
    UUID id,
    String username,
    String email,
    String taxCode,
    String firstName,
    String lastName,
    Set<AppRole> roles,
    Instant createdAt
) {
}
