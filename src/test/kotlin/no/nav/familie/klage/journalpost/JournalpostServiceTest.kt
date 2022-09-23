package no.nav.familie.klage.journalpost

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.klage.infrastruktur.exception.ApiFeil
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.integrasjoner.FamilieIntegrasjonerClient
import no.nav.familie.klage.testutil.DomainUtil.journalpost
import no.nav.familie.klage.testutil.DomainUtil.journalpostDokument
import no.nav.familie.kontrakter.felles.journalpost.Dokumentstatus
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

internal class JournalpostServiceTest {

    val familieIntegrasjonerClientMock = mockk<FamilieIntegrasjonerClient>()
    val journalpostService = JournalpostService(familieIntegrasjonerClientMock)
    val dokumentSomPdf = "123".toByteArray()
    val dokument1 = journalpostDokument()
    val dokument2 = journalpostDokument(dokumentvarianter = null)
    val journalpost = journalpost(dokumenter = listOf(dokument1, dokument2))

    @BeforeEach
    internal fun setUp() {
        every { familieIntegrasjonerClientMock.hentDokument(any(), any()) } returns dokumentSomPdf
    }

    @Test
    internal fun `skal hente ut dokument`() {
        Assertions.assertThat(journalpostService.hentDokument(journalpost, dokument1.dokumentInfoId)).isEqualTo(dokumentSomPdf)
    }

    @Test
    internal fun `skal ikke kunne hente ut dokument som ikke finnes i journalposten`() {
        assertThrows<Feil> {
            journalpostService.hentDokument(journalpost, UUID.randomUUID().toString())
        }
    }

    @Test
    internal fun `skal ikke kunne hente ut dokument som er under arbeid`() {
        assertThrows<ApiFeil> {
            journalpostService.hentDokument(journalpost, dokument2.dokumentInfoId)
        }
    }
}
