package dev.ctlabs.starter.auth.infrastructure.config;

import dev.ctlabs.starter.auth.autoconfigure.AuthProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Slf4j
@Configuration
@ConditionalOnClass(Flyway.class)
@ConditionalOnProperty(prefix = "ctlabs.auth.db", name = "migration-enabled", havingValue = "true", matchIfMissing = true)
public class AuthFlywayConfig {

    private final DataSource dataSource;

    public AuthFlywayConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void migrateAuthSchema() {
        log.info(">>> INICIANDO MIGRACIÓN DE AUTH STARTER (ctlabs/auth/migration) <<<");
        try {
            Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:ctlabs/auth/migration")
                    .table("ctlabs_auth_schema_history")
                    .baselineOnMigrate(true)
                    .baselineVersion("0")
                    .load()
                    .migrate();
            log.info(">>> MIGRACIÓN DE AUTH STARTER COMPLETADA EXITOSAMENTE <<<");
        } catch (Exception e) {
            log.error(">>> ERROR CRÍTICO EN MIGRACIÓN DE AUTH STARTER <<<", e);
            throw e;
        }
    }
}