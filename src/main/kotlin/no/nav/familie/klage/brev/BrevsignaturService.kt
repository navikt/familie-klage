package no.nav.familie.klage.brev

import no.nav.familie.klage.brev.dto.SignaturDto
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BrevsignaturService(
) {
    fun lagSignatur(behandlingId: UUID): SignaturDto {
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
