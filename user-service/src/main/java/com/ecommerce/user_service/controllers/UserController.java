package com.ecommerce.user_service.controllers;

import com.ecommerce.user_service.configurations.SecurityUtils;
import com.ecommerce.user_service.dto.APIResponse.APIResponse;
import com.ecommerce.user_service.dto.request.ChangePasswordRequest;
import com.ecommerce.user_service.dto.request.UpdateUserRequest;
import com.ecommerce.user_service.dto.request.UserEnabledRequest;
import com.ecommerce.user_service.dto.request.UserRoleRequest;
import com.ecommerce.user_service.dto.response.UserDTO;
import com.ecommerce.user_service.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final SecurityUtils securityUtils;

    @PreAuthorize("@securityUtils.hasRole('ROLE_ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<APIResponse<List<UserDTO>>> getAll(){
        return APIResponse.build(
                201,
                "User fetched Successfully",
                userService.getAllUsers()
        );
    }

    @GetMapping("/me")
    public ResponseEntity<APIResponse<UserDTO>> getCurrentUser(){
        return APIResponse.build(
                200,
                "User fetched successfully",
                userService.getUserById(securityUtils.getCurrentUserId())
        );
    }

    @PatchMapping("/me")
    public ResponseEntity<APIResponse<UserDTO>> updateCurrentUser(@Valid @RequestBody UpdateUserRequest request){
        return APIResponse.build(
                200,
                "User updated successfully",
                userService.updateUser(securityUtils.getCurrentUserId(), request)
        );
    }

    @PostMapping("/me/password")
    public ResponseEntity<APIResponse<Object>> resetCurrentUserPassword(@Valid @RequestBody ChangePasswordRequest request){
        userService.changePassword(securityUtils.getCurrentUserId(), request);
        return APIResponse.build(
                200,
                "Password updated successfully",
                null
        );
    }

    @PreAuthorize("@securityUtils.hasRole('ROLE_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<UserDTO>> getUserById(@PathVariable String id){
        return APIResponse.build(
                200,
                "user fetched Successfully",
                userService.getUserById(UUID.fromString(id))
        );
    }

    @PreAuthorize("@securityUtils.hasRole('ROLE_ADMIN')")
    @PatchMapping("/{id}")
    public ResponseEntity<APIResponse<UserDTO>> updateUser(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserRequest request){
        return APIResponse.build(
                200,
                "User updated successfully",
                userService.updateUser(UUID.fromString(id), request)
        );
    }

    @PreAuthorize("@securityUtils.hasRole('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse<Object>> deleteUser(@PathVariable String id){
        userService.deleteUser(UUID.fromString(id));
        return APIResponse.build(
                200,
                "User deleted successfully",
                null
        );
    }

    @PreAuthorize("@securityUtils.hasRole('ROLE_ADMIN')")
    @PatchMapping("/{id}/enabled")
    public ResponseEntity<APIResponse<Object>> setUserEnabled(
            @PathVariable String id,
            @Valid @RequestBody UserEnabledRequest request){
        userService.setUserEnabled(UUID.fromString(id), request.enabled());
        return APIResponse.build(
                200,
                "User status updated successfully",
                null
        );
    }

    @PreAuthorize("@securityUtils.hasRole('ROLE_ADMIN')")
    @PostMapping("/{id}/roles")
    public ResponseEntity<APIResponse<Object>> assignRole(
            @PathVariable String id,
            @Valid @RequestBody UserRoleRequest request){
        userService.assignRole(UUID.fromString(id), request.role());
        return APIResponse.build(
                200,
                "Role assigned successfully",
                null
        );
    }

    @PreAuthorize("@securityUtils.hasRole('ROLE_ADMIN')")
    @DeleteMapping("/{id}/roles/{roleName}")
    public ResponseEntity<APIResponse<Object>> removeRole(
            @PathVariable String id,
            @PathVariable String roleName){
        userService.removeRole(UUID.fromString(id), roleName);
        return APIResponse.build(
                200,
                "Role removed successfully",
                null
        );
    }
}
