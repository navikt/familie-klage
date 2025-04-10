package no.nav.familie.klage.behandling.enhet

import no.nav.familie.klage.infrastruktur.exception.ApiFeil
import no.nav.familie.klage.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import org.springframework.http.HttpStatus

object EnhetValidator {

    fun validerEnhetForFagsystem(enhetsnummer: String, fagsystem: Fagsystem) {
        when (fagsystem) {
            Fagsystem.BA -> brukerfeilHvis(!BarnetrygdEnhet.erGyldigBehandlendeBarnetrygdEnhet(enhetsnummer = enhetsnummer)) {
                "Kan ikke oppdatere behandlende enhet til $enhetsnummer. Dette er ikke et gyldig enhetsnummer for barnetrygd."
            }
            Fagsystem.KS -> brukerfeilHvis(!KontantstøtteEnhet.erGyldigBehandlendeKontantstøtteEnhet(enhetsnummer = enhetsnummer)) {
                "Kan ikke oppdatere behandlende enhet til $enhetsnummer. Dette er ikke et gyldig enhetsnummer for kontantstøtte."
            }
            else -> throw ApiFeil("Støtter ikke oppdatering av behandlende enhet for fagsystem $fagsystem", HttpStatus.BAD_REQUEST)
        }
    }
}