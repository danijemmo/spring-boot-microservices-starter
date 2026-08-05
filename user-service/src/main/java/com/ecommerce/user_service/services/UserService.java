package com.ecommerce.user_service.services;

import com.ecommerce.user_service.dto.request.*;
import com.ecommerce.user_service.dto.response.UserDTO;
import com.ecommerce.user_service.entities.TokenPurpose;
import com.ecommerce.user_service.entities.User;
import com.ecommerce.user_service.exceptions.ConflictException;
import com.ecommerce.user_service.exceptions.NotFoundException;
import com.ecommerce.user_service.keycloak.KeycloakAdminService;
import com.ecommerce.user_service.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final TokenEmailService tokenEmailService;
    private final VerificationTokenService verificationTokenService;
    private final KeycloakAdminService keycloakAdminService;

    public User _getUser(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("user not found"));
    }

    public User _getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("user not found"));
    }

    @Cacheable(cacheNames = "users", key = "'all'")
    public List<UserDTO> getAllUsers(){
        return userRepository.findAll().stream().map(UserDTO::fromEntity).collect(Collectors.toList());
    }

    @CacheEvict(cacheNames = {"users", "user"}, allEntries = true)
    public UserDTO createUser(CreateUserRequestDTO reqDTO) {
        boolean existingUser = userRepository.existsByEmail(reqDTO.email());

        if (existingUser){
            throw new ConflictException("Email already exists");
        }

        String keycloakId = keycloakAdminService.createUser(
                reqDTO.email(),
                reqDTO.password(),
                reqDTO.firstName(),
                reqDTO.lastName()
        );

        User user = User.builder()
                .id(UUID.fromString(keycloakId))
                .firstName(reqDTO.firstName())
                .lastName(reqDTO.lastName())
                .email(reqDTO.email())
                .roles(new HashSet<>(Set.of("ROLE_USER")))
                .build();

        User savedUser = userRepository.save(user);

        return UserDTO.fromEntity(savedUser);
    }

    @Cacheable(cacheNames = "user", key = "#id")
    public UserDTO getUserById (UUID id){
        return UserDTO.fromEntity(_getUser(id));
    }

    @CacheEvict(cacheNames = {"users", "user"}, allEntries = true)
    public UserDTO updateUser(UUID id, UpdateUserRequest request) {
        User user = _getUser(id);

        String newFirstName = (request.firstName() != null && !request.firstName().isBlank())
                ? request.firstName() : user.getFirstName();
        String newLastName = (request.lastName() != null && !request.lastName().isBlank())
                ? request.lastName() : user.getLastName();

        keycloakAdminService.updateUser(
                user.getId().toString(),
                newFirstName,
                newLastName
        );

        user.setFirstName(newFirstName);
        user.setLastName(newLastName);

        return UserDTO.fromEntity(userRepository.save(user));
    }

    public void changePassword(UUID id, ChangePasswordRequest request) {
        User user = _getUser(id);
        keycloakAdminService.verifyCredentials(user.getEmail(), request.currentPassword());
        keycloakAdminService.resetPassword(id.toString(), request.newPassword(), false);
    }

    public void resetPassword(ForgotPasswordRequest reqDTO) {
        Optional<User> userOpt = userRepository.findByEmail(reqDTO.email());

        if (userOpt.isEmpty()) {
            return;
        }

        String token = verificationTokenService.createPasswordResetToken(userOpt.get().getId());
        tokenEmailService.sendTokenEmail(reqDTO.email(), TokenPurpose.PASSWORD_RESET, token);
    }

    public void confirmPasswordReset(ConfirmPasswordResetRequest request) {
        UUID userId = verificationTokenService.validatePasswordResetToken(request.token());
        keycloakAdminService.resetPassword(userId.toString(), request.password(), false);
    }

    public void sendVerificationEmail(String email) {
        User user = _getUserByEmail(email);
        String token = verificationTokenService.createEmailVerificationToken(user.getId());
        tokenEmailService.sendTokenEmail(email, TokenPurpose.EMAIL_VERIFICATION, token);
    }

    public void verifyUserAccount(String token){
        UUID userId = verificationTokenService.verifyEmailToken(token);
        keycloakAdminService.setUserEnabled(userId.toString(), true);
    }

    public void resetPassword(UUID id, String password, boolean temporary) {
        _getUser(id);
        keycloakAdminService.resetPassword(id.toString(), password, temporary);
    }

    public void setUserEnabled(UUID id, boolean enabled) {
        _getUser(id);
        keycloakAdminService.setUserEnabled(id.toString(), enabled);
    }

    @CacheEvict(cacheNames = {"users", "user"}, allEntries = true)
    public void assignRole(UUID id, String role) {
        User user = _getUser(id);
        String formattedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        keycloakAdminService.assignRole(id.toString(), formattedRole);
        user.getRoles().add(formattedRole);
        userRepository.save(user);
    }

    @CacheEvict(cacheNames = {"users", "user"}, allEntries = true)
    public void removeRole(UUID id, String role) {
        User user = _getUser(id);
        String formattedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        keycloakAdminService.removeRole(id.toString(), formattedRole);
        user.getRoles().remove(formattedRole);
        userRepository.save(user);
    }

    public void deleteUser(UUID id){
        User user = _getUser(id);
        keycloakAdminService.deleteUser(user.getId().toString());
        userRepository.delete(user);
    }
}
