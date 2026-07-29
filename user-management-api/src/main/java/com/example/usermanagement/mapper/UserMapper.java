package com.example.usermanagement.mapper;

import com.example.usermanagement.enums.KeycloakRole;
import com.example.usermanagement.dto.CreateUserRequest;
import com.example.usermanagement.dto.UpdateUserRequest;
import com.example.usermanagement.dto.UserResponse;
import com.example.usermanagement.dto.UserSummaryResponse;
import com.example.usermanagement.model.AppRole;
import com.example.usermanagement.model.Role;
import com.example.usermanagement.model.User;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", expression = "java(request.email().trim().toLowerCase(java.util.Locale.ROOT))")
    @Mapping(target = "username", expression = "java(request.username().trim())")
    @Mapping(target = "taxCode", expression = "java(request.taxCode().trim().toUpperCase(java.util.Locale.ROOT))")
    @Mapping(target = "name", expression = "java(request.name().trim())")
    @Mapping(target = "surname", expression = "java(request.surname().trim())")
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(CreateUserRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "username", expression = "java(request.username().trim())")
    @Mapping(target = "taxCode", expression = "java(request.taxCode().trim().toUpperCase(java.util.Locale.ROOT))")
    @Mapping(target = "name", expression = "java(request.name().trim())")
    @Mapping(target = "surname", expression = "java(request.surname().trim())")
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateUser(@MappingTarget User user, UpdateUserRequest request);

    @Mapping(target = "taxCode", expression = "java(viewerRole.canSeeTaxCode() ? user.getTaxCode() : null)")
    @Mapping(target = "roles", expression = "java(viewerRole.canSeeRoles() ? toAppRoles(user.getRoles()) : null)")
    UserResponse toResponse(User user, KeycloakRole viewerRole);

    @Mapping(target = "taxCode", expression = "java(viewerRole.canSeeTaxCode() ? user.getTaxCode() : null)")
    @Mapping(target = "roles", expression = "java(viewerRole.canSeeRoles() ? toAppRoles(user.getRoles()) : null)")
    UserSummaryResponse toSummaryResponse(User user, KeycloakRole viewerRole);

    default Set<AppRole> toAppRoles(Set<Role> roles) {
        if (roles == null) {
            return null;
        }
        return roles.stream()
            .map(Role::getName)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
