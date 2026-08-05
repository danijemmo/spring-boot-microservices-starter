package com.ecommerce.user_service.repositories;

import com.ecommerce.user_service.entities.TokenPurpose;
import com.ecommerce.user_service.entities.UserActionToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserActionTokenRepository extends JpaRepository<UserActionToken, UUID> {
    long deleteByEmailAndPurposeAndUsedAtIsNull(String email, TokenPurpose purpose);

    Optional<UserActionToken> findByEmailAndPurposeAndTokenHashAndUsedAtIsNull(
            String email,
            TokenPurpose purpose,
            String tokenHash
    );
}
