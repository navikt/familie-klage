package no.nav.familie.klage.kabal

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.klage.behandling.BehandlingRepository
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.StegService
import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.fagsak.domain.PersonIdent
import no.nav.familie.klage.infrastruktur.config.DatabaseConfiguration
import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.klage.kabal.domain.KlageinstansResultat
import no.nav.familie.klage.oppgave.OpprettKabalEventOppgaveTask
import no.nav.familie.klage.oppgave.OpprettOppgavePayload
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.kontrakter.felles.klage.BehandlingEventType
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Behandlingstype
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.UUID

class BehandlingFeilregistrertTaskTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var behandlingRepository: BehandlingRepository

    @Autowired lateinit var stegService: StegService

    @Autowired lateinit var taskService: TaskService

    @Autowired lateinit var behandlingService: BehandlingService

    @Autowired lateinit var fagsakService: FagsakService

    @Autowired lateinit var klageresultatRepository: KlageresultatRepository

    private lateinit var behandlingFeilregistrertTask: BehandlingFeilregistrertTask

    val personIdent = "12345678901"
    private lateinit var fagsak: Fagsak
    private lateinit var behandling: Behandling
    private val featuretoggleService = mockk<FeatureToggleService>()

    @BeforeEach
    fun setup() {
        every { featuretoggleService.isEnabled(any()) } returns true

        behandlingFeilregistrertTask =
            BehandlingFeilregistrertTask(stegService, taskService, behandlingService, fagsakService)

        fagsak = testoppsettService.lagreFagsak(
            DomainUtil.fagsakDomain().tilFagsakMedPerson(
                setOf(
                    PersonIdent(personIdent),
                ),
            ),
        )
        behandling = DomainUtil.behandling(
            fagsak = fagsak,
            resultat = BehandlingResultat.IKKE_MEDHOLD,
            status = BehandlingStatus.VENTER,
            steg = StegType.KABAL_VENTER_SVAR,
        )

        behandlingRepository.insert(behandling)

        klageresultatRepository.insert(
            KlageinstansResultat(
                eventId = UUID.randomUUID(),
                type = BehandlingEventType.BEHANDLING_FEILREGISTRERT,
                utfall = null,
                mottattEllerAvsluttetTidspunkt = LocalDateTime.of(2023, 6, 22, 1, 1),
                kildereferanse = behandling.eksternBehandlingId,
                journalpostReferanser = DatabaseConfiguration.StringListWrapper(verdier = listOf()),
                behandlingId = behandling.id,
                årsakFeilregistrert = "fordi det var feil",
            ),
        )
    }

    @Test
    internal fun `task skal opprette OpprettOppgaveTask og ferdigstille behandling`() {
        assertThat(behandling.steg).isEqualTo(StegType.KABAL_VENTER_SVAR)
        assertThat(behandling.status).isEqualTo(BehandlingStatus.VENTER)

        behandlingFeilregistrertTask.doTask(BehandlingFeilregistrertTask.opprettTask(behandling.id))

        val oppdatertBehandling = behandlingService.hentBehandling(behandling.id)
        assertThat(oppdatertBehandling.steg).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
        assertThat(oppdatertBehandling.status).isEqualTo(BehandlingStatus.FERDIGSTILT)

        val opprettOppgaveTask = taskService.findAll().single { it.type == OpprettKabalEventOppgaveTask.TYPE }
        val opprettOppgavePayload = objectMapper.readValue<OpprettOppgavePayload>(opprettOppgaveTask.payload)
        assertThat(opprettOppgavePayload.oppgaveTekst).isEqualTo("Klagebehandlingen er sendt tilbake fra KA med status feilregistrert.\n\nBegrunnelse fra KA: \"fordi det var feil\"")

        assertThat(opprettOppgavePayload.klagebehandlingEksternId).isEqualTo(behandling.eksternBehandlingId)
        assertThat(opprettOppgavePayload.fagsystem).isEqualTo(fagsak.fagsystem)
        assertThat(opprettOppgavePayload.behandlingstema).isNull()
        assertThat(opprettOppgavePayload.behandlingstype).isEqualTo(Behandlingstype.Klage.value)
    }
}
