package no.nav.familie.klage.kabal.event

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.klage.behandling.BehandlingRepository
import no.nav.familie.klage.fagsak.FagsakPersonRepository
import no.nav.familie.klage.fagsak.FagsakRepository
import no.nav.familie.klage.fagsak.domain.PersonIdent
import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.integrasjoner.OppgaveClient
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsakDomain
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull

class OpprettOppgaveTaskTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var fagsakRepository: FagsakRepository
    @Autowired lateinit var behandlingRepository: BehandlingRepository
    @Autowired lateinit var personRepository: FagsakPersonRepository

    private val oppgaveClient = mockk<OppgaveClient>()
    private val opprettOppgaveRequestSlot = slot<OpprettOppgaveRequest>()

    private lateinit var opprettOppgaveTask: OpprettOppgaveTask

    @BeforeEach
    fun setup() {
        opprettOppgaveTask = OpprettOppgaveTask(fagsakRepository, behandlingRepository, personRepository, oppgaveClient)
        every { oppgaveClient.opprettOppgave(capture(opprettOppgaveRequestSlot)) } answers { 9L }
    }

    @Test
    fun `skal lage oppgave med riktige verdier i request`() {
        val personIdent = "12345678901"
        val fagsak = testoppsettService.lagreFagsak(
            fagsakDomain().tilFagsakMedPerson(
                setOf(
                    PersonIdent(personIdent)
                )
            )
        )
        val behandling = behandling(fagsak = fagsak, eksternFagsystemBehandlingId = "2")
        behandlingRepository.insert(behandling)

        val fagsakDomain = fagsakRepository.findByIdOrNull(fagsak.id) ?: error("Finner ikke fagsak med id")

        val opprettOppgavePayload = OpprettOppgavePayload(behandling.eksternBehandlingId, "tekst", Fagsystem.EF)
        opprettOppgaveTask.doTask(OpprettOppgaveTask.opprettTask(opprettOppgavePayload))

        assertThat(opprettOppgaveRequestSlot.captured.tema).isEqualTo(Tema.ENF)
        assertThat(opprettOppgaveRequestSlot.captured.beskrivelse).contains("tekst")
        assertThat(opprettOppgaveRequestSlot.captured.ident).isEqualTo(OppgaveIdentV2(personIdent, IdentGruppe.FOLKEREGISTERIDENT))
        assertThat(opprettOppgaveRequestSlot.captured.saksId).isEqualTo(fagsakDomain.eksternId)
        assertThat(opprettOppgaveRequestSlot.captured.enhetsnummer).isEqualTo(behandling.behandlendeEnhet)
        assertThat(opprettOppgaveRequestSlot.captured.behandlingstema).isEqualTo(Behandlingstema.Overgangsstønad.value)
    }
}
