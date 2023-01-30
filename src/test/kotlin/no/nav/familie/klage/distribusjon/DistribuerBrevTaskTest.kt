package no.nav.familie.klage.distribusjon

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.brev.domain.Brev
import no.nav.familie.klage.brev.domain.BrevmottakereJournalpost
import no.nav.familie.klage.brev.domain.BrevmottakereJournalposter
import no.nav.familie.klage.felles.domain.Fil
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

internal class DistribuerBrevTaskTest {

    private val brevService = mockk<BrevService>()
    private val distribusjonService = mockk<DistribusjonService>()

    val distribuerBrevTask = DistribuerBrevTask(brevService, distribusjonService)

    val behandlingId = UUID.randomUUID()
    val slotJournalposter = mutableListOf<BrevmottakereJournalposter>()

    @BeforeEach
    internal fun setUp() {
        every { distribusjonService.distribuerBrev(any()) } answers { "distId-${firstArg<String>()}" }
        justRun { brevService.oppdaterMottakerJournalpost(behandlingId, capture(slotJournalposter)) }
    }

    @Nested
    inner class ManglerJournalposter {

        @Test
        internal fun `feiler hvis det ikke finnes noen journalpost`() {
            mockHentBrev(null)
            assertThatThrownBy {
                doTask()
            }.hasMessageContaining("Mangler journalposter")

            verifyAntallKall(0)
        }

        @Test
        internal fun `feiler hvis journalpost er empty`() {
            mockHentBrev(emptyList())
            assertThatThrownBy {
                doTask()
            }.hasMessageContaining("Mangler journalposter")

            verifyAntallKall(0)
        }
    }

    @Test
    internal fun `skal distribuere og mellomlagre for hver journalpost`() {
        mockHentBrev(listOf(journalpost("1"), journalpost("2")))
        doTask()
        verifyAntallKall(2)
        verifyOrder {
            distribusjonService.distribuerBrev("1")
            brevService.oppdaterMottakerJournalpost(
                behandlingId,
                BrevmottakereJournalposter(
                    listOf(
                        journalpost("1", "distId-1"),
                        journalpost("2"),
                    ),
                ),
            )
            distribusjonService.distribuerBrev("2")
            brevService.oppdaterMottakerJournalpost(
                behandlingId,
                BrevmottakereJournalposter(
                    listOf(
                        journalpost("1", "distId-1"),
                        journalpost("2", "distId-2"),
                    ),
                ),
            )
        }
    }

    @Test
    internal fun `skal fortsette distribuere de journalposter som mangler distribusjonsId`() {
        mockHentBrev(listOf(journalpost("1", "distId-1"), journalpost("2")))
        doTask()
        verifyAntallKall(1)

        verifyOrder {
            distribusjonService.distribuerBrev("2")
            brevService.oppdaterMottakerJournalpost(
                behandlingId,
                BrevmottakereJournalposter(
                    listOf(
                        journalpost("1", "distId-1"),
                        journalpost("2", "distId-2"),
                    ),
                ),
            )
        }
    }

    private fun verifyAntallKall(antall: Int) {
        verify(exactly = antall) { distribusjonService.distribuerBrev(any()) }
        verify(exactly = antall) { brevService.oppdaterMottakerJournalpost(any(), any()) }
    }

    private fun journalpost(journalpostId: String, distribusjonId: String? = null) =
        BrevmottakereJournalpost("ident", journalpostId, distribusjonId = distribusjonId)

    private fun doTask() {
        distribuerBrevTask.doTask(Task(DistribuerBrevTask.TYPE, behandlingId.toString()))
    }

    private fun mockHentBrev(journalposter: List<BrevmottakereJournalpost>? = null) {
        every { brevService.hentBrev(behandlingId) } returns
            Brev(
                behandlingId = behandlingId,
                saksbehandlerHtml = "",
                mottakereJournalposter = journalposter?.let { BrevmottakereJournalposter(it) },
                pdf = Fil(byteArrayOf()),
            )
    }
}
