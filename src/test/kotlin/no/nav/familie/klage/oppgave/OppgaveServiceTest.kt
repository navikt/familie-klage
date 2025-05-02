package no.nav.familie.klage.oppgave

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.enhet.BarnetrygdEnhet
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.StatusEnum
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.cache.CacheManager
import java.util.UUID

internal class OppgaveServiceTest {
    val behandleSakOppgaveRepository = mockk<BehandleSakOppgaveRepository>()
    val oppgaveClient = mockk<OppgaveClient>()
    val behandlingService = mockk<BehandlingService>()
    val cacheManager = mockk<CacheManager>()
    val oppgaveService = OppgaveService(behandleSakOppgaveRepository, oppgaveClient, behandlingService, cacheManager)

    val behandlingId = UUID.randomUUID()
    val oppgaveId = 1L

    @Test
    internal fun `skal oppdatere oppgave med behandlingstemaet for klage-tilbakekreving`() {
        val oppgaveSlot = slot<Oppgave>()
        val eksisterendeOppgave =
            BehandleSakOppgave(
                behandlingId = behandlingId,
                oppgaveId = oppgaveId,
            )
        val behandling = behandling(id = behandlingId, status = BehandlingStatus.UTREDES)

        every { behandlingService.hentBehandling(behandlingId) } returns behandling
        every { behandleSakOppgaveRepository.findByBehandlingId(behandlingId) } returns eksisterendeOppgave
        every { oppgaveClient.oppdaterOppgave(capture(oppgaveSlot)) } returns oppgaveId

        oppgaveService.oppdaterOppgaveTilÅGjeldeTilbakekreving(behandlingId)

        assertThat(oppgaveSlot.captured.id).isEqualTo(oppgaveId)
        assertThat(oppgaveSlot.captured.behandlingstema).isEqualTo(Behandlingstema.Tilbakebetaling.value)

        // Sjekker at ingen andre felter blir satt
        assertThat(oppgaveSlot.captured.aktivDato).isNull()
        assertThat(oppgaveSlot.captured.behandlesAvApplikasjon).isNull()
        assertThat(oppgaveSlot.captured.behandlingstype).isNull()
        assertThat(oppgaveSlot.captured.beskrivelse).isNull()
        assertThat(oppgaveSlot.captured.bnr).isNull()
        assertThat(oppgaveSlot.captured.endretAv).isNull()
        assertThat(oppgaveSlot.captured.endretAvEnhetsnr).isNull()
        assertThat(oppgaveSlot.captured.endretTidspunkt).isNull()
        assertThat(oppgaveSlot.captured.ferdigstiltTidspunkt).isNull()
        assertThat(oppgaveSlot.captured.identer).isNull()
        assertThat(oppgaveSlot.captured.journalpostId).isNull()
        assertThat(oppgaveSlot.captured.journalpostkilde).isNull()
        assertThat(oppgaveSlot.captured.mappeId).isNull()
        assertThat(oppgaveSlot.captured.oppgavetype).isNull()
        assertThat(oppgaveSlot.captured.opprettetAv).isNull()
        assertThat(oppgaveSlot.captured.opprettetAvEnhetsnr).isNull()
        assertThat(oppgaveSlot.captured.opprettetTidspunkt).isNull()
        assertThat(oppgaveSlot.captured.orgnr).isNull()
        assertThat(oppgaveSlot.captured.prioritet).isNull()
        assertThat(oppgaveSlot.captured.saksreferanse).isNull()
        assertThat(oppgaveSlot.captured.samhandlernr).isNull()
        assertThat(oppgaveSlot.captured.status).isNull()
        assertThat(oppgaveSlot.captured.tema).isNull()
        assertThat(oppgaveSlot.captured.temagruppe).isNull()
        assertThat(oppgaveSlot.captured.tildeltEnhetsnr).isNull()
        assertThat(oppgaveSlot.captured.tilordnetRessurs).isNull()
        assertThat(oppgaveSlot.captured.versjon).isNull()
    }

    @Test
    internal fun `skal ikke oppdatere oppgave om behandling ikke har status opprettet eller utredes`() {
        val behandling = behandling(id = behandlingId, status = BehandlingStatus.FERDIGSTILT)

        every { behandlingService.hentBehandling(behandlingId) } returns behandling

        oppgaveService.oppdaterOppgaveTilÅGjeldeTilbakekreving(behandlingId)

        verify(exactly = 0) { behandleSakOppgaveRepository.findByBehandlingId(behandlingId) }
        verify(exactly = 0) { oppgaveClient.oppdaterOppgave(any()) }
    }

    @Nested
    inner class OppdaterEnhetPåBehandleSakOppgave {
        @ParameterizedTest
        @EnumSource(
            value = StatusEnum::class,
            names = ["FERDIGSTILT", "FEILREGISTRERT"],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `skal oppdatere BehandleSak-oppgave med ny enhet samt nullstille tilordnetRessurs og mappe`(status: StatusEnum) {
            // Arrange
            val behandling = behandling()
            val behandlendeEnhet = BarnetrygdEnhet.STORD
            val nyBehandlendeEnhet = BarnetrygdEnhet.STEINKJER
            val oppgaveId = 1L
            val behandleSakOppgave = BehandleSakOppgave(behandlingId = behandling.id, oppgaveId = oppgaveId)
            val oppgave =
                Oppgave(
                    id = oppgaveId,
                    tildeltEnhetsnr = behandlendeEnhet.enhetsnummer,
                    tilordnetRessurs = "Saksbehandler",
                    oppgavetype = "BEH_SAK",
                    mappeId = 101,
                    status = status,
                )

            val oppgaveSlot = slot<Oppgave>()

            every { behandleSakOppgaveRepository.findByBehandlingId(behandling.id) } returns behandleSakOppgave
            every { oppgaveClient.finnOppgaveMedId(oppgaveId) } returns oppgave
            every { oppgaveClient.oppdaterOppgave(capture(oppgaveSlot)) } returns oppgaveId

            // Act
            oppgaveService.oppdaterEnhetPåBehandleSakOppgave(behandlingId = behandling.id, nyBehandlendeEnhet)

            // Assert
            val oppdatertOppgave = oppgaveSlot.captured
            assertThat(oppdatertOppgave.tildeltEnhetsnr).isEqualTo(nyBehandlendeEnhet.enhetsnummer)
            assertThat(oppdatertOppgave.mappeId).isNull()
            assertThat(oppdatertOppgave.tilordnetRessurs).isNull()
        }

        @ParameterizedTest
        @EnumSource(
            value = StatusEnum::class,
            names = ["FERDIGSTILT", "FEILREGISTRERT"],
            mode = EnumSource.Mode.INCLUDE,
        )
        fun `skal ikke oppdatere oppgave om oppgaven allerede er ferdigstilt eller er feilregistrert`(status: StatusEnum) {
            // Arrange
            val behandling = behandling()
            val behandlendeEnhet = BarnetrygdEnhet.STORD
            val nyBehandlendeEnhet = BarnetrygdEnhet.STEINKJER
            val oppgaveId = 1L
            val behandleSakOppgave = BehandleSakOppgave(behandlingId = behandling.id, oppgaveId = oppgaveId)
            val oppgave =
                Oppgave(
                    id = oppgaveId,
                    tildeltEnhetsnr = behandlendeEnhet.enhetsnummer,
                    tilordnetRessurs = "Saksbehandler",
                    oppgavetype = "BEH_SAK",
                    mappeId = 101,
                    status = status,
                )

            every { behandleSakOppgaveRepository.findByBehandlingId(behandling.id) } returns behandleSakOppgave
            every { oppgaveClient.finnOppgaveMedId(oppgaveId) } returns oppgave

            // Act
            oppgaveService.oppdaterEnhetPåBehandleSakOppgave(behandlingId = behandling.id, nyBehandlendeEnhet)

            // Assert
            verify(exactly = 0) { oppgaveClient.oppdaterOppgave(any()) }
        }

        @Test
        fun `skal kaste feil dersom det ikke finnes en behandle sak oppgave for behandling`() {
            // Arrange
            val behandling = behandling()
            val nyBehandlendeEnhet = BarnetrygdEnhet.STEINKJER

            every { behandleSakOppgaveRepository.findByBehandlingId(behandling.id) } returns null

            // Act & Assert
            val feil = assertThrows<Feil> { oppgaveService.oppdaterEnhetPåBehandleSakOppgave(behandlingId = behandling.id, nyBehandlendeEnhet) }

            assertThat(feil.message).isEqualTo("Finner ingen BehandleSak-Oppgave tilknyttet behandling ${behandling.id}")
        }
    }
}
