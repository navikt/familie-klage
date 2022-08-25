package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandling.dto.BehandlingDto
import no.nav.familie.klage.behandling.dto.tilDto
import no.nav.familie.klage.brev.BrevRepository
import no.nav.familie.klage.brev.FamilieDokumentClient
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.fagsak.domain.Stønadstype
import no.nav.familie.klage.formkrav.FormRepository
import no.nav.familie.klage.formkrav.FormService
import no.nav.familie.klage.integrasjoner.FamilieIntegrasjonerClient
import no.nav.familie.klage.integrasjoner.IntegrasjonerService
import no.nav.familie.klage.kabal.KabalService
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import no.nav.familie.klage.repository.findByIdOrThrow
import no.nav.familie.klage.vurdering.VurderingService
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokdist.Distribusjonstype
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class BehandlingService(
    private val behandlingsRepository: BehandlingsRepository,
    private val personopplysningerService: PersonopplysningerService,
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
    fun hentBehandling(behandlingId: UUID): BehandlingDto {
        val behandling = behandlingsRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
        return behandling.tilDto(fagsak)
    }

    fun hentNavnFraBehandlingsId(behandlingId: UUID): String {
        return "Navn Navnesen"
    }

    @Transactional
    fun opprettBehandling(
        ident: String,
        stønadsype: Stønadstype,
        eksternBehandlingId: String,
        eksternFagsakId: String,
        fagsystem: Fagsystem,
        klageMottatt: LocalDate
    ): UUID { // TODO: Bruk et dto-objekt for disse fire feltene

        val fagsak = fagsakService.hentEllerOpprettFagsak(ident, eksternFagsakId, fagsystem, stønadsype)

        return behandlingsRepository.insert(
            Behandling(
                fagsakId = fagsak.id,
                eksternBehandlingId = eksternBehandlingId,
                klageMottatt = klageMottatt
            )
        ).id
    }

    @Transactional
    fun ferdigstillBrev(behandlingId: UUID) {
        stegService.oppdaterSteg(behandlingId, StegType.BREV, true)

        arkiverOgDistribuerBrev(behandlingId)
        sendTilKabal(behandlingId)
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
            familieIntegrasjonerClient.arkiverDokument(arkiverDokumentRequest, "Maja") // TODO: Hente en saksbehandlere her
        logger.info("Mottok id fra JoArk: ${respons.journalpostId}")

        val distnummer = familieIntegrasjonerClient.distribuerBrev(
            respons.journalpostId,
            Distribusjonstype.ANNET
        )

        logger.info("Mottok distnummer fra DokDist: $distnummer")
    }

    fun sendTilKabal(behandlingId: UUID) {
        val form = formRepository.findByIdOrThrow(behandlingId)
        if (
            formService.formkravErOppfylt(form) &&
            vurderingService.klageTasIkkeTilFølge(behandlingId)
        ) {
            logger.info("send til kabal")
            val fagsakId = behandlingsRepository.findByIdOrThrow(behandlingId).fagsakId
            kabalService.sendTilKabal(behandlingId, fagsakId)
        }
    }

    fun hentAktivIdent(behandlingId: UUID): String {
        val behandling = hentBehandling(behandlingId)

        return fagsakService.hentFagsak(behandling.fagsakId).hentAktivIdent()
    }
}
