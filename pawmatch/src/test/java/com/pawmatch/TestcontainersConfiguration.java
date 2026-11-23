package com.pawmatch;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Real PostgreSQL for integration tests. Import it with {@code @Import(TestcontainersConfiguration.class)}.
 * Requires a container runtime; tests using it must be tagged {@code @Tag("docker")} so they can be skipped
 * with {@code ./gradlew build -PskipDockerTests}.
 * <p>
 * <b>One container for the whole test run</b> (singleton pattern): every Spring test context that imports this
 * configuration shares it, instead of each cached context keeping its own Postgres alive. Tests therefore share one
 * database: roll back (the {@code @DataJpaTest} default) or delete only the rows they created.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));

    /** Started by the first context that needs it; starting an already running container is a no-op. */
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return POSTGRES;
    }
}
