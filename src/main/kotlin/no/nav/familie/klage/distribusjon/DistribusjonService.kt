package no.nav.familie.klage.distribusjon

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.integrasjoner.FamilieIntegrasjonerClient
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import no.nav.familie.kontrakter.felles.dokdist.Distribusjonstype
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class DistribusjonService(
    private val familieIntegrasjonerClient: FamilieIntegrasjonerClient,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val brevService: BrevService
) {

    fun journalførBrev(behandlingId: UUID, saksbehandler: String): String {
        val brev = brevService.hentBrevPdf(behandlingId)

        return journalfør(
            behandlingId = behandlingId,
            pdf = brev,
            tittel = { "Brev for ${it.name.lowercase()}" }, // TODO: Utled en bra tittel her
            dokumenttype = Dokumenttype.BARNETRYGD_VEDTAK_INNVILGELSE, // TODO: Riktig dokumenttype
            saksbehandler = saksbehandler
        )
    }

    fun journalførSaksbehandlingsblankett(
        behandlingId: UUID,
        saksbehandlingsblankettPdf: ByteArray,
        saksbehandler: String
    ): String {
        return journalfør(
            behandlingId = behandlingId,
            pdf = saksbehandlingsblankettPdf,
            tittel = { "Saksbehandlingsblankett klage ${it.name.lowercase()}" },
            dokumenttype = Dokumenttype.BARNETRYGD_VEDTAK_INNVILGELSE, // TODO: Riktig dokumenttype
            saksbehandler = saksbehandler,
            suffixEksternReferanseId = "-blankett"
        )
    }

    private fun journalfør(
        behandlingId: UUID,
        pdf: ByteArray,
        tittel: (Stønadstype) -> String,
        dokumenttype: Dokumenttype,
        saksbehandler: String,
        suffixEksternReferanseId: String = ""
    ): String {
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)

        val dokument = lagDokument(
            pdf = pdf,
            dokumenttype = dokumenttype,
            tittel = tittel(fagsak.stønadstype)
        )
        val arkiverDokumentRequest = ArkiverDokumentRequest(
            fnr = fagsak.hentAktivIdent(),
            forsøkFerdigstill = true,
            hoveddokumentvarianter = listOf(dokument),
            vedleggsdokumenter = listOf(),
            fagsakId = fagsak.eksternId,
            journalførendeEnhet = behandling.behandlendeEnhet,
            eksternReferanseId = "${behandling.eksternBehandlingId}$suffixEksternReferanseId"
        )

        return familieIntegrasjonerClient.arkiverDokument(
            arkiverDokumentRequest,
            saksbehandler
        ).journalpostId
    }

    fun distribuerBrev(journalpostId: String): String {
        return familieIntegrasjonerClient.distribuerBrev(journalpostId, Distribusjonstype.ANNET)
    }

    private fun lagDokument(
        pdf: ByteArray,
        dokumenttype: Dokumenttype,
        tittel: String
    ): Dokument {
        return Dokument(
            dokument = pdf,
            filtype = Filtype.PDFA,
            filnavn = null,
            tittel = tittel,
            dokumenttype = dokumenttype
        )
    }
}
