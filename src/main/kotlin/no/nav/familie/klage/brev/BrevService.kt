package no.nav.familie.klage.brev

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
    private val familieDokumentClient: FamilieDokumentClient,
){


    fun hentBrev(behandlingId: UUID): BrevMedAvsnitt? {
        val brev =  brevRepository.findByIdOrThrow(behandlingId)
        val avsnitt = avsnittRepository.findByIdOrThrow(behandlingId)
        return BrevMedAvsnitt(brev.behandlingId, brev.overskrift, listOf(avsnitt))
    }

    fun lagBrev(fritekstbrevDto: FritekstBrevDto): ByteArray{
        //val navn = personopplysningerService.hentGjeldeneNavn(listOf(saksbehandling.ident)).getValue(saksbehandling.ident)
        val request = FritekstBrevRequestDto(
            overskrift = fritekstbrevDto.overskrift,
            avsnitt = fritekstbrevDto.avsnitt,
            personIdent = "en ident",//behandling.ident
            navn = "ett navn"
        )

        //val signaturMedEnhet = brevSignaturService.lagSignaturMedEnhet(saksbehandling)

        val html = brevClient.genererHtmlFritekstbrev(
            fritekstBrev = request,
            saksbehandlerNavn = "Maja", //TODO legge til ekte verdier
            enhet = "min enhet"
        )

        lagEllerOppdaterBrev(
            behandlingId = fritekstbrevDto.behandlingId,
            overskrift = fritekstbrevDto.overskrift,
            saksbehandlerHtml = html
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
        saksbehandlerHtml: String
    ): Brev {
        val brev =
            Brev(
                behandlingId = behandlingId,
                overskrift = overskrift,
                saksbehandlerHtml = saksbehandlerHtml,
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
}
