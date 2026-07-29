package com.example.usermanagement.dto;

import com.example.usermanagement.model.AppRole;
import java.time.Instant;
import java.util.Set;

public record UserSummaryResponse(
    Long id,
    String username,
    String email,
    String taxCode,
    String name,
    String surname,
    Set<AppRole> roles,
    Instant createdAt
) {
}
