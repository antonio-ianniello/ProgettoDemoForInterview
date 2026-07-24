package com.example.usermanagement.dto;

import com.example.usermanagement.model.AppRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record CreateUserRequest(
    @NotBlank @Size(max = 100) String username,
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Pattern(regexp = "^[A-Za-z0-9]{16}$", message = "taxCode must contain 16 alphanumeric characters") String taxCode,
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName,
    @NotEmpty Set<@NotNull AppRole> roles
) {
}
