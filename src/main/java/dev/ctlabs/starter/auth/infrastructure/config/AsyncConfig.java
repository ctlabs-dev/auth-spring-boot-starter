package dev.ctlabs.starter.auth.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration for asynchronous processing.
 * Enables Spring's @Async annotation.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
