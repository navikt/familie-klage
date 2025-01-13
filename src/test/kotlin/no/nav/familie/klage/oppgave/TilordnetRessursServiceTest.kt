import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.klage.behandling.dto.OppgaveDto
import no.nav.familie.klage.infrastruktur.exception.ApiFeil
import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.klage.oppgave.BehandleSakOppgave
import no.nav.familie.klage.oppgave.BehandleSakOppgaveRepository
import no.nav.familie.klage.oppgave.OppgaveClient
import no.nav.familie.klage.oppgave.TilordnetRessursService
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import java.util.*
import org.junit.jupiter.api.Assertions.assertEquals

internal class TilordnetRessursServiceTest {

    private val oppgaveClient = mockk<OppgaveClient>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val behandleSakOppgaveRepository = mockk<BehandleSakOppgaveRepository>()

    private val tilordnetRessursService = TilordnetRessursService(
        oppgaveClient = oppgaveClient,
        featureToggleService = featureToggleService,
        behandleSakOppgaveRepository = behandleSakOppgaveRepository
    )

    @Test
    internal fun `skal returnere oppgave tilknyttet behandling`() {
        val behandlingId = UUID.randomUUID()
        val oppgaveId = 12345L
        val behandleSakOppgave = BehandleSakOppgave(
            behandlingId = behandlingId,
            oppgaveId = oppgaveId
        )
        val oppgave = Oppgave(
            id = oppgaveId,
            tildeltEnhetsnr = "1234",
            beskrivelse = "Test beskrivelse",
            tilordnetRessurs = "Test ressurs",
            prioritet = OppgavePrioritet.NORM,
            fristFerdigstillelse = "2025-01-01",
            mappeId = 1L
        )

        every { behandleSakOppgaveRepository.findByBehandlingId(behandlingId) } returns behandleSakOppgave
        every { oppgaveClient.finnOppgaveMedId(oppgaveId) } returns oppgave

        val resultat = tilordnetRessursService.hentOppgave(behandlingId)

        val forventetOppgaveDto = OppgaveDto(
            oppgaveId = oppgave.id,
            tildeltEnhetsnr = oppgave.tildeltEnhetsnr,
            beskrivelse = oppgave.beskrivelse,
            tilordnetRessurs = oppgave.tilordnetRessurs ?: "",
            prioritet = oppgave.prioritet,
            fristFerdigstillelse = oppgave.fristFerdigstillelse ?: "",
            mappeId = oppgave.mappeId
        )

        assertEquals(forventetOppgaveDto, resultat)

        verify { behandleSakOppgaveRepository.findByBehandlingId(behandlingId) }
        verify { oppgaveClient.finnOppgaveMedId(oppgaveId) }
    }

    @Test
    internal fun `skal feile når behandlingen ikke har tilknyttet oppgave`() {
        val behandlingId = UUID.randomUUID()

        every { behandleSakOppgaveRepository.findByBehandlingId(behandlingId) } returns null

        val exception = assertThrows<ApiFeil> {
            tilordnetRessursService.hentOppgave(behandlingId)
        }

        assert(exception.message == "Finnes ikke oppgave for behandlingen")
        assert(exception.httpStatus == HttpStatus.BAD_REQUEST)

        verify { behandleSakOppgaveRepository.findByBehandlingId(behandlingId) }
    }
}