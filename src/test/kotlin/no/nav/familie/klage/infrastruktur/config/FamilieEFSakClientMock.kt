package no.nav.familie.klage.infrastruktur.config

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.klage.integrasjoner.FamilieEFSakClient
import no.nav.familie.kontrakter.felles.Regelverk
import no.nav.familie.kontrakter.felles.klage.FagsystemType
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import no.nav.familie.kontrakter.felles.klage.IkkeOpprettet
import no.nav.familie.kontrakter.felles.klage.IkkeOpprettetÅrsak
import no.nav.familie.kontrakter.felles.klage.KanIkkeOppretteRevurderingÅrsak
import no.nav.familie.kontrakter.felles.klage.KanOppretteRevurderingResponse
import no.nav.familie.kontrakter.felles.klage.OpprettRevurderingResponse
import no.nav.familie.kontrakter.felles.klage.Opprettet
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.LocalDateTime
import java.time.Month
import java.util.UUID

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
                    fagsystemType = FagsystemType.ORDNIÆR,
                    regelverk = Regelverk.NASJONAL
                ),
                FagsystemVedtak(
                    "124",
                    "Revurdering",
                    "Opphørt",
                    vedtakstidspunkt = LocalDateTime.of(2022, Month.OCTOBER, 1, 8, 0),
                    fagsystemType = FagsystemType.ORDNIÆR,
                    regelverk = Regelverk.NASJONAL
                ),
                FagsystemVedtak(
                    "tilbake-123",
                    "Tilbakekreving",
                    "Full tilbakekreving",
                    vedtakstidspunkt = LocalDateTime.of(2022, Month.OCTOBER, 1, 8, 10, 2),
                    fagsystemType = FagsystemType.TILBAKEKREVING,
                    regelverk = Regelverk.NASJONAL
                ),
                FagsystemVedtak(
                    "sanksjon-123",
                    "Revurdering",
                    "Sanksjon 1 måned",
                    vedtakstidspunkt = LocalDateTime.of(2022, Month.OCTOBER, 1, 8, 15, 2),
                    fagsystemType = FagsystemType.SANKSJON_1_MND,
                    regelverk = Regelverk.NASJONAL
                )
            )
            // mocker annen hver
            var opprettet = true
            every { mock.opprettRevurdering(any()) } answers {
                opprettet = !opprettet
                if (opprettet) {
                    OpprettRevurderingResponse(Opprettet(eksternBehandlingId = UUID.randomUUID().toString()))
                } else {
                    OpprettRevurderingResponse(IkkeOpprettet(årsak = IkkeOpprettetÅrsak.ÅPEN_BEHANDLING))
                }
            }

            var kanOpprette = true
            every { mock.kanOppretteRevurdering(any()) } answers {
                kanOpprette = !kanOpprette
                if (kanOpprette) {
                    KanOppretteRevurderingResponse(true, null)
                } else {
                    KanOppretteRevurderingResponse(false, KanIkkeOppretteRevurderingÅrsak.ÅPEN_BEHANDLING)
                }
            }

            return mock
        }
    }
}
