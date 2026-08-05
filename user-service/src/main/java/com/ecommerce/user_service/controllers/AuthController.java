package com.ecommerce.user_service.controllers;

import com.ecommerce.user_service.dto.APIResponse.APIResponse;
import com.ecommerce.user_service.dto.request.CreateUserRequestDTO;
import com.ecommerce.user_service.dto.request.ConfirmPasswordResetRequest;
import com.ecommerce.user_service.dto.request.ForgotPasswordRequest;
import com.ecommerce.user_service.dto.request.LoginRequest;
import com.ecommerce.user_service.dto.request.LogoutRequest;
import com.ecommerce.user_service.dto.request.RefreshRequest;
import com.ecommerce.user_service.dto.response.LoginResponse;
import com.ecommerce.user_service.dto.response.UserDTO;
import com.ecommerce.user_service.exceptions.ConflictException;
import com.ecommerce.user_service.exceptions.KeycloakAuthException;
import com.ecommerce.user_service.keycloak.KeycloakErrorParser;
import com.ecommerce.user_service.services.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {

    @Value("${keycloak.token-url}")
    private String tokenUrl;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    private final UserService userService;
    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/register")
    public ResponseEntity<APIResponse<UserDTO>> register(@Valid @RequestBody CreateUserRequestDTO request)
            throws ConflictException {
        return APIResponse.build(
                201,
                "User registration successful",
                userService.createUser(request)
        );
    }

    @PostMapping("/verify-email")
    public ResponseEntity<APIResponse<Object>> verifyEmail(@RequestParam String token){
        userService.verifyUserAccount(token);
        return APIResponse.build(
                200,
                "User account verified successfully",
                null
        );
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<APIResponse<Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        userService.resetPassword(request);
        return APIResponse.build(
                200,
                "If an account with this email exists, a password reset link has been sent.",
                null
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<APIResponse<Object>> resetPassword(@Valid @RequestBody ConfirmPasswordResetRequest request) {
        userService.confirmPasswordReset(request);
        return APIResponse.build(
                200,
                "Password updated successfully",
                null
        );
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("username", request.email());
        body.add("password", request.password());

        return restClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange((clientRequest, clientResponse) -> {
                    String responseBody = new String(
                            clientResponse.getBody().readAllBytes(),
                            StandardCharsets.UTF_8
                    );

                    JsonNode root = objectMapper.readTree(responseBody);

                    log.info("Keycloak response - Status: {}, Body: {}", clientResponse.getStatusCode(), responseBody);

                    HttpStatusCode status = clientResponse.getStatusCode();
                    String errorDescription = root.path("error_description").asText();

                    boolean emailNotVerified =
                            status.value() == 400 &&
                                    "Account is not fully set up".equals(errorDescription);

                    if (emailNotVerified) {
                        userService.sendVerificationEmail(request.email());

                        throw buildKeycloakAuthException(
                                status.value(),
                                responseBody,
                                "Your email address has not been verified yet.We've sent a verification email to your inbox. Please verify your account before logging in."
                        );
                    }

                    if (status.isError()) {
                        throw buildKeycloakAuthException(
                                status.value(),
                                responseBody,
                                "Invalid email or password"
                        );
                    }

                    return ResponseEntity.status(clientResponse.getStatusCode())
                            .body(new LoginResponse(
                                    root.path("access_token").asText(),
                                    root.path("refresh_token").asText(),
                                    root.path("expires_in").asInt()
                            ));
                });
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody RefreshRequest request) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", request.refreshToken());

        return restClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange((clientRequest, clientResponse) -> {
                    String responseBody = new String(
                            clientResponse.getBody().readAllBytes(),
                            StandardCharsets.UTF_8
                    );

                    if (clientResponse.getStatusCode().isError()) {
                        throw buildKeycloakAuthException(
                                clientResponse.getStatusCode().value(),
                                responseBody,
                                "Invalid refresh token"
                        );
                    }

                    JsonNode root = objectMapper.readTree(responseBody);

                    return ResponseEntity.ok(new LoginResponse(
                            root.path("access_token").asText(),
                            root.path("refresh_token").asText(),
                            root.path("expires_in").asInt()
                    ));
                });
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody LogoutRequest request) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", request.refreshToken());

        return restClient.post()
                .uri(tokenUrl.replace("/token", "/logout"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange((clientRequest, clientResponse) -> {
                    String responseBody = new String(
                            clientResponse.getBody().readAllBytes(),
                            StandardCharsets.UTF_8
                    );

                    log.info("Keycloak logout response - Status: {}, Body: {}", clientResponse.getStatusCode(), responseBody);

                    if (clientResponse.getStatusCode().isError()) {
                        throw buildKeycloakAuthException(
                                clientResponse.getStatusCode().value(),
                                responseBody,
                                "Invalid refresh token"
                        );
                    }

                    return ResponseEntity.noContent().build();
                });
    }

    private KeycloakAuthException buildKeycloakAuthException(
            int statusCode,
            String responseBody,
            String clientMessage) {
        KeycloakErrorParser.ParsedKeycloakError keycloakError = KeycloakErrorParser.parse(
                responseBody,
                "keycloak_auth_error",
                "Authentication failed"
        );

        return new KeycloakAuthException(
                statusCode,
                keycloakError.error(),
                keycloakError.description(),
                clientMessage
        );
    }
}
