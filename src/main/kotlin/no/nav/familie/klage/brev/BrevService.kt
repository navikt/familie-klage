package no.nav.familie.klage.brev

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.StegService
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.brev.domain.Brev
import no.nav.familie.klage.brev.domain.BrevMedAvsnitt
import no.nav.familie.klage.brev.dto.Avsnitt
import no.nav.familie.klage.brev.dto.FritekstBrevDto
import no.nav.familie.klage.brev.dto.FritekstBrevRequestDto
import no.nav.familie.klage.brev.dto.FritekstBrevtype
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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
@Service
class BrevService(
    private val brevClient: BrevClient,
    private val brevRepository: BrevRepository,
    private val avsnittRepository: AvsnittRepository,
    private val behandlingService: BehandlingService,
    private val familieDokumentClient: FamilieDokumentClient,
    private val brevsignaturService: BrevsignaturService,
    private val fagsakService: FagsakService,
    private val familieIntegrasjonerClient: FamilieIntegrasjonerClient,
    private val formService: FormService,
    private val formRepository: FormRepository,
    private val vurderingService: VurderingService,
    private val kabalService: KabalService,
    private val integrasjonerService: IntegrasjonerService,
    private val stegService: StegService
) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun hentMellomlagretBrev(behandlingId: UUID): BrevMedAvsnitt? {
        val eksisterer = sjekkOmBrevEksisterer(behandlingId)
        if (eksisterer) {
            val brev = brevRepository.findByIdOrThrow(behandlingId)
            val avsnitt = avsnittRepository.hentAvsnittPåBehandlingId(behandlingId)
            return BrevMedAvsnitt(behandlingId, brev.overskrift, avsnitt)
        }
        return null
    }

    fun sjekkOmBrevEksisterer(id: UUID): Boolean {
        return brevRepository.findById(id).isPresent
    }

    @Transactional
    fun lagEllerOppdaterBrev(fritekstbrevDto: FritekstBrevDto): ByteArray {
        val navn = behandlingService.hentNavnFraBehandlingsId(fritekstbrevDto.behandlingId)
        val behandling = behandlingService.hentBehandling(fritekstbrevDto.behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)

        slettAvsnittOmEksisterer(fritekstbrevDto.behandlingId)

        val request = FritekstBrevRequestDto(
            overskrift = fritekstbrevDto.overskrift,
            avsnitt = fritekstbrevDto.avsnitt,
            personIdent = fagsak.hentAktivIdent(),
            navn = navn
        )

        val signaturMedEnhet = brevsignaturService.lagSignatur(behandling.id)

        val html = brevClient.genererHtmlFritekstbrev(
            fritekstBrev = request,
            saksbehandlerNavn = signaturMedEnhet.navn,
            enhet = signaturMedEnhet.enhet
        )

        lagEllerOppdaterBrev(
            behandlingId = fritekstbrevDto.behandlingId,
            overskrift = fritekstbrevDto.overskrift,
            saksbehandlerHtml = html,
            brevtype = fritekstbrevDto.brevType

        )

        for (avsnitt in fritekstbrevDto.avsnitt) {
            lagEllerOppdaterAvsnitt(
                avsnittId = avsnitt.avsnittId,
                behandlingId = fritekstbrevDto.behandlingId,
                deloverskrift = avsnitt.deloverskrift,
                innhold = avsnitt.innhold,
                skalSkjulesIBrevBygger = avsnitt.skalSkjulesIBrevbygger
            )
        }

        return familieDokumentClient.genererPdfFraHtml(html)
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
        val behandling = behandlingService.hentBehandling(behandlingId)
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
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        val vurdering = vurderingService.hentVurdering(behandlingId)

        kabalService.sendTilKabal(fagsak, behandling, vurdering)
    }

    private fun skalSendeTilKabal(behandlingId: UUID): Boolean {
        val form = formRepository.findByIdOrThrow(behandlingId)
        return formService.formkravErOppfylt(form) && vurderingService.klageTasIkkeTilFølge(behandlingId)
    }

    fun lagEllerOppdaterBrev(
        behandlingId: UUID,
        overskrift: String,
        saksbehandlerHtml: String,
        brevtype: FritekstBrevtype
    ): Brev {
        val brev =
            Brev(
                behandlingId = behandlingId,
                overskrift = overskrift,
                saksbehandlerHtml = saksbehandlerHtml,
                brevtype = brevtype
            )

        return when (brevRepository.existsById(brev.behandlingId)) {
            true -> brevRepository.update(brev)
            false -> brevRepository.insert(brev)
        }
    }

    fun lagEllerOppdaterAvsnitt(
        avsnittId: UUID,
        behandlingId: UUID,
        deloverskrift: String,
        innhold: String,
        skalSkjulesIBrevBygger: Boolean?
    ): Avsnitt {
        val avsnitt = Avsnitt(
            avsnittId = avsnittId,
            behandlingId = behandlingId,
            deloverskrift = deloverskrift,
            innhold = innhold,
            skalSkjulesIBrevbygger = skalSkjulesIBrevBygger,
        )
        return when (avsnittRepository.existsById(avsnittId)) {
            true -> avsnittRepository.update(avsnitt)
            false -> avsnittRepository.insert(avsnitt)
        }
    }

    fun slettAvsnittOmEksisterer(behandlingId: UUID) {
        avsnittRepository.slettAvsnittMedBehanldingId(behandlingId)
    }
}
