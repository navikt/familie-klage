package no.nav.familie.klage.henlegg

import no.nav.familie.klage.brevmottaker.domain.MottakerRolle
import no.nav.familie.klage.brevmottaker.dto.NyBrevmottakerDto
import no.nav.familie.klage.brevmottaker.dto.NyBrevmottakerPersonDto
import no.nav.familie.klage.brevmottaker.dto.NyBrevmottakerPersonMedIdentDto
import no.nav.familie.klage.infrastruktur.exception.ApiFeil
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.kontrakter.felles.klage.HenlagtÅrsak

data class HenleggBehandlingDto(
    val årsak: HenlagtÅrsak,
    val skalSendeHenleggelsesbrev: Boolean = false,
    val nyeBrevmottakere: List<NyBrevmottakerPersonDto> = emptyList(),
) {
    fun valider() {
        if (årsak == HenlagtÅrsak.FEILREGISTRERT && skalSendeHenleggelsesbrev) {
            throw ApiFeil.badRequest("Kan ikke sende henleggelsesbrev når årsak er ${HenlagtÅrsak.FEILREGISTRERT}.")
        }

        if (skalSendeHenleggelsesbrev && nyeBrevmottakere.isEmpty()) {
            throw ApiFeil.badRequest("Forventer minst en brevmottaker.")
        }

        if (nyeBrevmottakere.size > 2) {
            throw ApiFeil.badRequest("Forventer ikke mer enn 2 brevmottakere.")
        }

        if (nyeBrevmottakere.distinct().size != nyeBrevmottakere.size) {
            throw ApiFeil.badRequest("Forventer ingen duplikate mottaker roller.")
        }

        val mottakerRoller = nyeBrevmottakere.map { it.mottakerRolle }
        if (mottakerRoller.containsAll(setOf(MottakerRolle.BRUKER, MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE))) {
            throw ApiFeil.badRequest("${MottakerRolle.BRUKER} kan ikke kombineres med ${MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE}.")
        }

        if (mottakerRoller.contains(MottakerRolle.DØDSBO) && mottakerRoller.size > 1) {
            throw ApiFeil.badRequest("${MottakerRolle.DØDSBO} kan ikke kombineres med flere mottaker roller.")
        }

        nyeBrevmottakere.forEach(NyBrevmottakerDto::valider)
    }

    fun finnNyBrevmottakerBruker(): NyBrevmottakerPersonMedIdentDto? {
        val brevmottakerBruker =
            nyeBrevmottakere
                .filterIsInstance<NyBrevmottakerPersonMedIdentDto>()
                .filter { it.mottakerRolle == MottakerRolle.BRUKER }
        if (brevmottakerBruker.size > 1) {
            throw Feil("Forventer ikke mer enn 1 brevmottaker med mottaker rolle ${MottakerRolle.BRUKER}.")
        }
        return brevmottakerBruker.singleOrNull()
    }
}
