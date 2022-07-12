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
    private val familieDokumentClient: FamilieDokumentClient
){
    fun hentBrev(behandlingId: UUID): BrevMedAvsnitt? {
        val brev =  brevRepository.findByIdOrThrow(behandlingId)
        val avsnitt = avsnittRepository.findByIdOrThrow(brev.brevId)
        return BrevMedAvsnitt(brev.behandlingId, brev.brevId, brev.overskrift, listOf(avsnitt))
    }

    fun lagBrev(fritekstbrevDto: FritekstBrevDto): ByteArray{
        //val navn = personopplysningerService.hentGjeldeneNavn(listOf(saksbehandling.ident)).getValue(saksbehandling.ident)

        val request = FritekstBrevRequestDto(
            overskrift = fritekstbrevDto.overskrift,
            avsnitt = fritekstbrevDto.avsnitt,
            personIdent = "ident",//behandling.ident
            navn = "navn"
        )

        //val signaturMedEnhet = brevSignaturService.lagSignaturMedEnhet(saksbehandling)

        val html = brevClient.genererHtmlFritekstbrev(
            fritekstBrev = request,
            saksbehandlerNavn = "Maja", //TODO legge til ekte verdier
            enhet = "min enhet"

        )
        val brev = Brev(
            behandlingId = fritekstbrevDto.behandlingId,
            brevId = UUID.randomUUID(),
            overskrift = "Overskrift",
        )
        println(brev)
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