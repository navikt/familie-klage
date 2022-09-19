package no.nav.familie.klage.distribusjon

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
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

    fun journalførBrev(behandlingId: UUID): String {
        val brev = brevService.lagBrevSomPdf(behandlingId)
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)

        val arkiverDokumentRequest = lagArkiverDokumentRequest(
            personIdent = fagsak.hentAktivIdent(),
            pdf = brev,
            fagsakId = fagsak.eksternId,
            eksternBehandlingId = behandling.eksternBehandlingId,
            enhet = behandling.behandlendeEnhet,
            stønadstype = fagsak.stønadstype,
            dokumenttype = Dokumenttype.BARNETRYGD_VEDTAK_INNVILGELSE // TODO: Riktig dokumenttype
        )

        return familieIntegrasjonerClient.arkiverDokument(
            arkiverDokumentRequest,
            SikkerhetContext.hentSaksbehandler(true)
        ).journalpostId
    }

    fun distribuerBrev(journalpostId: String): String {
        return familieIntegrasjonerClient.distribuerBrev(journalpostId, Distribusjonstype.ANNET)
    }

    private fun lagArkiverDokumentRequest(
        personIdent: String,
        pdf: ByteArray,
        fagsakId: String,
        eksternBehandlingId: UUID,
        enhet: String,
        stønadstype: Stønadstype,
        dokumenttype: Dokumenttype
    ): ArkiverDokumentRequest {
        val dokument = Dokument(
            pdf,
            Filtype.PDFA,
            null,
            "Brev for ${stønadstype.name.lowercase()}", // TODO: Utled en bra tittel her
            dokumenttype
        )
        return ArkiverDokumentRequest(
            fnr = personIdent,
            forsøkFerdigstill = true,
            hoveddokumentvarianter = listOf(dokument),
            vedleggsdokumenter = listOf(),
            fagsakId = fagsakId,
            journalførendeEnhet = enhet,
            eksternReferanseId = "$eksternBehandlingId-$stønadstype-klage"
        )
    }
}
