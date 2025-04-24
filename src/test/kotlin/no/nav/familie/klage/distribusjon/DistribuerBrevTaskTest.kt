package no.nav.familie.klage.distribusjon

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.brev.domain.Brev
import no.nav.familie.klage.brev.domain.BrevmottakereJournalposter
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonMedIdent
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonUtenIdent
import no.nav.familie.klage.brevmottaker.domain.Brevmottakere
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle
import no.nav.familie.klage.distribusjon.domain.BrevmottakerJournalpost
import no.nav.familie.klage.distribusjon.domain.BrevmottakerJournalpostMedIdent
import no.nav.familie.klage.distribusjon.domain.BrevmottakerJournalpostUtenIdent
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.felles.domain.Fil
import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.dokdist.AdresseType
import no.nav.familie.kontrakter.felles.dokdist.ManuellAdresse
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID.randomUUID

internal class DistribuerBrevTaskTest {
    private val brevService = mockk<BrevService>()
    private val distribusjonService = mockk<DistribusjonService>()
    private val fagsakService = mockk<FagsakService>()
    private val featureToggleService = mockk<FeatureToggleService>()

    private val distribuerBrevTask =
        DistribuerBrevTask(brevService, distribusjonService, fagsakService, featureToggleService)

    private val behandlingId = randomUUID()
    private val slotJournalposter = mutableListOf<BrevmottakereJournalposter>()

    private val identForPersonMedIdent = "ident"
    private val idForPersonUtenIdent = randomUUID()

    @BeforeEach
    internal fun setUp() {
        every { distribusjonService.distribuerBrev(any(), any(), any()) } answers { "distId-${firstArg<String>()}" }
        justRun { brevService.oppdaterMottakerJournalpost(behandlingId, capture(slotJournalposter)) }
        every { fagsakService.hentFagsakForBehandling(behandlingId) } returns fagsak()
        every { featureToggleService.isEnabled(any()) } returns true
    }

    @Nested
    inner class ManglerJournalposter {
        @Test
        internal fun `feiler hvis journalposter er null`() {
            // Arrange
            mockHentBrev(journalposter = null)

            // Act & Assert
            assertThatThrownBy { doTask() }.hasMessageContaining("Mangler journalposter")

            // Assert
            verifyAntallKall(0)
        }

        @Test
        internal fun `feiler hvis journalposter er tom`() {
            // Arrange
            mockHentBrev(journalposter = emptyList())

            // Act & Assert
            assertThatThrownBy { doTask() }.hasMessageContaining("Mangler journalposter")

            // Assert
            verifyAntallKall(0)
        }
    }

    @Nested
    inner class ManglerBrevmottakere {

        @Test
        internal fun `feiler hvis brevmottakere er null når det finnes journalposter for mottaker uten ident`() {
            // Arrange
            val journalposter = listOf(journalpostUtenIdent("1"))
            mockHentBrev(journalposter = journalposter, brevmottakere = null)

            // Act & Assert
            assertThatThrownBy { doTask() }.hasMessageContaining("Mangler brevmottaker for journalpost=1")

            // Assert
            verifyAntallKall(0)
        }

        @Test
        internal fun `feiler hvis brevmottakere er tom når det finnes journalposter for mottaker uten ident`() {
            // Arrange
            val journalposter = listOf(journalpostUtenIdent("1"))
            mockHentBrev(journalposter = journalposter, brevmottakere = Brevmottakere())

            // Act & Assert
            assertThatThrownBy { doTask() }.hasMessageContaining("Mangler brevmottaker for journalpost=1")

            // Assert
            verifyAntallKall(0)
        }

        @Test
        internal fun `feiler ikke hvis brevmottakere er tom når det kun finnes journalposter for mottaker med ident`() {
            // Arrange
            val journalposter = listOf(journalpostMedIdent("1"))
            mockHentBrev(journalposter = journalposter, brevmottakere = Brevmottakere())

            // Act & Assert
            doTask()

            // Assert
            verifyAntallKall(1)
        }
    }

    @Test
    internal fun `skal distribuere og mellomlagre for hver journalpost`() {
        // Arrange
        val journalposter = listOf(journalpostMedIdent("1"), journalpostUtenIdent("2"))
        val brevmottakere = Brevmottakere(personer = listOf(personMedIdent, personUtenIdent))
        mockHentBrev(journalposter = journalposter, brevmottakere = brevmottakere)

        // Act
        doTask()

        // Assert
        verifyAntallKall(2)
        verifyOrder {
            distribusjonService.distribuerBrev("1", null, Fagsystem.EF)
            brevService.oppdaterMottakerJournalpost(
                behandlingId,
                BrevmottakereJournalposter(
                    listOf(
                        journalpostMedIdent("1", "distId-1"),
                        journalpostUtenIdent("2"),
                    ),
                ),
            )
            distribusjonService.distribuerBrev("2", manuellAdresse, Fagsystem.EF)
            brevService.oppdaterMottakerJournalpost(
                behandlingId,
                BrevmottakereJournalposter(
                    listOf(
                        journalpostMedIdent("1", "distId-1"),
                        journalpostUtenIdent("2", "distId-2"),
                    ),
                ),
            )
        }
    }

    @Test
    internal fun `skal fortsette distribuere de journalposter som mangler distribusjonsId`() {
        // Arrange
        val journalposter = listOf(journalpostMedIdent("1", "distId-1"), journalpostUtenIdent("2"))
        val brevmottakere = Brevmottakere(personer = listOf(personMedIdent, personUtenIdent))
        mockHentBrev(journalposter = journalposter, brevmottakere = brevmottakere)

        // Act
        doTask()

        // Assert
        verifyAntallKall(1)
        verifyOrder {
            distribusjonService.distribuerBrev("2", manuellAdresse, Fagsystem.EF)
            brevService.oppdaterMottakerJournalpost(
                behandlingId,
                BrevmottakereJournalposter(
                    listOf(
                        journalpostMedIdent("1", "distId-1"),
                        journalpostUtenIdent("2", "distId-2"),
                    ),
                ),
            )
        }
    }

    private fun verifyAntallKall(antall: Int) {
        verify(exactly = antall) { distribusjonService.distribuerBrev(any(), any(), any()) }
        verify(exactly = antall) { brevService.oppdaterMottakerJournalpost(any(), any()) }
    }

    private fun journalpostMedIdent(
        journalpostId: String,
        distribusjonId: String? = null,
    ) = BrevmottakerJournalpostMedIdent(identForPersonMedIdent, journalpostId, distribusjonId = distribusjonId)

    private fun journalpostUtenIdent(journalpostId: String, distribusjonId: String? = null) =
        BrevmottakerJournalpostUtenIdent(idForPersonUtenIdent, journalpostId, distribusjonId = distribusjonId)

    private val personMedIdent =
        BrevmottakerPersonMedIdent(identForPersonMedIdent, MottakerRolle.BRUKER, "Navn Navnesen")

    private val personUtenIdent =
        BrevmottakerPersonUtenIdent(
            id = idForPersonUtenIdent,
            mottakerRolle = MottakerRolle.BRUKER,
            navn = "Navn Navnesen",
            adresselinje1 = "Adresseveien 1",
            adresselinje2 = null,
            postnummer = "0123",
            poststed = "Oslo",
            landkode = "NO",
        )

    private val manuellAdresse =
        ManuellAdresse(
            adresseType = AdresseType.norskPostadresse,
            adresselinje1 = "Adresseveien 1",
            postnummer = "0123",
            poststed = "Oslo",
            land = "NO",
        )

    private fun doTask() {
        distribuerBrevTask.doTask(Task(DistribuerBrevTask.TYPE, behandlingId.toString()))
    }

    private fun mockHentBrev(
        journalposter: List<BrevmottakerJournalpost>? = null,
        brevmottakere: Brevmottakere? = null,
    ) {
        every { brevService.hentBrev(behandlingId) } returns
            Brev(
                behandlingId = behandlingId,
                saksbehandlerHtml = "",
                mottakereJournalposter = journalposter?.let { BrevmottakereJournalposter(it) },
                mottakere = brevmottakere,
                pdf = Fil(byteArrayOf()),
            )
    }
}
