package no.nav.familie.klage.brev

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.domain.erLåstForVidereBehandling
import no.nav.familie.klage.brev.domain.Avsnitt
import no.nav.familie.klage.brev.domain.Brev
import no.nav.familie.klage.brev.dto.AvsnittDto
import no.nav.familie.klage.brev.dto.BrevMedAvsnittDto
import no.nav.familie.klage.brev.dto.FritekstBrevDto
import no.nav.familie.klage.brev.dto.FritekstBrevRequestDto
import no.nav.familie.klage.brev.dto.FritekstBrevtype
import no.nav.familie.klage.brev.dto.tilDto
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.felles.domain.Fil
import no.nav.familie.klage.infrastruktur.exception.feilHvis
import no.nav.familie.klage.repository.findByIdOrThrow
import org.springframework.data.repository.findByIdOrNull
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

    fun hentMellomlagretBrev(behandlingId: UUID): BrevMedAvsnittDto? {
        feilHvis(behandlingService.erLåstForVidereBehandling(behandlingId)) {
            "Kan ikke hente mellomlagret brev når behandlingen er låst"
        }
        return brevRepository.findByIdOrNull(behandlingId)?.let {
            val avsnitt = avsnittRepository.findByBehandlingId(behandlingId)
            BrevMedAvsnittDto(behandlingId, it.overskrift, avsnitt.map { it.tilDto() })
        }
    }

    @Transactional
    fun lagEllerOppdaterBrev(fritekstbrevDto: FritekstBrevDto): ByteArray {
        val navn = behandlingService.hentNavnFraBehandlingsId(fritekstbrevDto.behandlingId)
        val behandling = behandlingService.hentBehandling(fritekstbrevDto.behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
        feilHvis(behandling.status.erLåstForVidereBehandling()) {
            "Kan ikke oppdatere brev når behandlingen er låst"
        }

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

    fun hentBrevPdf(behandlingId: UUID): ByteArray {
        return brevRepository.findByIdOrThrow(behandlingId).pdf?.bytes
            ?: error("Finner ikke brev-pdf for behandling=$behandlingId")
    }

    private fun lagEllerOppdaterBrev(
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
        return avsnittRepository.insert(
            Avsnitt(
                behandlingId = behandlingId,
                deloverskrift = avsnitt.deloverskrift,
                innhold = avsnitt.innhold,
                skalSkjulesIBrevbygger = avsnitt.skalSkjulesIBrevbygger
            )
        )
    }

    private fun slettAvsnittOmEksisterer(behandlingId: UUID) {
        avsnittRepository.slettAvsnittMedBehandlingId(behandlingId)
    }

    fun lagBrevSomPdf(behandlingId: UUID): ByteArray {
        val brev = brevRepository.findByIdOrThrow(behandlingId)
        feilHvis(behandlingService.erLåstForVidereBehandling(behandlingId)) {
            "Kan ikke lage pdf når behandlingen er låst"
        }
        feilHvis(brev.pdf != null) {
            "Det finnes allerede en lagret pdf"
        }

        val generertBrev = familieDokumentClient.genererPdfFraHtml(brev.saksbehandlerHtml)
        brevRepository.update(brev.copy(pdf = Fil(generertBrev)))
        return generertBrev
    }
}
