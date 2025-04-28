package no.nav.familie.klage.distribusjon

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.brev.domain.Brev
import no.nav.familie.klage.brev.domain.BrevmottakereJournalposter
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerOrganisasjon
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonMedIdent
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonUtenIdent
import no.nav.familie.klage.brevmottaker.domain.Brevmottakere
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle
import no.nav.familie.klage.distribusjon.domain.BrevmottakerJournalpost
import no.nav.familie.klage.distribusjon.domain.BrevmottakerJournalpostMedIdent
import no.nav.familie.klage.distribusjon.domain.BrevmottakerJournalpostUtenIdent
import no.nav.familie.klage.felles.domain.Fil
import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottakerIdType
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Properties
import java.util.UUID

internal class JournalførBrevTaskTest {
    val behandlingService = mockk<BehandlingService>()
    val taskService = mockk<TaskService>()
    val distribusjonService = mockk<DistribusjonService>()
    val brevService = mockk<BrevService>()
    val featureToggleService = mockk<FeatureToggleService>()

    val journalførBrevTask =
        JournalførBrevTask(
            distribusjonService = distribusjonService,
            taskService = taskService,
            behandlingService = behandlingService,
            brevService = brevService,
            featureToggleService = featureToggleService,
        )

    val behandlingId = UUID.randomUUID()
    val journalpostId = "12345678"
    val propertiesMedJournalpostId =
        Properties().apply {
            this["journalpostId"] = journalpostId
        }

    val slotBrevmottakereJournalposter = mutableListOf<BrevmottakereJournalposter>()
    val slotSaveTask = mutableListOf<Task>()

    @BeforeEach
    internal fun setUp() {
        every { behandlingService.hentAktivIdent(behandlingId) } returns Pair("ident", fagsak())
        justRun { brevService.oppdaterMottakerJournalpost(any(), capture(slotBrevmottakereJournalposter)) }
        every { taskService.save(capture(slotSaveTask)) } answers { firstArg() }
        every {
            distribusjonService.journalførBrev(any(), any(), any(), any(), any())
        } answers { "journalpostId-${(it.invocation.args[3] as Int)}" }
        every { featureToggleService.isEnabled(any()) } returns true
    }

    @Test
    internal fun `skal ikke opprette sendTilKabalTask hvis behandlingen har annen status enn IKKE_MEDHOLD`() {
        val taskSlots = mutableListOf<Task>()
        every { taskService.save(capture(taskSlots)) } answers { firstArg() }
        every { behandlingService.hentBehandling(any()) } returns
            DomainUtil.behandling(resultat = BehandlingResultat.IKKE_MEDHOLD_FORMKRAV_AVVIST)

        journalførBrevTask.onCompletion(Task(JournalførBrevTask.TYPE, behandlingId.toString(), propertiesMedJournalpostId))

        assertThat(taskSlots).hasSize(1)
        assertThat(taskSlots.first().type).isEqualTo(DistribuerBrevTask.TYPE)
    }

    @Test
    internal fun `skal opprette sendTilKabalTask og distribuerBrevTask hvis behandlingsresultatet er IKKE_MEDHOLD`() {
        val taskSlots = mutableListOf<Task>()
        every { taskService.save(capture(taskSlots)) } answers { firstArg() }
        every { behandlingService.hentBehandling(any()) } returns DomainUtil.behandling(resultat = BehandlingResultat.IKKE_MEDHOLD)

        journalførBrevTask.onCompletion(Task(JournalførBrevTask.TYPE, behandlingId.toString(), propertiesMedJournalpostId))

        assertThat(taskSlots).hasSize(2)
        assertThat(taskSlots.first().type).isEqualTo(SendTilKabalTask.TYPE)
        assertThat(taskSlots.last().type).isEqualTo(DistribuerBrevTask.TYPE)
    }

    @Nested
    inner class JournalførMottakere {
        val avsenderMottaker1 = AvsenderMottaker("1", AvsenderMottakerIdType.FNR, "1navn")
        val avsenderMottaker2 = AvsenderMottaker("2", AvsenderMottakerIdType.FNR, "2navn")
        val avsenderMottakerOrganisasjon = AvsenderMottaker("org1", AvsenderMottakerIdType.ORGNR, "mottaker")
        val avsenderMottakerUtenIdent = AvsenderMottaker(null, null, "3navn")

        val brevmottakerUtenIdent =
            BrevmottakerPersonUtenIdent(
                id = UUID.randomUUID(),
                mottakerRolle = MottakerRolle.FULLMAKT,
                navn = "3navn",
                adresselinje1 = "Dirección Calle 1",
                adresselinje2 = null,
                postnummer = "0000",
                poststed = "Barcelona",
                landkode = "ES",
            )

        val brevmottakere =
            Brevmottakere(
                listOf(
                    BrevmottakerPersonMedIdent("1", MottakerRolle.BRUKER, "1navn"),
                    BrevmottakerPersonMedIdent("2", MottakerRolle.FULLMAKT, "2navn"),
                    brevmottakerUtenIdent,
                ),
                listOf(BrevmottakerOrganisasjon("org1", "orgnavn", "mottaker")),
            )

        @Test
        internal fun `skal lagre hver person i listen over mottakere`() {
            mockHentBrev(mottakere = brevmottakere)

            runTask()

            verifyJournalførtBrev(4)
            verifyOrder {
                distribusjonService.journalførBrev(behandlingId, any(), any(), 0, avsenderMottaker1)
                distribusjonService.journalførBrev(behandlingId, any(), any(), 1, avsenderMottaker2)
                distribusjonService.journalførBrev(behandlingId, any(), any(), 2, avsenderMottakerUtenIdent)
                distribusjonService.journalførBrev(behandlingId, any(), any(), 3, avsenderMottakerOrganisasjon)
            }

            validerLagringAvBrevmottakereJournalposter(slotBrevmottakereJournalposter[3].journalposter)
        }

        @Test
        internal fun `skal fortsette fra forrige state`() {
            val journalposter =
                listOf(
                    BrevmottakerJournalpostMedIdent(avsenderMottaker1.id!!, "journalpostId-0"),
                    BrevmottakerJournalpostMedIdent(avsenderMottaker2.id!!, "journalpostId-1"),
                )
            mockHentBrev(mottakere = brevmottakere, BrevmottakereJournalposter(journalposter))

            runTask()

            verifyJournalførtBrev(2)
            verifyOrder {
                distribusjonService.journalførBrev(behandlingId, any(), any(), 2, avsenderMottakerUtenIdent)
                distribusjonService.journalførBrev(behandlingId, any(), any(), 3, avsenderMottakerOrganisasjon)
            }

            validerLagringAvBrevmottakereJournalposter(slotBrevmottakereJournalposter[1].journalposter)
        }

        @Test
        internal fun `skal fortsette fra forrige state når organisasjon har mottatt`() {
            val journalposter =
                listOf(
                    BrevmottakerJournalpostMedIdent(avsenderMottaker1.id!!, "journalpostId-0"),
                    BrevmottakerJournalpostMedIdent(avsenderMottaker2.id!!, "journalpostId-1"),
                    BrevmottakerJournalpostMedIdent(avsenderMottakerOrganisasjon.id!!, "journalpostId-2"),
                )
            mockHentBrev(mottakere = brevmottakere, BrevmottakereJournalposter(journalposter))

            runTask()

            verifyJournalførtBrev(1)
            verifyOrder {
                distribusjonService.journalførBrev(behandlingId, any(), any(), 3, avsenderMottakerUtenIdent)
            }

            validerLagringAvBrevmottakereJournalposter(
                journalposter = slotBrevmottakereJournalposter.single().journalposter,
                mottakere =
                    listOf(
                        avsenderMottaker1,
                        avsenderMottaker2,
                        avsenderMottakerOrganisasjon,
                        avsenderMottakerUtenIdent,
                    ),
            )
        }

        private fun verifyJournalførtBrev(antallGanger: Int) {
            verify(exactly = antallGanger) {
                distribusjonService.journalførBrev(behandlingId, any(), any(), any(), any())
            }
        }

        private fun validerLagringAvBrevmottakereJournalposter(
            journalposter: List<BrevmottakerJournalpost>,
            mottakere: List<AvsenderMottaker> =
                listOf(
                    avsenderMottaker1,
                    avsenderMottaker2,
                    avsenderMottakerUtenIdent,
                    avsenderMottakerOrganisasjon,
                ),
        ) {
            assertThat(journalposter).hasSize(mottakere.size)
            mottakere.forEachIndexed { index, avsenderMottaker ->
                when (val journalpost = journalposter[index]) {
                    is BrevmottakerJournalpostMedIdent -> assertThat(journalpost.ident).isEqualTo(avsenderMottaker.id)
                    is BrevmottakerJournalpostUtenIdent -> assertThat(journalpost.id).isEqualTo(brevmottakerUtenIdent.id)
                }
                assertThat(journalposter[index].journalpostId).isEqualTo("journalpostId-$index")
            }
        }
    }

    private fun runTask(): Task {
        val task = Task(JournalførBrevTask.TYPE, behandlingId.toString())
        journalførBrevTask.doTask(task)
        return task
    }

    private fun mockHentBrev(
        mottakere: Brevmottakere? = null,
        mottakereJournalpost: BrevmottakereJournalposter? = null,
    ) {
        every { brevService.hentBrev(behandlingId) } returns
            Brev(
                behandlingId = behandlingId,
                saksbehandlerHtml = "",
                mottakere = mottakere,
                mottakereJournalposter = mottakereJournalpost,
                pdf = Fil(byteArrayOf()),
            )
    }
}
