package no.nav.familie.klage.brev

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.brev.domain.Brev
import no.nav.familie.klage.brev.domain.BrevMedAvsnitt
import no.nav.familie.klage.brev.dto.Avsnitt
import no.nav.familie.klage.brev.dto.FritekstBrevDto
import no.nav.familie.klage.brev.dto.FritekstBrevRequestDto
import no.nav.familie.klage.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import java.util.UUID
@Service
class BrevService(
    private val brevClient: BrevClient,
    private val brevRepository: BrevRepository,
    private val avsnittRepository: AvsnittRepository,
    private val behandlingService: BehandlingService,
    private val familieDokumentClient: FamilieDokumentClient,
    private val brevsignaturService: BrevsignaturService
){


    fun hentBrev(behandlingId: UUID): BrevMedAvsnitt? {
        val brev =  brevRepository.findByIdOrThrow(behandlingId)
        val avsnitt = avsnittRepository.findByIdOrThrow(brev.brevId)
        return BrevMedAvsnitt(brev.behandlingId, brev.brevId, brev.overskrift, listOf(avsnitt))
    }

    fun lagBrev(fritekstbrevDto: FritekstBrevDto): ByteArray{
        val navn = behandlingService.hentNavnFraBehandlingsId(fritekstbrevDto.behandlingId)
        val personIdent = brevRepository.findPersonIdByBehandlingId(fritekstbrevDto.behandlingId)
        val behandling = behandlingService.hentBehandling(fritekstbrevDto.behandlingId)

        val request = FritekstBrevRequestDto(
            overskrift = fritekstbrevDto.overskrift,
            avsnitt = fritekstbrevDto.avsnitt,
            personIdent = personIdent,
            navn = navn
        )

        val signaturMedEnhet = brevsignaturService.lagSignatur(behandling)

        val html = brevClient.genererHtmlFritekstbrev(
            fritekstBrev = request,
            saksbehandlerNavn = signaturMedEnhet.navn,
            enhet = signaturMedEnhet.enhet
        )
        val brev =
            Brev(
            behandlingId = fritekstbrevDto.behandlingId,
            brevId = UUID.randomUUID(),
            overskrift = "Overskrift",
            saksbehandlerHtml = html
        )
        brevRepository.insert(brev)

        for (avsnitt in fritekstbrevDto.avsnitt){
            val a = Avsnitt(
                avsnittId = UUID.randomUUID(),
                deloverskrift = avsnitt.deloverskrift,
                innhold = avsnitt.innhold,
                skalSkjulesIBrevbygger = avsnitt.skalSkjulesIBrevbygger,
                brevId = brev.brevId
            )
            avsnittRepository.insert(a)
        }
        return familieDokumentClient.genererPdfFraHtml(html)

    }
}
