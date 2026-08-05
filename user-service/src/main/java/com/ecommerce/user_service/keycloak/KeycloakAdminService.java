package com.ecommerce.user_service.keycloak;

import com.ecommerce.user_service.exceptions.KeycloakAuthException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

import com.ecommerce.user_service.exceptions.NotAuthorizedException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class KeycloakAdminService {

    private final Keycloak keycloak;
    private final String realm;
    private final String defaultRole;
    private final String tokenUrl;
    private final String clientId;
    private final String clientSecret;
    private final RestClient restClient = RestClient.create();

    public KeycloakAdminService(
            @Value("${keycloak.realm}") String realm,
            @Value("${keycloak.server-url}") String serverUrl,
            @Value("${keycloak.token-url}") String tokenUrl,
            @Value("${keycloak.client-id}") String clientId,
            @Value("${keycloak.client-secret}") String clientSecret,
            @Value("${keycloak.default-role:ROLE_USER}") String defaultRole) {

        this.realm = realm;
        this.tokenUrl = tokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.defaultRole = defaultRole;
        this.keycloak = KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .build();
    }

    public void verifyCredentials(String email, String password) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("username", email);
        body.add("password", password);

        try {
            ResponseEntity<String> response = restClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .toEntity(String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new NotAuthorizedException("Invalid current password");
            }
        } catch (Exception e) {
            throw new NotAuthorizedException("Invalid current password");
        }
    }

    public String createUser(String email, String password,
                             String firstName, String lastName) {
        // Build the user representation
        UserRepresentation user = new UserRepresentation();
        user.setEmail(email);
        user.setUsername(email); // using email as username
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);
        user.setEmailVerified(true); // set false if you want email verification

        // Set password credential
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        user.setCredentials(List.of(credential));

        // Create user in Keycloak
        Response response = keycloak.realm(realm).users().create(user);

        if (response.getStatus() != 201) {
            throw keycloakException(response, "Failed to create user in Keycloak");
        }

        log.info("User created in Keycloak: {}", response);

        // Extract the created user's Keycloak ID from the Location header
        String locationHeader = response.getHeaderString("Location");
        String keycloakUserId = locationHeader.substring(
                locationHeader.lastIndexOf("/") + 1);

        // Assign default role
        assignRole(keycloakUserId, defaultRole);

        return keycloakUserId;
    }

    public void assignRole(String keycloakUserId, String roleName) {
        try {
            RoleRepresentation role = keycloak.realm(realm)
                    .roles()
                    .get(roleName)
                    .toRepresentation();

            keycloak.realm(realm)
                    .users()
                    .get(keycloakUserId)
                    .roles()
                    .realmLevel()
                    .add(List.of(role));
        } catch (NotFoundException e) {
            throw new KeycloakAuthException(
                    Response.Status.BAD_GATEWAY.getStatusCode(),
                    "keycloak_admin_error",
                    "Realm role '" + roleName + "' does not exist in Keycloak realm '" + realm + "'"
            );
        } catch (ForbiddenException e) {
            throw new KeycloakAuthException(
                    Response.Status.BAD_GATEWAY.getStatusCode(),
                    "keycloak_admin_error",
                    "Admin client is not allowed to assign role '" + roleName + "' in Keycloak realm '" + realm + "'"
            );
        } catch (Exception e) {
            throw new KeycloakAuthException(
                    Response.Status.BAD_GATEWAY.getStatusCode(),
                    "keycloak_admin_error",
                    "Failed to assign user role in Keycloak: " + e.getMessage()
            );
        }
    }

    public void deleteUser(String keycloakUserId) {
        try {
            keycloak.realm(realm).users().get(keycloakUserId).remove();
        } catch (Exception e) {
            throw new KeycloakAuthException(
                    Response.Status.BAD_GATEWAY.getStatusCode(),
                    "keycloak_admin_error",
                    "Failed to delete user in Keycloak"
            );
        }
    }

    public void updateUser(String keycloakUserId, String firstName, String lastName) {
        try {
            UserRepresentation user = keycloak.realm(realm).users().get(keycloakUserId).toRepresentation();
            user.setFirstName(firstName);
            user.setLastName(lastName);

            keycloak.realm(realm).users().get(keycloakUserId).update(user);
        } catch (Exception e) {
            throw new KeycloakAuthException(
                    Response.Status.BAD_GATEWAY.getStatusCode(),
                    "keycloak_admin_error",
                    "Failed to update user in Keycloak"
            );
        }
    }

    public void resetPassword(String keycloakUserId, String password, boolean temporary) {
        try {
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(password);
            credential.setTemporary(temporary);

            keycloak.realm(realm).users().get(keycloakUserId).resetPassword(credential);
        } catch (Exception e) {
            throw new KeycloakAuthException(
                    Response.Status.BAD_GATEWAY.getStatusCode(),
                    "keycloak_admin_error",
                    "Failed to reset user password in Keycloak"
            );
        }
    }

    public void sendPasswordResetEmail(String email) {
        try {
            List<UserRepresentation> users = keycloak.realm(realm).users().searchByEmail(email, true);
            if (users.isEmpty()) {
                return;
            }

            log.info("user " + users.get(0).getFirstName() + " " + users.get(0).getLastName());

            keycloak.realm(realm)
                    .users()
                    .get(users.get(0).getId())
                    .executeActionsEmail(List.of("UPDATE_PASSWORD"));
        } catch (Exception e) {
            log.error("Failed to send password reset email", e);

            if (e instanceof WebApplicationException ex) {
                log.error("Status: {}", ex.getResponse().getStatus());

                String body = ex.getResponse().readEntity(String.class);
                log.error("Body: {}", body);
            }

            throw new KeycloakAuthException(
                    502,
                    "keycloak_admin_error",
                    "Failed to send password reset email"
            );
        }
    }

    public void setUserEnabled(String keycloakUserId, boolean enabled) {
        try {
            UserRepresentation user = keycloak.realm(realm).users().get(keycloakUserId).toRepresentation();
            user.setEnabled(enabled);

            keycloak.realm(realm).users().get(keycloakUserId).update(user);
        } catch (Exception e) {
            throw new KeycloakAuthException(
                    Response.Status.BAD_GATEWAY.getStatusCode(),
                    "keycloak_admin_error",
                    "Failed to update user status in Keycloak"
            );
        }
    }

    public void removeRole(String keycloakUserId, String roleName) {
        try {
            RoleRepresentation role = keycloak.realm(realm)
                    .roles()
                    .get(roleName)
                    .toRepresentation();

            keycloak.realm(realm)
                    .users()
                    .get(keycloakUserId)
                    .roles()
                    .realmLevel()
                    .remove(List.of(role));
        } catch (NotFoundException e) {
            throw new KeycloakAuthException(
                    Response.Status.BAD_GATEWAY.getStatusCode(),
                    "keycloak_admin_error",
                    "Realm role '" + roleName + "' does not exist in Keycloak realm '" + realm + "'"
            );
        } catch (Exception e) {
            throw new KeycloakAuthException(
                    Response.Status.BAD_GATEWAY.getStatusCode(),
                    "keycloak_admin_error",
                    "Failed to remove user role in Keycloak"
            );
        }
    }

    private KeycloakAuthException keycloakException(Response response, String fallbackMessage) {
        String message = fallbackMessage;

        if (response.hasEntity()) {
            String responseBody = response.readEntity(String.class);
            if (responseBody != null && !responseBody.isBlank()) {
                message = KeycloakErrorParser.parse(
                        responseBody,
                        "keycloak_admin_error",
                        fallbackMessage
                ).description();
            }
        }

        return new KeycloakAuthException(
                response.getStatus(),
                "keycloak_admin_error",
                message
        );
    }
}
