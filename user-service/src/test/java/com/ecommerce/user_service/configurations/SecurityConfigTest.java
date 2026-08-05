package com.ecommerce.user_service.configurations;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigTest {

    @Test
    void jwtAuthConverterMapsKeycloakRealmRolesWithoutDuplicatingRolePrefix() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claim("realm_access", Map.of("roles", List.of("ROLE_ADMIN", "USER")))
                .build();

        List<String> authorities = new SecurityConfig()
                .jwtAuthConverter()
                .convert(jwt)
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        assertTrue(authorities.contains("ROLE_ADMIN"));
        assertTrue(authorities.contains("ROLE_USER"));
    }
}
