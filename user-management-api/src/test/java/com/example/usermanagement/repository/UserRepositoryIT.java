package com.example.usermanagement.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.usermanagement.model.AppRole;
import com.example.usermanagement.model.Role;
import com.example.usermanagement.model.User;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class UserRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("user_management")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", postgreSQLContainer::getDriverClassName);
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void savesUserAndLoadsRolesFromPostgres() {
        Set<Role> roles = roleRepository.findByNameIn(Set.of(AppRole.MAINTAINER, AppRole.REPORTER));

        User user = new User();
        user.setUsername("postgres-user");
        user.setEmail("postgres.user@example.com");
        user.setTaxCode("RSSMRA80A01H501U");
        user.setName("Postgres");
        user.setSurname("Tester");
        user.setRoles(roles);

        User saved = userRepository.saveAndFlush(user);
        Optional<User> loaded = userRepository.findById(saved.getId());

        assertThat(loaded).isPresent();
        Set<AppRole> loadedRoles = loaded.orElseThrow().getRoles().stream()
            .map(Role::getName)
            .collect(java.util.stream.Collectors.toSet());
        assertThat(loadedRoles).containsExactlyInAnyOrder(AppRole.MAINTAINER, AppRole.REPORTER);
    }

    @Test
    void duplicateEmailViolatesUniqueConstraint() {
        Set<Role> roles = roleRepository.findByNameIn(Set.of(AppRole.OWNER));

        User first = new User();
        first.setUsername("first-user");
        first.setEmail("duplicate@example.com");
        first.setTaxCode("RSSMRA80A01H501U");
        first.setName("First");
        first.setSurname("User");
        first.setRoles(roles);
        userRepository.saveAndFlush(first);

        User second = new User();
        second.setUsername("second-user");
        second.setEmail("duplicate@example.com");
        second.setTaxCode("VRDLGI80A01H501U");
        second.setName("Second");
        second.setSurname("User");
        second.setRoles(roles);

        assertThatThrownBy(() -> userRepository.saveAndFlush(second))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
