package com.example.usermanagement.mapper;

import com.example.usermanagement.config.KeycloakRole;
import com.example.usermanagement.dto.CreateUserRequest;
import com.example.usermanagement.dto.UpdateUserRequest;
import com.example.usermanagement.dto.UserResponse;
import com.example.usermanagement.dto.UserSummaryResponse;
import com.example.usermanagement.model.AppRole;
import com.example.usermanagement.model.User;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
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
    @Mapping(target = "firstName", expression = "java(request.firstName().trim())")
    @Mapping(target = "lastName", expression = "java(request.lastName().trim())")
    @Mapping(target = "roles", expression = "java(normalizeRoles(request.roles()))")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(CreateUserRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "username", expression = "java(request.username().trim())")
    @Mapping(target = "taxCode", expression = "java(request.taxCode().trim().toUpperCase(java.util.Locale.ROOT))")
    @Mapping(target = "firstName", expression = "java(request.firstName().trim())")
    @Mapping(target = "lastName", expression = "java(request.lastName().trim())")
    @Mapping(target = "roles", expression = "java(normalizeRoles(request.roles()))")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateUser(@MappingTarget User user, UpdateUserRequest request);

    @Mapping(target = "taxCode", expression = "java(viewerRole.canSeeTaxCode() ? user.getTaxCode() : null)")
    @Mapping(target = "roles", expression = "java(viewerRole.canSeeRoles() ? copyRoles(user.getRoles()) : null)")
    UserResponse toResponse(User user, KeycloakRole viewerRole);

    @Mapping(target = "taxCode", expression = "java(viewerRole.canSeeTaxCode() ? user.getTaxCode() : null)")
    @Mapping(target = "roles", expression = "java(viewerRole.canSeeRoles() ? copyRoles(user.getRoles()) : null)")
    UserSummaryResponse toSummaryResponse(User user, KeycloakRole viewerRole);

    default Set<AppRole> normalizeRoles(Set<AppRole> roles) {
        if (roles == null || roles.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return EnumSet.copyOf(roles);
    }

    default Set<AppRole> copyRoles(Set<AppRole> roles) {
        return roles == null ? null : new LinkedHashSet<>(roles);
    }
}
