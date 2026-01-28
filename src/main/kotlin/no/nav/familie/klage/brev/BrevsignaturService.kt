package no.nav.familie.klage.brev

import no.nav.familie.klage.brev.dto.SignaturDto
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.klage.oppgave.OppgaveClient
import no.nav.familie.klage.personopplysninger.dto.PersonopplysningerDto
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BrevsignaturService(
    private val oppgaveClient: OppgaveClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun lagSignatur(
        personopplysningerDto: PersonopplysningerDto,
        fagsystem: Fagsystem,
    ): SignaturDto {
        val harStrengtFortroligAdresse: Boolean = personopplysningerDto.adressebeskyttelse?.erStrengtFortrolig() ?: false

        if (harStrengtFortroligAdresse) {
            return SignaturDto(NAV_ANONYM_NAVN, ENHET_VIKAFOSSEN)
        }

        return when (fagsystem) {
            Fagsystem.EF -> {
                val saksbehandler = hentSaksbehandlerInfo(SikkerhetContext.hentSaksbehandler())
                val signaturEnhet = utledSignaturEnhet(saksbehandler.enhetsnavn)

                SignaturDto(SikkerhetContext.hentSaksbehandlerNavn(true), signaturEnhet)
            }

            Fagsystem.BA, Fagsystem.KS -> {
                SignaturDto(SikkerhetContext.hentSaksbehandlerNavn(true), ENHET_NFP)
            }
        }
    }

    private fun utledSignaturEnhet(enhetsnavn: String) =
        when (enhetsnavn) {
            "NAV ARBEID OG YTELSER SKIEN" -> "Nav arbeid og ytelser Skien"
            "NAV ARBEID OG YTELSER MØRE OG ROMSDAL" -> "Nav arbeid og ytelser Møre og Romsdal"
            "NAV ARBEID OG YTELSER SØRLANDET" -> "Nav arbeid og ytelser Sørlandet"
            else -> loggAdvarselOgReturnerEnhetsnavn(enhetsnavn)
        }

    private fun loggAdvarselOgReturnerEnhetsnavn(enhetsnavn: String): String {
        logger.warn("En saksbehandler med enhet $enhetsnavn har signert et brev. Vurder om vi må legge til dette enhetsnavnet for korrekt visning i brevsignaturen.")
        return ENHET_NAY
    }

    private fun hentSaksbehandlerInfo(navIdent: String) = oppgaveClient.hentSaksbehandlerInfo(navIdent)

    companion object {
        const val NAV_ANONYM_NAVN = "Nav anonym"
        const val ENHET_VIKAFOSSEN = "Nav Vikafossen"
        const val ENHET_NAY = "Nav arbeid og ytelser"
        const val ENHET_NFP = "Nav familie- og pensjonsytelser"
    }
}
