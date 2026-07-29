package com.example.usermanagement.config;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtRoleExtractor {

    private final JwtConfig jwtConfig;

    public JwtRoleExtractor(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    public KeycloakRole extract(Authentication authentication) {
        // Prima priorità: realm roles (ADMIN, OPERATOR, USER)
        Set<String> realmRoles = extractRealmRolesFromAuthorities(authentication);
        if (realmRoles.contains(KeycloakRole.ADMIN.name())) return KeycloakRole.ADMIN;
        if (realmRoles.contains(KeycloakRole.OPERATOR.name())) return KeycloakRole.OPERATOR;
        if (realmRoles.contains(KeycloakRole.USER.name())) return KeycloakRole.USER;

        // Fallback: deriva il ruolo dai client roles (delete_user → ADMIN, create/update → OPERATOR, read → USER)
        Set<String> clientRoles = extractClientRolesFromJwt(authentication);
        if (clientRoles.contains("delete_user")) return KeycloakRole.ADMIN;
        if (clientRoles.contains("update_user") || clientRoles.contains("create_user")) return KeycloakRole.OPERATOR;
        if (clientRoles.contains("read_user")) return KeycloakRole.USER;

        throw new AccessDeniedException("Authenticated principal does not contain a supported Keycloak role");
    }

    private Set<String> extractRealmRolesFromAuthorities(Authentication authentication) {
        Set<String> roles = new LinkedHashSet<>();
        if (authentication == null || authentication.getAuthorities() == null) {
            return roles;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String value = authority.getAuthority();
            if (value != null && value.startsWith("ROLE_")) {
                roles.add(value.substring(5).toUpperCase(Locale.ROOT));
            }
        }
        return roles;
    }

    @SuppressWarnings("unchecked")
    private Set<String> extractClientRolesFromJwt(Authentication authentication) {
        Set<String> roles = new LinkedHashSet<>();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return roles;
        }
        Object resourceAccess = jwt.getClaim("resource_access");
        if (!(resourceAccess instanceof Map<?, ?> resourceAccessMap)) return roles;

        Object clientAccess = resourceAccessMap.get(jwtConfig.getClientId());
        if (!(clientAccess instanceof Map<?, ?> clientAccessMap)) return roles;

        Object clientRoles = clientAccessMap.get("roles");
        if (clientRoles instanceof Collection<?> clientRoleCollection) {
            clientRoleCollection.stream()
                .map(String::valueOf)
                .forEach(roles::add);
        }
        return roles;
    }
}
