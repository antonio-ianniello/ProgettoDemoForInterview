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

    public KeycloakRole extract(Authentication authentication) {
        Set<String> roles = new LinkedHashSet<>();
        roles.addAll(extractFromAuthorities(authentication));
        roles.addAll(extractFromJwt(authentication));

        if (roles.contains(KeycloakRole.ADMIN.name())) {
            return KeycloakRole.ADMIN;
        }
        if (roles.contains(KeycloakRole.OPERATOR.name())) {
            return KeycloakRole.OPERATOR;
        }
        if (roles.contains(KeycloakRole.USER.name())) {
            return KeycloakRole.USER;
        }

        throw new AccessDeniedException("Authenticated principal does not contain a supported Keycloak role");
    }

    private Set<String> extractFromAuthorities(Authentication authentication) {
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
    private Set<String> extractFromJwt(Authentication authentication) {
        Set<String> roles = new LinkedHashSet<>();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return roles;
        }

        Object realmAccess = jwt.getClaim("realm_access");
        if (!(realmAccess instanceof Map<?, ?> realmAccessMap)) {
            return roles;
        }

        Object realmRoles = realmAccessMap.get("roles");
        if (realmRoles instanceof Collection<?> roleCollection) {
            roleCollection.stream()
                .map(String::valueOf)
                .map(value -> value.toUpperCase(Locale.ROOT))
                .forEach(roles::add);
        }
        return roles;
    }
}
