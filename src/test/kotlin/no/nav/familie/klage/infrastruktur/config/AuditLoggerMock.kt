package no.nav.familie.klage.infrastruktur.config

import io.mockk.mockk
import no.nav.familie.klage.felles.domain.AuditLogger
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-auditlogger", "integrasjonstest")
class AuditLoggerMock {
    @Bean
    @Primary
    fun auditLogger(): AuditLogger = mockk(relaxed = true)
}
