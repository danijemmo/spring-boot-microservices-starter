package com.ecommerce.api_gateway.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class RateLimiterService {

    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> generalBuckets = new ConcurrentHashMap<>();

    @Value("${app.rate-limiting.auth-capacity:5}")
    private long authCapacity;

    @Value("${app.rate-limiting.general-capacity:60}")
    private long generalCapacity;

    public ConsumptionProbe tryConsumeAuth(String key) {
        Bucket bucket = authBuckets.computeIfAbsent(key, k -> createBucket(authCapacity, Duration.ofMinutes(1)));
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    public ConsumptionProbe tryConsumeGeneral(String key) {
        Bucket bucket = generalBuckets.computeIfAbsent(key, k -> createBucket(generalCapacity, Duration.ofMinutes(1)));
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    public long getAuthCapacity() {
        return authCapacity;
    }

    public long getGeneralCapacity() {
        return generalCapacity;
    }

    private Bucket createBucket(long capacity, Duration refillPeriod) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillIntervally(capacity, refillPeriod)
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
