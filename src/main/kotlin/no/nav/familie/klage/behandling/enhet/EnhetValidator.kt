package no.nav.familie.klage.behandling.enhet

import no.nav.familie.klage.infrastruktur.exception.ApiFeil
import no.nav.familie.kontrakter.felles.klage.Fagsystem

object EnhetValidator {
    fun validerEnhetForFagsystem(
        enhetsnummer: String,
        fagsystem: Fagsystem,
    ) {
        when (fagsystem) {
            Fagsystem.BA ->
                if (!BarnetrygdEnhet.erGyldigBehandlendeBarnetrygdEnhet(enhetsnummer)) {
                    throw ApiFeil.badRequest("Kan ikke oppdatere behandlende enhet til $enhetsnummer. Dette er ikke et gyldig enhetsnummer for barnetrygd.")
                }

            Fagsystem.KS ->
                if (!KontantstøtteEnhet.erGyldigBehandlendeKontantstøtteEnhet(enhetsnummer)) {
                    throw ApiFeil.badRequest("Kan ikke oppdatere behandlende enhet til $enhetsnummer. Dette er ikke et gyldig enhetsnummer for kontantstøtte.")
                }

            else -> throw ApiFeil.badRequest("Støtter ikke endring av enhet for fagsystem $fagsystem")
        }
    }
}
