package no.nav.familie.klage.brev

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.brev.dto.SignaturDto
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import org.springframework.stereotype.Service

@Service
class BrevsignaturService(
    val personopplysningerService: PersonopplysningerService
) {
    fun lagSignatur(behandling: Behandling): SignaturDto {
        return lagSignaturDto("ident") //TODO: endre til ekte data
        //return lagSignaturDto(behandling.ident)
    }

    private fun lagSignaturDto(ident: String): SignaturDto{
        return SignaturDto(SikkerhetContext.hentSaksbehandlerNavn(true), ENHET_NAY, false)
    }

    companion object {
        val NAV_ANONYM_NAVN = "NAV anonym"
        val ENHET_VIKAFOSSEN = "NAV Vikafossen"
        val ENHET_NAY = "NAV Arbeid og ytelser"
    }
}
