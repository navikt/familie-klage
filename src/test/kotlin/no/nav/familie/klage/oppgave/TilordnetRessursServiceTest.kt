package no.nav.familie.klage.oppgave

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.klage.behandling.dto.OppgaveDto
import no.nav.familie.klage.infrastruktur.exception.ApiFeil
import no.nav.familie.klage.infrastruktur.exception.ManglerTilgang
import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet
import no.nav.familie.kontrakter.felles.saksbehandler.Saksbehandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import java.util.UUID

internal class TilordnetRessursServiceTest {
    private val oppgaveClient = mockk<OppgaveClient>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val behandleSakOppgaveRepository = mockk<BehandleSakOppgaveRepository>()

    private val tilordnetRessursService =
        TilordnetRessursService(
            oppgaveClient = oppgaveClient,
            featureToggleService = featureToggleService,
            behandleSakOppgaveRepository = behandleSakOppgaveRepository,
        )

    @Test
    internal fun `skal returnere oppgave tilknyttet behandling`() {
        val behandlingId = UUID.randomUUID()
        val oppgaveId = 12345L
        val behandleSakOppgave =
            BehandleSakOppgave(
                behandlingId = behandlingId,
                oppgaveId = oppgaveId,
            )
        val oppgave =
            Oppgave(
                id = oppgaveId,
                tildeltEnhetsnr = "1234",
                beskrivelse = "Test beskrivelse",
                tilordnetRessurs = "Test ressurs",
                prioritet = OppgavePrioritet.NORM,
                fristFerdigstillelse = "2025-01-01",
                mappeId = 1L,
            )

        every { behandleSakOppgaveRepository.findByBehandlingId(behandlingId) } returns behandleSakOppgave
        every { oppgaveClient.finnOppgaveMedId(oppgaveId) } returns oppgave

        val resultat = tilordnetRessursService.hentOppgave(behandlingId)

        val forventetOppgaveDto =
            OppgaveDto(
                oppgaveId = oppgave.id,
                tildeltEnhetsnr = oppgave.tildeltEnhetsnr,
                beskrivelse = oppgave.beskrivelse,
                tilordnetRessurs = oppgave.tilordnetRessurs ?: "",
                prioritet = oppgave.prioritet,
                fristFerdigstillelse = oppgave.fristFerdigstillelse ?: "",
                mappeId = oppgave.mappeId,
            )

        assertEquals(forventetOppgaveDto, resultat)

        verify { behandleSakOppgaveRepository.findByBehandlingId(behandlingId) }
        verify { oppgaveClient.finnOppgaveMedId(oppgaveId) }
    }

    @Test
    internal fun `skal feile når behandlingen ikke har tilknyttet oppgave`() {
        val behandlingId = UUID.randomUUID()

        every { behandleSakOppgaveRepository.findByBehandlingId(behandlingId) } returns null

        val exception =
            assertThrows<ApiFeil> {
                tilordnetRessursService.hentOppgave(behandlingId)
            }

        assert(exception.message == "Finnes ikke oppgave for behandlingen")
        assert(exception.httpStatus == HttpStatus.BAD_REQUEST)

        verify { behandleSakOppgaveRepository.findByBehandlingId(behandlingId) }
    }

    @Nested
    inner class HentAnsvarligSaksbehandlerForBehandlingsIdTest {
        @Test
        internal fun `Skal kaste ManglerTilgang exception hvis bruker ikke har tilgang til enhet som oppgaven tilhører`() {
            val behandlingId = UUID.randomUUID()

            val behandleSakOppgave =
                BehandleSakOppgave(
                    behandlingId = behandlingId,
                    oppgaveId = 1,
                )

            every { behandleSakOppgaveRepository.findByBehandlingId(behandlingId) } returns behandleSakOppgave
            every { oppgaveClient.finnOppgaveMedId(1) } throws HttpClientErrorException(HttpStatus.FORBIDDEN)

            val exception =
                assertThrows<ManglerTilgang> {
                    tilordnetRessursService.hentAnsvarligSaksbehandlerForBehandlingsId(behandlingId)
                }

            assertThat(exception.message).isEqualTo("Bruker mangler tilgang til etterspurt oppgave")
            assertThat(exception.frontendFeilmelding).isEqualTo("Behandlingen er koblet til en oppgave du ikke har tilgang til. Visning av ansvarlig saksbehandler er derfor ikke mulig")
        }

        @Test
        internal fun `Skal kaste exception videre hvis det ikke er FORBIDDEN`() {
            val behandlingId = UUID.randomUUID()

            val behandleSakOppgave =
                BehandleSakOppgave(
                    behandlingId = behandlingId,
                    oppgaveId = 1,
                )

            every { behandleSakOppgaveRepository.findByBehandlingId(behandlingId) } returns behandleSakOppgave
            every { oppgaveClient.finnOppgaveMedId(1) } throws HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR)

            val exception =
                assertThrows<HttpClientErrorException> {
                    tilordnetRessursService.hentAnsvarligSaksbehandlerForBehandlingsId(behandlingId)
                }

            assertThat(exception.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        }

        @Test
        internal fun `Skal hente ansvarlig saksbehandler fra oppgave`() {
            val oppgave =
                Oppgave(
                    id = 1,
                    tildeltEnhetsnr = "1234",
                    beskrivelse = "Test beskrivelse",
                    tilordnetRessurs = "Test ressurs",
                    prioritet = OppgavePrioritet.NORM,
                    fristFerdigstillelse = "2025-01-01",
                    mappeId = 1L,
                )

            val behandlingId = UUID.randomUUID()
            val behandleSakOppgave = BehandleSakOppgave(behandlingId = behandlingId, oppgaveId = 1)
            val saksbehandler =
                Saksbehandler(
                    azureId = UUID.randomUUID(),
                    navIdent = "testNavIdent",
                    fornavn = "testFornavn",
                    etternavn = "testEtternavn",
                    enhet = "testEnhet",
                    enhetsnavn = "testEnhetsnavn",
                )

            every { behandleSakOppgaveRepository.findByBehandlingId(behandlingId) } returns behandleSakOppgave
            every { oppgaveClient.finnOppgaveMedId(1) } returns oppgave
            every { featureToggleService.isEnabled(any()) } returns true
            every { oppgaveClient.hentSaksbehandlerInfo(oppgave.tilordnetRessurs!!) } returns saksbehandler

            val ansvarligSaksbehandler =
                tilordnetRessursService.hentAnsvarligSaksbehandlerForBehandlingsId(behandlingId)

            assertThat(ansvarligSaksbehandler.fornavn).isEqualTo("testFornavn")
            assertThat(ansvarligSaksbehandler.etternavn).isEqualTo("testEtternavn")
        }
    }
}
