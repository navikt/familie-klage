package no.nav.familie.klage.infrastruktur.config

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.klage.integrasjoner.FamilieEFSakClient
import no.nav.familie.kontrakter.felles.klage.FagsystemType
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.LocalDateTime
import java.time.Month

@Configuration
@Profile("mock-ef-sak")
class FamilieEFSakClientMock {

    @Bean
    @Primary
    fun hentVedtak(): FamilieEFSakClient {
        return resetMock(mockk())
    }

    companion object {

        fun resetMock(mock: FamilieEFSakClient): FamilieEFSakClient {
            clearMocks(mock)

            every { mock.hentVedtak(any()) } returns listOf(
                FagsystemVedtak(
                    "123",
                    "Førstegangsbehandling",
                    "Innvilget",
                    vedtakstidspunkt = LocalDateTime.of(2022, Month.AUGUST, 1, 8, 0),
                    fagsystemType = FagsystemType.ORDNIÆR
                ),
                FagsystemVedtak(
                    "124",
                    "Revurdering",
                    "Opphørt",
                    vedtakstidspunkt = LocalDateTime.of(2022, Month.OCTOBER, 1, 8, 0),
                    fagsystemType = FagsystemType.ORDNIÆR
                ),
                FagsystemVedtak(
                    "tilbake-123",
                    "Tilbakekreving",
                    "Full tilbakekreving",
                    vedtakstidspunkt = LocalDateTime.of(2022, Month.OCTOBER, 1, 8, 10, 2),
                    fagsystemType = FagsystemType.TILBAKEKREVING
                )
            )
            return mock
        }
    }
}
