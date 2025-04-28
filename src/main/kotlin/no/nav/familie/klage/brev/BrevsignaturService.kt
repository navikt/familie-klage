package no.nav.familie.klage.brev

import no.nav.familie.klage.brev.dto.SignaturDto
import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.klage.infrastruktur.featuretoggle.Toggle
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.klage.personopplysninger.dto.PersonopplysningerDto
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import org.springframework.stereotype.Service

@Service
class BrevsignaturService(
    private val featureToggleService: FeatureToggleService,
) {
    fun lagSignatur(
        personopplysningerDto: PersonopplysningerDto,
        fagsystem: Fagsystem,
    ): SignaturDto {
        val harStrengtFortroligAdresse: Boolean = personopplysningerDto.adressebeskyttelse?.erStrengtFortrolig() ?: false

        if (harStrengtFortroligAdresse) {
            return SignaturDto(NAV_ANONYM_NAVN, ENHET_VIKAFOSSEN)
        }

        if (!featureToggleService.isEnabled(Toggle.VELG_SIGNATUR_BASERT_PÅ_FAGSAK)) {
            SignaturDto(SikkerhetContext.hentSaksbehandlerNavn(true), ENHET_NAY)
        }

        return when (fagsystem) {
            Fagsystem.EF -> {
                SignaturDto(SikkerhetContext.hentSaksbehandlerNavn(true), ENHET_NAY)
            }
            Fagsystem.BA, Fagsystem.KS -> {
                SignaturDto(SikkerhetContext.hentSaksbehandlerNavn(true), ENHET_NFP)
            }
        }
    }

    companion object {
        val NAV_ANONYM_NAVN = "Nav anonym"
        val ENHET_VIKAFOSSEN = "Nav Vikafossen"
        val ENHET_NAY = "Nav arbeid og ytelser"
        val ENHET_NFP = "Nav familie- og pensjonsytelser"
    }
}
