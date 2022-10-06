package no.nav.familie.klage.brev

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.brev.domain.Avsnitt
import no.nav.familie.klage.brev.domain.Brev
import no.nav.familie.klage.brev.dto.AvsnittDto
import no.nav.familie.klage.brev.dto.BrevMedAvsnittDto
import no.nav.familie.klage.brev.dto.FritekstBrevDto
import no.nav.familie.klage.brev.dto.FritekstBrevRequestDto
import no.nav.familie.klage.brev.dto.FritekstBrevtype
import no.nav.familie.klage.brev.dto.tilDto
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.repository.findByIdOrThrow
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
    private val fagsakService: FagsakService
) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun hentMellomlagretBrev(behandlingId: UUID): BrevMedAvsnittDto? {
        val eksisterer = sjekkOmBrevEksisterer(behandlingId)
        if (eksisterer) {
            val brev = brevRepository.findByIdOrThrow(behandlingId)
            val avsnitt = avsnittRepository.findByBehandlingId(behandlingId)
            return BrevMedAvsnittDto(behandlingId, brev.overskrift, avsnitt.map { it.tilDto() })
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

        fritekstbrevDto.avsnitt.forEach {
            lagreAvsnitt(behandlingId = fritekstbrevDto.behandlingId, avsnitt = it)
        }

        return familieDokumentClient.genererPdfFraHtml(html)
    }

    fun lagEllerOppdaterBrev(
        behandlingId: UUID,
        overskrift: String,
        saksbehandlerHtml: String,
        brevtype: FritekstBrevtype
    ): Brev {
        val brev = Brev(
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

    private fun lagreAvsnitt(behandlingId: UUID, avsnitt: AvsnittDto): Avsnitt {
        return avsnittRepository.insert(Avsnitt(
            behandlingId = behandlingId,
            deloverskrift = avsnitt.deloverskrift,
            innhold = avsnitt.innhold,
            skalSkjulesIBrevbygger = avsnitt.skalSkjulesIBrevbygger
        ))
    }

    fun slettAvsnittOmEksisterer(behandlingId: UUID) {
        avsnittRepository.slettAvsnittMedBehandlingId(behandlingId)
    }

    fun lagBrevSomPdf(behandlingId: UUID): ByteArray {
        val brev = brevRepository.findByIdOrThrow(behandlingId)
        return familieDokumentClient.genererPdfFraHtml(brev.saksbehandlerHtml)
    }
}
