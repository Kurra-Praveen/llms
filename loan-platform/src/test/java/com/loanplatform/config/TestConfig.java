package com.loanplatform.config;

import com.loanplatform.common.config.TenantContext;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.UUID;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestConfig {

    public static final UUID TEST_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID TEST_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return mock(RedisConnectionFactory.class);
    }

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate() {
        return mock(RedisTemplate.class);
    }

    public static void setTestTenant() {
        TenantContext.setCurrentTenant(TEST_TENANT_ID);
    }

    public static void clearTenant() {
        TenantContext.clear();
    }
}
