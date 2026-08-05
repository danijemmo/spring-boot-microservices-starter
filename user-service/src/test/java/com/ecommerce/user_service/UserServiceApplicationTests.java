package com.ecommerce.user_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = {
				"grpc.server.port=-1",
				"eureka.client.enabled=false",
				"spring.cloud.discovery.enabled=false"
		}
)
class UserServiceApplicationTests {

	@Test
	void contextLoads() {
	}

	@TestConfiguration
	static class TestBeans {
		@Bean
		JwtDecoder jwtDecoder() {
			return token -> {
				throw new JwtException("Not used in context load test");
			};
		}
	}

}
