package no.nav.familie.klage.brev.baks

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.brev.felles.BrevClient
import no.nav.familie.klage.brev.felles.BrevsignaturService
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import org.springframework.stereotype.Component

@Component
class FritekstbrevHtmlUtleder(
    private val brevClient: BrevClient,
    private val brevsignaturService: BrevsignaturService,
    private val fagsakService: FagsakService,
    private val personopplysningerService: PersonopplysningerService,
    private val fritekstBrevRequestDtoUtleder: FritekstBrevRequestDtoUtleder,
) {
    fun utledFritekstbrevHtml(behandling: Behandling): String {
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
        val personopplysninger = personopplysningerService.hentPersonopplysninger(behandling.id)

        val fritekstBrevRequestDto = fritekstBrevRequestDtoUtleder.utled(
            fagsak,
            behandling,
            personopplysninger.navn,
        )

        val signaturDto = brevsignaturService.lagSignatur(
            personopplysninger,
            fagsak.fagsystem,
        )

        return brevClient.genererHtmlFritekstbrev(
            fritekstBrev = fritekstBrevRequestDto,
            saksbehandlerNavn = signaturDto.navn,
            enhet = signaturDto.enhet,
        )
    }
}
