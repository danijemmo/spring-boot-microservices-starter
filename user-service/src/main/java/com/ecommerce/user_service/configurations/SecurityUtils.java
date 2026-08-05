package com.ecommerce.user_service.configurations;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class SecurityUtils {

    public UUID getCurrentUserId() {
        Jwt jwt = getJwt();
        return UUID.fromString(jwt.getSubject());
    }

    public String getCurrentUsername() {
        Jwt jwt = getJwt();
        return jwt.getClaimAsString("preferred_username");
    }

    public List<String> getCurrentUserRoles() {
        Jwt jwt = getJwt();
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        return realmAccess != null
                ? (List<String>) realmAccess.get("roles")
                : List.of();
    }

    public boolean hasRole(String role) {
        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        String roleWithoutPrefix = role.startsWith("ROLE_") ? role.substring(5) : role;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities() != null) {
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                if (authority.getAuthority().equalsIgnoreCase(roleWithPrefix) ||
                    authority.getAuthority().equalsIgnoreCase(roleWithoutPrefix)) {
                    return true;
                }
            }
        }

        List<String> rawRoles = getCurrentUserRoles();
        return rawRoles.stream().anyMatch(r ->
                r.equalsIgnoreCase(roleWithPrefix) || r.equalsIgnoreCase(roleWithoutPrefix)
        );
    }

    private Jwt getJwt() {
        return (Jwt) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }
}
