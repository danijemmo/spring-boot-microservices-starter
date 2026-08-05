package com.ecommerce.user_service.dto.response;

import com.ecommerce.user_service.entities.User;

import java.util.Set;
import java.util.UUID;

public record UserDTO(
        UUID id,
        String firstName,
        String lastName,
        String email,
        Set<String> roles
) {
    public static UserDTO fromEntity(User entity){
        return new UserDTO(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getRoles()
        );
    }
}
