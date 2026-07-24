package com.example.usermanagement.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.usermanagement.model.AppRole;
import com.example.usermanagement.model.User;
import com.example.usermanagement.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void getUserAsUserHidesTaxCodeAndRoles() throws Exception {
        User savedUser = userRepository.save(buildUser("reader@example.com"));

        mockMvc.perform(get("/users/{id}", savedUser.getId()).with(jwtWithRole("USER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(savedUser.getId().toString()))
            .andExpect(jsonPath("$.email").value("reader@example.com"))
            .andExpect(jsonPath("$.taxCode").doesNotExist())
            .andExpect(jsonPath("$.roles").doesNotExist());
    }

    @Test
    void getUserAsAdminShowsAllFields() throws Exception {
        User savedUser = userRepository.save(buildUser("admin-visible@example.com"));

        mockMvc.perform(get("/users/{id}", savedUser.getId()).with(jwtWithRole("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.taxCode").value("RSSMRA80A01H501U"))
            .andExpect(jsonPath("$.roles[0]").exists());
    }

    @Test
    void createUserAsOperatorReturnsCreatedAndLocationHeader() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
            "username", "new-user",
            "email", "new.user@example.com",
            "taxCode", "VRDLGI80A01H501U",
            "firstName", "Luigi",
            "lastName", "Verdi",
            "roles", List.of("DEVELOPER", "REPORTER")
        ));

        mockMvc.perform(post("/users")
                .with(jwtWithRole("OPERATOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", Matchers.containsString("/users/")))
            .andExpect(jsonPath("$.email").value("new.user@example.com"))
            .andExpect(jsonPath("$.taxCode").doesNotExist())
            .andExpect(jsonPath("$.roles[0]").exists());
    }

    @Test
    void deleteUserAsStandardUserIsForbidden() throws Exception {
        User savedUser = userRepository.save(buildUser("delete-check@example.com"));

        mockMvc.perform(delete("/users/{id}", savedUser.getId()).with(jwtWithRole("USER")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    private User buildUser(String email) {
        User user = new User();
        user.setUsername("mrossi");
        user.setEmail(email);
        user.setTaxCode("RSSMRA80A01H501U");
        user.setFirstName("Mario");
        user.setLastName("Rossi");
        user.setRoles(Set.of(AppRole.OWNER));
        user.setCreatedAt(Instant.parse("2024-01-01T10:15:30Z"));
        user.setUpdatedAt(Instant.parse("2024-01-01T10:15:30Z"));
        return user;
    }

    private JwtRequestPostProcessor jwtWithRole(String role) {
        return jwt()
            .jwt(token -> token.claim("realm_access", Map.of("roles", List.of(role))))
            .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
