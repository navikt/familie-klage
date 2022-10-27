package no.nav.familie.klage.distribusjon

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.brev.domain.Brev
import no.nav.familie.klage.brev.domain.BrevmottakerOrganisasjon
import no.nav.familie.klage.brev.domain.BrevmottakerPerson
import no.nav.familie.klage.brev.domain.Brevmottakere
import no.nav.familie.klage.brev.domain.BrevmottakereJournalpost
import no.nav.familie.klage.brev.domain.BrevmottakereJournalposter
import no.nav.familie.klage.brev.domain.MottakerRolle
import no.nav.familie.klage.felles.domain.Fil
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.klage.testutil.DomainUtil.defaultIdent
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Properties
import java.util.UUID

internal class JournalførBrevTaskTest {

    val behandlingService = mockk<BehandlingService>()
    val taskRepository = mockk<TaskRepository>()
    val distribusjonService = mockk<DistribusjonService>()
    val brevService = mockk<BrevService>()

    val journalførBrevTask = JournalførBrevTask(
        distribusjonService = distribusjonService,
        taskRepository = taskRepository,
        behandlingService = behandlingService,
        brevService = brevService
    )

    val behandlingId = UUID.randomUUID()
    val journalpostId = "12345678"
    val propertiesMedJournalpostId = Properties().apply {
        this["journalpostId"] = journalpostId
    }

    val slotBrevmottakereJournalposter = mutableListOf<BrevmottakereJournalposter>()
    val slotSaveTask = mutableListOf<Task>()

    @BeforeEach
    internal fun setUp() {
        every { behandlingService.hentAktivIdent(behandlingId) } returns Pair("ident", fagsak())
        justRun { brevService.oppdaterMottakerJournalpost(any(), capture(slotBrevmottakereJournalposter)) }
        every { taskRepository.save(capture(slotSaveTask)) } answers { firstArg() }
        every {
            distribusjonService.journalførBrev(any(), any(), any(), any(), any())
        } answers { "journalpostId-${(it.invocation.args[3] as Int)}" }
    }

    @Test
    internal fun `skal ikke opprette sendTilKabalTask hvis behandlingen har annen status enn IKKE_MEDHOLD`() {
        val taskSlots = mutableListOf<Task>()
        every { taskRepository.save(capture(taskSlots)) } answers { firstArg() }
        every { behandlingService.hentBehandling(any()) } returns DomainUtil.behandling(resultat = BehandlingResultat.IKKE_MEDHOLD_FORMKRAV_AVVIST)

        journalførBrevTask.onCompletion(Task(JournalførBrevTask.TYPE, behandlingId.toString(), propertiesMedJournalpostId))

        assertThat(taskSlots).hasSize(0)
    }

    @Test
    internal fun `skal opprette sendTilKabalTask og distribuerBrevTask hvis behandlingsresultatet er IKKE_MEDHOLD`() {
        val taskSlots = mutableListOf<Task>()
        every { taskRepository.save(capture(taskSlots)) } answers { firstArg() }
        every { behandlingService.hentBehandling(any()) } returns DomainUtil.behandling(resultat = BehandlingResultat.IKKE_MEDHOLD)

        journalførBrevTask.onCompletion(Task(JournalførBrevTask.TYPE, behandlingId.toString(), propertiesMedJournalpostId))

        assertThat(taskSlots.single().type).isEqualTo(SendTilKabalTask.TYPE)
    }

    @Nested
    inner class JournalførSøker {

        @Test
        internal fun `skal journalføre brev for søker når det ikke finnes mottakere`() {
            mockHentBrev(mottakere = null)

            runTask()

            verify { distribusjonService.journalførBrev(behandlingId, any(), any(), 0, null) }

            val brevmottakereJournalposter = slotBrevmottakereJournalposter.single()
            val journalpost = brevmottakereJournalposter.journalposter.single()
            assertThat(journalpost.ident).isEqualTo(defaultIdent)
            assertThat(journalpost.journalpostId).isEqualTo("journalpostId-0")
        }

        @Test
        internal fun `skal journalføre brev for søker når det finnes mottaker med tomme lister`() {
            mockHentBrev(mottakere = Brevmottakere(emptyList(), emptyList()))

            runTask()

            verify { distribusjonService.journalførBrev(behandlingId, any(), any(), 0, null) }

            val brevmottakereJournalposter = slotBrevmottakereJournalposter.single()
            val journalpost = brevmottakereJournalposter.journalposter.single()
            assertThat(journalpost.ident).isEqualTo(defaultIdent)
            assertThat(journalpost.journalpostId).isEqualTo("journalpostId-0")
            assertThat(slotSaveTask.single().payload).isEqualTo("journalpostId-0")
        }

    }

    @Nested
    inner class JournalførMottakere {

        val mottakerPerson = AvsenderMottaker("1", BrukerIdType.FNR, "1navn")
        val mottakerPerson2 = AvsenderMottaker("2", BrukerIdType.FNR, "2navn")
        val mottakerOrganisasjon = AvsenderMottaker("org1", BrukerIdType.ORGNR, "orgnavn")

        val mottakere = Brevmottakere(
            listOf(
                BrevmottakerPerson("1", "1navn", MottakerRolle.BRUKER),
                BrevmottakerPerson("2", "2navn", MottakerRolle.FULLMAKT)
            ),
            listOf(BrevmottakerOrganisasjon("org1", "orgnavn", MottakerRolle.VERGE))
        )

        @Test
        internal fun `skal lagre hver person i listen over mottakere`() {
            mockHentBrev(mottakere = mottakere)

            runTask()

            verifyJournalførtBrev(3)
            verifyOrder {
                distribusjonService.journalførBrev(behandlingId, any(), any(), 0, mottakerPerson)
                distribusjonService.journalførBrev(behandlingId, any(), any(), 1, mottakerPerson2)
                distribusjonService.journalførBrev(behandlingId, any(), any(), 2, mottakerOrganisasjon)
            }

            validerLagringAvBrevmottakereJournalposter(slotBrevmottakereJournalposter[2].journalposter)
            validerOpprettetTasks()
        }

        @Test
        internal fun `skal fortsette fra forrige state`() {
            val journalposter = listOf(
                BrevmottakereJournalpost(mottakerPerson.id!!, "journalpostId-0"),
                BrevmottakereJournalpost(mottakerPerson2.id!!, "journalpostId-1")
            )
            mockHentBrev(mottakere = mottakere, BrevmottakereJournalposter(journalposter))

            runTask()

            verifyJournalførtBrev(1)
            verifyOrder {
                distribusjonService.journalførBrev(behandlingId, any(), any(), 2, mottakerOrganisasjon)
            }

            validerLagringAvBrevmottakereJournalposter(slotBrevmottakereJournalposter.single().journalposter)
            validerOpprettetTasks()
        }

        private fun verifyJournalførtBrev(antallGanger: Int) {
            verify(exactly = antallGanger) {
                distribusjonService.journalførBrev(behandlingId, any(), any(), any(), any())
            }
        }

        private fun validerOpprettetTasks() {
            assertThat(slotSaveTask.map { it.payload }).containsExactly("journalpostId-0", "journalpostId-1", "journalpostId-2")
        }

        private fun validerLagringAvBrevmottakereJournalposter(
            journalposter: List<BrevmottakereJournalpost>,
            mottakere: List<AvsenderMottaker> = listOf(mottakerPerson, mottakerPerson2, mottakerOrganisasjon)
        ) {
            assertThat(journalposter).hasSize(3)
            mottakere.forEachIndexed { index, avsenderMottaker ->
                assertThat(journalposter[index].ident).isEqualTo(avsenderMottaker.id)
                assertThat(journalposter[index].journalpostId).isEqualTo("journalpostId-$index")
            }
        }
    }

    private fun runTask(): Task {
        val task = Task(JournalførBrevTask.TYPE, behandlingId.toString())
        journalførBrevTask.doTask(task)
        return task
    }

    private fun mockHentBrev(mottakere: Brevmottakere? = null, mottakereJournalpost: BrevmottakereJournalposter? = null) {
        every { brevService.hentBrev(behandlingId) } returns
                Brev(
                    behandlingId = behandlingId,
                    saksbehandlerHtml = "",
                    mottakere = mottakere,
                    mottakereJournalpost = mottakereJournalpost,
                    pdf = Fil(byteArrayOf())
                )
    }
}
