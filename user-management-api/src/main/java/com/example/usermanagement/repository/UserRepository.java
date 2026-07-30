package com.example.usermanagement.repository;

import com.example.usermanagement.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmailIgnoreCase(String email);

    @Override
    @EntityGraph(attributePaths = "roles")
    Optional<User> findById(Long id);
}
