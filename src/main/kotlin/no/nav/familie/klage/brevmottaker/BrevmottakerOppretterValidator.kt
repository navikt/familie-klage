package no.nav.familie.klage.brevmottaker

import no.nav.familie.klage.brevmottaker.domain.Brevmottaker
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonUtenIdent
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle
import no.nav.familie.klage.brevmottaker.domain.NyBrevmottakerOrganisasjon
import no.nav.familie.klage.brevmottaker.domain.NyBrevmottakerPersonUtenIdent
import no.nav.familie.klage.infrastruktur.exception.Feil
import java.util.UUID

object BrevmottakerOppretterValidator {
    fun validerNyBrevmottakerPersonUtenIdent(
        brukerensNavn: String,
        behandlingId: UUID,
        nyBrevmottakerPersonUtenIdent: NyBrevmottakerPersonUtenIdent,
        eksisterendeBrevmottakerePersonerUtenIdent: List<BrevmottakerPersonUtenIdent>,
    ) {
        val eksisterendeMottakerRoller = eksisterendeBrevmottakerePersonerUtenIdent.map { it.mottakerRolle }
        when {
            eksisterendeMottakerRoller.any { it == nyBrevmottakerPersonUtenIdent.mottakerRolle } -> {
                throw Feil(
                    "Kan ikke ha duplikate MottakerRolle. ${nyBrevmottakerPersonUtenIdent.mottakerRolle} finnes allerede for $behandlingId.",
                )
            }

            nyBrevmottakerPersonUtenIdent.mottakerRolle == MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE &&
                nyBrevmottakerPersonUtenIdent.navn != brukerensNavn -> {
                throw Feil("Ved bruker med utenlandsk adresse skal brevmottakerens navn være brukerens navn for $behandlingId.")
            }

            nyBrevmottakerPersonUtenIdent.mottakerRolle == MottakerRolle.DØDSBO &&
                !nyBrevmottakerPersonUtenIdent.navn.contains(brukerensNavn) -> {
                throw Feil("Ved dødsbo skal brevmottakerens navn inneholde brukerens navn for $behandlingId.")
            }

            nyBrevmottakerPersonUtenIdent.mottakerRolle == MottakerRolle.DØDSBO &&
                eksisterendeBrevmottakerePersonerUtenIdent.isNotEmpty() -> {
                throw Feil("Kan ikke legge til dødsbo når det allerede finnes brevmottakere for $behandlingId.")
            }

            eksisterendeMottakerRoller.any { it == MottakerRolle.DØDSBO } -> {
                throw Feil("Kan ikke legge til flere brevmottakere når det allerede finnes et dødsbo for $behandlingId.")
            }

            MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE in eksisterendeMottakerRoller &&
                nyBrevmottakerPersonUtenIdent.mottakerRolle !== MottakerRolle.VERGE &&
                nyBrevmottakerPersonUtenIdent.mottakerRolle !== MottakerRolle.FULLMAKT -> {
                throw Feil("Bruker med utenlandsk adresse kan kun kombineres med verge eller fullmektig for $behandlingId.")
            }

            eksisterendeMottakerRoller.isNotEmpty() &&
                MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE !in eksisterendeMottakerRoller &&
                nyBrevmottakerPersonUtenIdent.mottakerRolle !== MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE -> {
                throw Feil("Kan kun legge til bruker med utenlandsk adresse om det finnes en brevmottaker allerede for $behandlingId.")
            }
        }
    }

    fun validerNyBrevmottakerOrganisasjon(
        behandlingId: UUID,
        nyBrevmottakerOrganisasjon: NyBrevmottakerOrganisasjon,
        eksisterendeBrevmottakere: List<Brevmottaker>,
    ) {
        val eksisterendeMottakerRoller = eksisterendeBrevmottakere.map { it.mottakerRolle }
        when {
            eksisterendeMottakerRoller.any { it == nyBrevmottakerOrganisasjon.mottakerRolle } -> {
                throw Feil(
                    "Kan ikke ha duplikate MottakerRolle. ${nyBrevmottakerOrganisasjon.mottakerRolle} finnes allerede for $behandlingId.",
                )
            }

            nyBrevmottakerOrganisasjon.mottakerRolle != MottakerRolle.FULLMAKT -> {
                throw Feil("Organisasjon kan kun ha mottakerrolle fullmakt for $behandlingId.")
            }

            eksisterendeMottakerRoller.any { it == MottakerRolle.INSTITUSJON } && eksisterendeBrevmottakere.size > 1 -> {
                throw Feil("Kan kun ha én ekstra brevmottaker når institusjon er brevmottaker for $behandlingId.")
            }
        }
    }
}
