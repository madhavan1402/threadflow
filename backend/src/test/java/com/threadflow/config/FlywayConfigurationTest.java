package com.threadflow.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FlywayConfigurationTest {

    @Autowired
    private Flyway flyway;

    @Test
    void initialMigrationIsRecorded() {
        MigrationInfo[] applied = flyway.info().applied();
        assertTrue(applied.length >= 2);
        assertTrue(java.util.Arrays.stream(applied)
                .anyMatch(migration -> "1".equals(migration.getVersion().getVersion())));
        assertTrue(java.util.Arrays.stream(applied)
                .anyMatch(migration -> "2".equals(migration.getVersion().getVersion())));
    }
}
