package no.nav.familie.klage.brev

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.brev.domain.Brev
import no.nav.familie.klage.brev.dto.FritekstBrevDto
import no.nav.familie.klage.brev.dto.FritekstBrevRequestDto
import no.nav.familie.klage.brev.dto.FrittståendeBrevAvsnitt
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.UUID
@Service
class BrevService(
    private val brevClient: BrevClient,
    private val brevRepository: BrevRepository,
    private val familieDokumentClient: FamilieDokumentClient
){
    fun hentBrev(behandlingId: UUID): Brev? {
        return brevRepository.findByIdOrNull(behandlingId)
    }

    fun lagBrev(fritekstbrevDto: FritekstBrevDto, behandling: Behandling): ByteArray{
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
            overskrift = "Overskrift",
            avsnitt = listOf(FrittståendeBrevAvsnitt("deloverskrift", "innhold"))
        )
        brevRepository.insert(brev)

        return familieDokumentClient.genererPdfFraHtml(html)

    }
}