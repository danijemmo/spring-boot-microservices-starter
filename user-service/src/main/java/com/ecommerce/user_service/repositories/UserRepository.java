package com.ecommerce.user_service.repositories;

import com.ecommerce.user_service.entities.User;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Boolean existsByEmail(@NotNull String email);
    Optional<User> findByEmail(@NotNull String email);
}
