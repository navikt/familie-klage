package no.nav.familie.klage.distribusjon

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.fagsak.domain.PersonIdent
import no.nav.familie.klage.integrasjoner.FamilieIntegrasjonerClient
import no.nav.familie.klage.testutil.BrukerContextUtil.clearBrukerContext
import no.nav.familie.klage.testutil.BrukerContextUtil.mockBrukerContext
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsakDomain
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokdist.AdresseType
import no.nav.familie.kontrakter.felles.dokdist.Distribusjonstype
import no.nav.familie.kontrakter.felles.dokdist.ManuellAdresse
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DistribusjonServiceTest {
    val behandlingService = mockk<BehandlingService>()
    val fagsakService = mockk<FagsakService>()
    val familieIntegrasjonerClient = mockk<FamilieIntegrasjonerClient>()

    val distribusjonService = DistribusjonService(familieIntegrasjonerClient, fagsakService, behandlingService)

    val ident = "1"
    val fagsak = fagsakDomain().tilFagsakMedPersonOgInstitusjon(setOf(PersonIdent(ident)))
    val behandlendeEnhet = "enhet"
    val behandling = behandling(fagsak = fagsak, behandlendeEnhet = behandlendeEnhet, resultat = BehandlingResultat.IKKE_MEDHOLD)

    val journalpostSlot = slot<ArkiverDokumentRequest>()

    @BeforeEach
    fun setUp() {
        mockBrukerContext()

        every { fagsakService.hentFagsakForBehandling(any()) } returns fagsak
        every { behandlingService.hentBehandling(any()) } returns behandling
        every {
            familieIntegrasjonerClient.arkiverDokument(
                capture(journalpostSlot),
                any(),
            )
        } returns ArkiverDokumentResponse("journalpostId", false)
    }

    @AfterEach
    fun tearDown() {
        clearBrukerContext()
    }

    @Test
    fun journalførBrev() {
        val mottaker = AvsenderMottaker(null, null, "navn")
        distribusjonService.journalførBrev(behandling.id, "123".toByteArray(), "saksbehandler", 0, mottaker)

        assertThat(journalpostSlot.captured.fagsakId).isEqualTo(fagsak.eksternId)
        assertThat(journalpostSlot.captured.fnr).isEqualTo(ident)
        assertThat(journalpostSlot.captured.journalførendeEnhet).isEqualTo(behandlendeEnhet)
        assertThat(journalpostSlot.captured.forsøkFerdigstill).isEqualTo(true)
        assertThat(journalpostSlot.captured.hoveddokumentvarianter.map { it.dokument }).contains("123".toByteArray())

        assertThat(journalpostSlot.captured.eksternReferanseId).isEqualTo("${behandling.eksternBehandlingId}-0")
    }

    @Test
    fun journalførSaksbehandlingsblankett() {
        distribusjonService.journalførSaksbehandlingsblankett(behandling.id, "pdf".toByteArray(), "saksbehandler")

        assertThat(journalpostSlot.captured.fagsakId).isEqualTo(fagsak.eksternId)
        assertThat(journalpostSlot.captured.fnr).isEqualTo(ident)
        assertThat(journalpostSlot.captured.journalførendeEnhet).isEqualTo(behandlendeEnhet)
        assertThat(journalpostSlot.captured.forsøkFerdigstill).isEqualTo(true)
        assertThat(journalpostSlot.captured.hoveddokumentvarianter.map { it.dokument }).contains("pdf".toByteArray())

        assertThat(journalpostSlot.captured.eksternReferanseId).isEqualTo("${behandling.eksternBehandlingId}-blankett")
    }

    @Test
    fun distribuerBrev() {
        val journalpostSlot = slot<String>()
        val distribusjonstypeSlot = slot<Distribusjonstype>()
        val adresseSlot = slot<ManuellAdresse>()
        val fagsystemSlot = slot<Fagsystem>()
        val journalpostId = "journalpostId"
        val manuellAdresse =
            ManuellAdresse(
                adresseType = AdresseType.norskPostadresse,
                adresselinje1 = "Adresseveien 1",
                postnummer = "0123",
                poststed = "Oslo",
            )

        every {
            familieIntegrasjonerClient.distribuerBrev(
                capture(journalpostSlot),
                capture(distribusjonstypeSlot),
                capture(adresseSlot),
                capture(fagsystemSlot),
            )
        } returns "distribusjonsnummer"

        distribusjonService.distribuerBrev(journalpostId, manuellAdresse, Fagsystem.EF)

        assertThat(journalpostSlot.captured).isEqualTo(journalpostId)
        assertThat(distribusjonstypeSlot.captured).isEqualTo(Distribusjonstype.ANNET)
        assertThat(adresseSlot.captured).isEqualTo(manuellAdresse)
        assertThat(fagsystemSlot.captured).isEqualTo(Fagsystem.EF)
    }
}
