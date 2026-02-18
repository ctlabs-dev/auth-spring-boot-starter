package dev.ctlabs.starter.auth.infrastructure.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Configuration for Flyway database migrations for the Auth starter.
 * Automatically migrates the database schema on startup if enabled.
 */
@Slf4j
@Configuration
@ConditionalOnClass(Flyway.class)
@ConditionalOnProperty(
        prefix = "ctlabs.auth.db",
        name = "migration-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AuthFlywayConfig {

    private final DataSource dataSource;

    public AuthFlywayConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void migrateAuthSchema() {
        log.info(">>> STARTING AUTH STARTER MIGRATION (ctlabs/auth/migration) <<<");
        try {
            Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:ctlabs/auth/migration")
                    .table("ctlabs_auth_schema_history")
                    .baselineOnMigrate(true)
                    .baselineVersion("0")
                    .load()
                    .migrate();
            log.info(">>> AUTH STARTER MIGRATION COMPLETED SUCCESSFULLY <<<");
        } catch (Exception e) {
            log.error(">>> CRITICAL ERROR IN AUTH STARTER MIGRATION <<<", e);
            throw e;
        }
    }
}
