package no.nav.familie.klage.infrastruktur.db

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.postgresql.PostgreSQLContainer

class DbContainerInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        postgres.start()
        TestPropertyValues
            .of(
                "spring.datasource.url=${postgres.jdbcUrl}",
                "spring.datasource.username=${postgres.username}",
                "spring.datasource.password=${postgres.password}",
            ).applyTo(applicationContext.environment)
    }

    companion object {
        // Lazy because we only want it to be initialized when accessed
        private val postgres: PostgreSQLContainer by lazy {
            PostgreSQLContainer("postgres:17.9")
                .withDatabaseName("klage")
                .withUsername("postgres")
                .withPassword("test")
        }
    }
}
