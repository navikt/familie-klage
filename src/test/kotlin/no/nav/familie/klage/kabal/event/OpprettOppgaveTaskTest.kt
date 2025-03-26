package no.nav.familie.klage.kabal.event

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.klage.behandling.BehandlingRepository
import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.fagsak.FagsakPersonRepository
import no.nav.familie.klage.fagsak.FagsakRepository
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.fagsak.domain.PersonIdent
import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.oppgave.OppgaveClient
import no.nav.familie.klage.oppgave.OpprettKabalEventOppgaveTask
import no.nav.familie.klage.oppgave.OpprettOppgavePayload
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsakDomain
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.KlageinstansUtfall
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet
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

    private lateinit var opprettOppgaveTask: OpprettKabalEventOppgaveTask

    val personIdent = "12345678901"
    private lateinit var fagsak: Fagsak
    private lateinit var behandling: Behandling

    @BeforeEach
    fun setup() {
        opprettOppgaveTask = OpprettKabalEventOppgaveTask(fagsakRepository, behandlingRepository, personRepository, oppgaveClient)
        every { oppgaveClient.opprettOppgave(capture(opprettOppgaveRequestSlot)) } answers { 9L }

        fagsak = testoppsettService.lagreFagsak(
            fagsakDomain().tilFagsakMedPerson(
                setOf(
                    PersonIdent(personIdent),
                ),
            ),
        )
        behandling = behandling(fagsak)

        behandlingRepository.insert(behandling)
    }

    @Test
    fun `skal lage oppgave med riktige verdier i request`() {
        val fagsakDomain = fagsakRepository.findByIdOrNull(fagsak.id) ?: error("Finner ikke fagsak med id")

        val opprettOppgavePayload = OpprettOppgavePayload(behandling.eksternBehandlingId, "tekst", Fagsystem.EF, null, behandlingstema = Behandlingstema.Overgangsstønad)
        opprettOppgaveTask.doTask(OpprettKabalEventOppgaveTask.opprettTask(opprettOppgavePayload, fagsakDomain.eksternId, behandling.eksternBehandlingId.toString(), fagsakDomain.fagsystem))

        assertThat(opprettOppgaveRequestSlot.captured.tema).isEqualTo(Tema.ENF)
        assertThat(opprettOppgaveRequestSlot.captured.beskrivelse).contains("tekst")
        assertThat(opprettOppgaveRequestSlot.captured.ident).isEqualTo(OppgaveIdentV2(personIdent, IdentGruppe.FOLKEREGISTERIDENT))
        assertThat(opprettOppgaveRequestSlot.captured.saksId).isEqualTo(fagsakDomain.eksternId)
        assertThat(opprettOppgaveRequestSlot.captured.enhetsnummer).isEqualTo(behandling.behandlendeEnhet)
        assertThat(opprettOppgaveRequestSlot.captured.behandlingstema).isEqualTo(Behandlingstema.Overgangsstønad.value)
        assertThat(opprettOppgaveRequestSlot.captured.prioritet).isEqualTo(OppgavePrioritet.NORM)
    }

    @Test
    fun `skal gi høy prioritet til oppgaver med klageinnstansutfall lik opphevet`() {
        val opprettOppgavePayload = OpprettOppgavePayload(behandling.eksternBehandlingId, "tekst", Fagsystem.EF, KlageinstansUtfall.OPPHEVET)
        opprettOppgaveTask.doTask(OpprettKabalEventOppgaveTask.opprettTask(opprettOppgavePayload, fagsak.eksternId, behandling.eksternBehandlingId.toString(), fagsak.fagsystem))

        assertThat(opprettOppgaveRequestSlot.captured.prioritet).isEqualTo(OppgavePrioritet.HOY)
    }

    @Test
    fun `skal gi norm prioritet til oppgaver med klageinnstansutfall ikke lik opphevet`() {
        val opprettOppgavePayload = OpprettOppgavePayload(behandling.eksternBehandlingId, "tekst", Fagsystem.EF, KlageinstansUtfall.MEDHOLD)
        opprettOppgaveTask.doTask(OpprettKabalEventOppgaveTask.opprettTask(opprettOppgavePayload, fagsak.eksternId, behandling.eksternBehandlingId.toString(), fagsak.fagsystem))

        assertThat(opprettOppgaveRequestSlot.captured.prioritet).isEqualTo(OppgavePrioritet.NORM)
    }
}
