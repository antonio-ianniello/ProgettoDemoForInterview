package com.example.usermanagement.config.jwt;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import com.example.usermanagement.enums.KeycloakRole;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class JwtRoleExtractor {

    public KeycloakRole extract(Authentication authentication) {
        Set<String> realmRoles = extractPrefixedAuthorities(authentication, "ROLE_");
        if (realmRoles.contains(KeycloakRole.ADMIN.name())) return KeycloakRole.ADMIN;
        if (realmRoles.contains(KeycloakRole.OPERATOR.name())) return KeycloakRole.OPERATOR;
        if (realmRoles.contains(KeycloakRole.USER.name())) return KeycloakRole.USER;

        // Fallback: deriva il ruolo dai client roles già estratti da SecurityConfig
        Set<String> clientRoles = extractUnprefixedAuthorities(authentication);
        if (clientRoles.contains("delete_user")) return KeycloakRole.ADMIN;
        if (clientRoles.contains("update_user") || clientRoles.contains("create_user")) return KeycloakRole.OPERATOR;
        if (clientRoles.contains("read_user")) return KeycloakRole.USER;

        throw new AccessDeniedException("Authenticated principal does not contain a supported Keycloak role");
    }

    private Set<String> extractPrefixedAuthorities(Authentication authentication, String prefix) {
        Set<String> result = new LinkedHashSet<>();
        if (authentication == null) return result;
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String value = authority.getAuthority();
            if (value != null && value.startsWith(prefix)) {
                result.add(value.substring(prefix.length()).toUpperCase(Locale.ROOT));
            }
        }
        return result;
    }

    private Set<String> extractUnprefixedAuthorities(Authentication authentication) {
        Set<String> result = new LinkedHashSet<>();
        if (authentication == null) return result;
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String value = authority.getAuthority();
            if (value != null && !value.startsWith("ROLE_")) {
                result.add(value);
            }
        }
        return result;
    }
}
