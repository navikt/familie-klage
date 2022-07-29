package no.nav.familie.klage.brev

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.brev.domain.Brev
import no.nav.familie.klage.brev.domain.BrevMedAvsnitt
import no.nav.familie.klage.brev.dto.Avsnitt
import no.nav.familie.klage.brev.dto.FritekstBrevDto
import no.nav.familie.klage.brev.dto.FritekstBrevRequestDto
import no.nav.familie.klage.brev.dto.FritekstBrevtype
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.repository.findByIdOrThrow
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
){
    fun hentMellomlagretBrev(behandlingId: UUID): BrevMedAvsnitt? {
        val brev = brevRepository.findByIdOrThrow(behandlingId)
        val avsnitt = avsnittRepository.hentAvsnittPåBehandlingId(behandlingId)
        return BrevMedAvsnitt(behandlingId, brev.overskrift, avsnitt)
    }

    @Transactional
    fun lagBrev(fritekstbrevDto: FritekstBrevDto): ByteArray{
        val navn = behandlingService.hentNavnFraBehandlingsId(fritekstbrevDto.behandlingId)
        val behandling = behandlingService.hentBehandling(fritekstbrevDto.behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)

        slettAvsnittOmEksisterer(fritekstbrevDto.behandlingId)

        val request = FritekstBrevRequestDto(
            overskrift = fritekstbrevDto.overskrift,
            avsnitt = fritekstbrevDto.avsnitt,
            personIdent = fagsak.personIdent,
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

        for (avsnitt in fritekstbrevDto.avsnitt){
            lagEllerOppdaterAvsnitt(
                avsnittId = avsnitt.avsnittId,
                behandlingId = fritekstbrevDto.behandlingId,
                deloverskrift = avsnitt.deloverskrift,
                innhold = avsnitt.innhold,
                skalSkjulesIBrevBygger = avsnitt.skalSkjulesIBrevbygger)
        }

        return familieDokumentClient.genererPdfFraHtml(html)
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

        return when(brevRepository.existsById(brev.behandlingId)){
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
            deloverskrift =deloverskrift,
            innhold = innhold,
            skalSkjulesIBrevbygger = skalSkjulesIBrevBygger,
        )
        return when(avsnittRepository.existsById(avsnittId)){
            true -> avsnittRepository.update(avsnitt)
            false -> avsnittRepository.insert(avsnitt)
        }
    }

    fun slettAvsnittOmEksisterer(behandlingId: UUID){
        avsnittRepository.slettAvsnittMedBehanldingId(behandlingId)
    }
}
