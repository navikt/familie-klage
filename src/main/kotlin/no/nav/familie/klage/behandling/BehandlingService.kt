package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandling.dto.BehandlingDto
import no.nav.familie.klage.behandling.dto.tilDto
import no.nav.familie.klage.brev.BrevRepository
import no.nav.familie.klage.brev.FamilieDokumentClient
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.formkrav.FormRepository
import no.nav.familie.klage.formkrav.FormService
import no.nav.familie.klage.integrasjoner.FamilieIntegrasjonerClient
import no.nav.familie.klage.integrasjoner.IntegrasjonerService
import no.nav.familie.klage.kabal.KabalService
import no.nav.familie.klage.repository.findByIdOrThrow
import no.nav.familie.klage.vurdering.VurderingService
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokdist.Distribusjonstype
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BehandlingService(
    private val behandlingsRepository: BehandlingsRepository,
    private val fagsakService: FagsakService,
    private val brevRepository: BrevRepository,
    private val familieDokumentClient: FamilieDokumentClient,
    private val familieIntegrasjonerClient: FamilieIntegrasjonerClient,
    private val formService: FormService,
    private val formRepository: FormRepository,
    private val vurderingService: VurderingService,
    private val kabalService: KabalService,
    private val integrasjonerService: IntegrasjonerService,
    private val stegService: StegService
) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun hentBehandling(behandlingId: UUID): Behandling = behandlingsRepository.findByIdOrThrow(behandlingId)

    fun hentBehandlingDto(behandlingId: UUID): BehandlingDto {
        val stønadstype = fagsakService.hentFagsakForBehandling(behandlingId).stønadstype
        return behandlingsRepository.findByIdOrThrow(behandlingId).tilDto(stønadstype)
    }

    fun hentNavnFraBehandlingsId(behandlingId: UUID): String {
        return "Navn Navnesen"
    }

    @Transactional
    fun opprettBehandling(
        opprettKlageBehandlingDto: OpprettKlagebehandlingRequest
    ): UUID {

        val fagsak = fagsakService.hentEllerOpprettFagsak(
            opprettKlageBehandlingDto.ident,
            opprettKlageBehandlingDto.eksternFagsakId,
            opprettKlageBehandlingDto.fagsystem,
            opprettKlageBehandlingDto.stønadstype
        )

        return behandlingsRepository.insert(
            Behandling(
                fagsakId = fagsak.id,
                eksternBehandlingId = opprettKlageBehandlingDto.eksternBehandlingId,
                klageMottatt = opprettKlageBehandlingDto.klageMottatt
            )
        ).id
    }

    @Transactional
    fun ferdigstillBrev(behandlingId: UUID) {
        arkiverOgDistribuerBrev(behandlingId)
        if (skalSendeTilKabal(behandlingId)) {
            sendTilKabal(behandlingId)
            stegService.oppdaterSteg(behandlingId, StegType.OVERFØRING_TIL_KABAL)
        } else {
            stegService.oppdaterSteg(behandlingId, StegType.BEHANDLING_FERDIGSTILT)
        }
    }

    fun arkiverOgDistribuerBrev(behandlingId: UUID) {
        val brev = brevRepository.findByIdOrThrow(behandlingId)
        val behandling = behandlingsRepository.findByIdOrThrow(behandlingId)
        val pdf = familieDokumentClient.genererPdfFraHtml(brev.saksbehandlerHtml)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)

        val arkiverDokumentRequest = integrasjonerService.lagArkiverDokumentRequest(
            personIdent = fagsak.hentAktivIdent(),
            pdf = pdf,
            fagsakId = fagsak.eksternId,
            behandlingId = behandlingId,
            enhet = "enhet",
            stønadstype = fagsak.stønadstype,
            dokumenttype = Dokumenttype.BARNETRYGD_VEDTAK_INNVILGELSE // TODO: Riktig dokumenttype
        )

        val respons =
            familieIntegrasjonerClient.arkiverDokument(
                arkiverDokumentRequest,
                "Maja"
            ) // TODO: Hente en saksbehandlere her
        logger.info("Mottok id fra JoArk: ${respons.journalpostId}")

        val distnummer = familieIntegrasjonerClient.distribuerBrev(
            respons.journalpostId,
            Distribusjonstype.ANNET
        )

        logger.info("Mottok distnummer fra DokDist: $distnummer")
    }

    fun sendTilKabal(behandlingId: UUID) {
        logger.info("send til kabal")
        val fagsakId = behandlingsRepository.findByIdOrThrow(behandlingId).fagsakId
        kabalService.sendTilKabal(behandlingId, fagsakId)
    }

    private fun skalSendeTilKabal(behandlingId: UUID): Boolean {
        val form = formRepository.findByIdOrThrow(behandlingId)
        return formService.formkravErOppfylt(form) && vurderingService.klageTasIkkeTilFølge(behandlingId)
    }

    fun hentAktivIdent(behandlingId: UUID): String {
        val behandling = hentBehandling(behandlingId)

        return fagsakService.hentFagsak(behandling.fagsakId).hentAktivIdent()
    }
}
