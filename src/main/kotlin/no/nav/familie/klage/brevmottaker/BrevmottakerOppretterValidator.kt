package no.nav.familie.klage.brevmottaker

import no.nav.familie.klage.brevmottaker.domain.Brevmottaker
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle.BRUKER
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle.DØDSBO
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle.FULLMAKT
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle.INSTITUSJON
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle.VERGE
import no.nav.familie.klage.brevmottaker.domain.NyBrevmottakerOrganisasjon
import no.nav.familie.klage.brevmottaker.domain.NyBrevmottakerPersonUtenIdent
import no.nav.familie.klage.infrastruktur.exception.Feil
import java.util.UUID

object BrevmottakerOppretterValidator {
    fun validerNyBrevmottakerPersonUtenIdent(
        brukerensNavn: String,
        behandlingId: UUID,
        nyBrevmottakerPersonUtenIdent: NyBrevmottakerPersonUtenIdent,
        eksisterendeBrevmottakere: List<Brevmottaker>,
    ) {
        val manueltOpprettedeBrevmottakere = eksisterendeBrevmottakere.filter { it.mottakerRolle !in setOf(BRUKER, INSTITUSJON) }
        val eksisterendeMottakerRoller = manueltOpprettedeBrevmottakere.mapNotNull { it.mottakerRolle }
        when {
            eksisterendeMottakerRoller.any { it == nyBrevmottakerPersonUtenIdent.mottakerRolle } -> {
                throw Feil(
                    "Kan ikke ha duplikate MottakerRolle. ${nyBrevmottakerPersonUtenIdent.mottakerRolle} finnes allerede for $behandlingId.",
                )
            }

            nyBrevmottakerPersonUtenIdent.mottakerRolle == BRUKER_MED_UTENLANDSK_ADRESSE &&
                nyBrevmottakerPersonUtenIdent.navn != brukerensNavn -> {
                throw Feil("Ved bruker med utenlandsk adresse skal brevmottakerens navn være brukerens navn for $behandlingId.")
            }

            nyBrevmottakerPersonUtenIdent.mottakerRolle == DØDSBO &&
                !nyBrevmottakerPersonUtenIdent.navn.contains(brukerensNavn) -> {
                throw Feil("Ved dødsbo skal brevmottakerens navn inneholde brukerens navn for $behandlingId.")
            }

            nyBrevmottakerPersonUtenIdent.mottakerRolle == DØDSBO &&
                manueltOpprettedeBrevmottakere.isNotEmpty() -> {
                throw Feil("Kan ikke legge til dødsbo når det allerede finnes brevmottakere for $behandlingId.")
            }

            eksisterendeMottakerRoller.any { it == DØDSBO } -> {
                throw Feil("Kan ikke legge til flere brevmottakere når det allerede finnes et dødsbo for $behandlingId.")
            }

            BRUKER_MED_UTENLANDSK_ADRESSE in eksisterendeMottakerRoller &&
                nyBrevmottakerPersonUtenIdent.mottakerRolle !== VERGE &&
                nyBrevmottakerPersonUtenIdent.mottakerRolle !== FULLMAKT -> {
                throw Feil("Bruker med utenlandsk adresse kan kun kombineres med verge eller fullmektig for $behandlingId.")
            }

            eksisterendeMottakerRoller.isNotEmpty() &&
                BRUKER_MED_UTENLANDSK_ADRESSE !in eksisterendeMottakerRoller &&
                nyBrevmottakerPersonUtenIdent.mottakerRolle !== BRUKER_MED_UTENLANDSK_ADRESSE -> {
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

            nyBrevmottakerOrganisasjon.mottakerRolle != FULLMAKT -> {
                throw Feil("Organisasjon kan kun ha mottakerrolle fullmakt for $behandlingId.")
            }

            eksisterendeMottakerRoller.any { it == INSTITUSJON } && eksisterendeBrevmottakere.size > 1 -> {
                throw Feil("Kan kun ha én ekstra brevmottaker når institusjon er brevmottaker for $behandlingId.")
            }
        }
    }
}
