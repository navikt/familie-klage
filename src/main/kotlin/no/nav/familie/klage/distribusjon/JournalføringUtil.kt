package no.nav.familie.klage.distribusjon

import no.nav.familie.klage.brev.domain.Brevmottakere
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottakerIdType

object JournalføringUtil {

    fun mapAvsenderMottaker(brevmottakere: Brevmottakere): List<AvsenderMottaker> {
        return brevmottakere.let { mottakere ->
            mottakere.personer.map {
                AvsenderMottaker(
                    id = it.personIdent,
                    navn = it.navn,
                    idType = AvsenderMottakerIdType.FNR,
                )
            } + mottakere.organisasjoner.map {
                AvsenderMottaker(
                    id = it.organisasjonsnummer,
                    navn = it.navnHosOrganisasjon,
                    idType = AvsenderMottakerIdType.ORGNR,
                )
            }
        }
    }
}
