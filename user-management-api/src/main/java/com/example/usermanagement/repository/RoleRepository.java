package com.example.usermanagement.repository;

import com.example.usermanagement.model.AppRole;
import com.example.usermanagement.model.Role;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Set<Role> findByNameIn(Set<AppRole> names);
}
