package com.ecommerce.user_service.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerificationTokenService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String EMAIL_VERIFY_PREFIX = "email_verify:";
    private static final String PASSWORD_RESET_PREFIX = "password_reset:";

    public String createEmailVerificationToken(UUID userId) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                EMAIL_VERIFY_PREFIX + token,
                userId.toString(),
                Duration.ofHours(24)
        );
        return token;
    }

    public UUID verifyEmailToken(String token) {
        String key = EMAIL_VERIFY_PREFIX + token;
        Object userId = redisTemplate.opsForValue().get(key);

        if (userId == null) {
            throw new IllegalArgumentException("Invalid or expired verification token");
        }

        redisTemplate.delete(key);
        return UUID.fromString((String) userId);
    }

    // --- Password reset ---

    public String createPasswordResetToken(UUID userId) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                PASSWORD_RESET_PREFIX + token,
                userId.toString(),
                Duration.ofMinutes(15)
        );
        return token;
    }

    public UUID validatePasswordResetToken(String token) {
        String key = PASSWORD_RESET_PREFIX + token;
        Object userId = redisTemplate.opsForValue().get(key);

        if (userId == null) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }

        redisTemplate.delete(key);
        return UUID.fromString((String) userId);
    }

}