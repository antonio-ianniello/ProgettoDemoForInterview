package com.example.usermanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.usermanagement.config.KeycloakRole;
import com.example.usermanagement.dto.CreateUserRequest;
import com.example.usermanagement.dto.UpdateUserRequest;
import com.example.usermanagement.event.UserCreatedEvent;
import com.example.usermanagement.exception.DuplicateEmailException;
import com.example.usermanagement.mapper.UserMapper;
import com.example.usermanagement.model.AppRole;
import com.example.usermanagement.model.Role;
import com.example.usermanagement.model.User;
import com.example.usermanagement.repository.RoleRepository;
import com.example.usermanagement.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private UserService userService;

    @BeforeEach
    void setUp() {
        UserMapper userMapper = Mappers.getMapper(UserMapper.class);
        userService = new UserServiceImpl(userRepository, roleRepository, userMapper, applicationEventPublisher);
    }

    @Test
    void createUserPublishesEventAndReturnsFilteredResponseForOperator() {
        CreateUserRequest request = new CreateUserRequest(
            "mrossi",
            "Mario.Rossi@Example.com",
            "RSSMRA80A01H501U",
            "Mario",
            "Rossi",
            Set.of(AppRole.OWNER, AppRole.REPORTER)
        );

        Set<Role> resolvedRoles = Set.of(buildRole(1L, AppRole.OWNER), buildRole(2L, AppRole.REPORTER));
        User savedUser = buildUser();

        when(userRepository.existsByEmailIgnoreCase("mario.rossi@example.com")).thenReturn(false);
        when(roleRepository.findByNameIn(anySet())).thenReturn(resolvedRoles);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        var response = userService.createUser(request, KeycloakRole.OPERATOR);

        assertThat(response.email()).isEqualTo("mario.rossi@example.com");
        assertThat(response.taxCode()).isNull();
        assertThat(response.roles()).containsExactlyInAnyOrder(AppRole.OWNER, AppRole.REPORTER);

        ArgumentCaptor<UserCreatedEvent> eventCaptor = ArgumentCaptor.forClass(UserCreatedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().userId()).isEqualTo(savedUser.getId());
        assertThat(eventCaptor.getValue().email()).isEqualTo(savedUser.getEmail());
    }

    @Test
    void createUserThrowsDuplicateEmailWhenEmailAlreadyExists() {
        CreateUserRequest request = new CreateUserRequest(
            "mrossi",
            "mario.rossi@example.com",
            "RSSMRA80A01H501U",
            "Mario",
            "Rossi",
            Set.of(AppRole.OWNER)
        );

        when(userRepository.existsByEmailIgnoreCase("mario.rossi@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request, KeycloakRole.ADMIN))
            .isInstanceOf(DuplicateEmailException.class)
            .hasMessageContaining("mario.rossi@example.com");

        verify(userRepository, never()).save(any(User.class));
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void updateUserChangesMutableFieldsAndPreservesEmail() {
        Long userId = 1L;
        User existingUser = buildUser();
        existingUser.setId(userId);
        existingUser.setEmail("immutable@example.com");

        UpdateUserRequest request = new UpdateUserRequest(
            "updated-user",
            "VRDLGI80A01H501U",
            "Luigi",
            "Verdi",
            Set.of(AppRole.MAINTAINER)
        );

        Set<Role> resolvedRoles = Set.of(buildRole(3L, AppRole.MAINTAINER));
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(roleRepository.findByNameIn(anySet())).thenReturn(resolvedRoles);
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        var response = userService.updateUser(userId, request, KeycloakRole.ADMIN);

        assertThat(existingUser.getEmail()).isEqualTo("immutable@example.com");
        assertThat(existingUser.getUsername()).isEqualTo("updated-user");
        assertThat(existingUser.getTaxCode()).isEqualTo("VRDLGI80A01H501U");
        assertThat(existingUser.getRoles())
            .extracting(Role::getName)
            .containsExactly(AppRole.MAINTAINER);
        assertThat(response.email()).isEqualTo("immutable@example.com");
    }

    @Test
    void getUserHidesSensitiveFieldsForUserRole() {
        User existingUser = buildUser();
        when(userRepository.findById(existingUser.getId())).thenReturn(Optional.of(existingUser));

        var response = userService.getUser(existingUser.getId(), KeycloakRole.USER);

        assertThat(response.taxCode()).isNull();
        assertThat(response.roles()).isNull();
    }

    private User buildUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("mrossi");
        user.setEmail("mario.rossi@example.com");
        user.setTaxCode("RSSMRA80A01H501U");
        user.setName("Mario");
        user.setSurname("Rossi");
        user.setRoles(Set.of(buildRole(1L, AppRole.OWNER), buildRole(2L, AppRole.REPORTER)));
        user.setCreatedAt(Instant.parse("2024-01-01T10:15:30Z"));
        user.setUpdatedAt(Instant.parse("2024-01-01T10:15:30Z"));
        return user;
    }

    private Role buildRole(Long id, AppRole appRole) {
        Role role = new Role();
        role.setId(id);
        role.setName(appRole);
        return role;
    }
}
