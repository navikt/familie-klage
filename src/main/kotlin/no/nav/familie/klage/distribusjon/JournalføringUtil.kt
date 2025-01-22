package no.nav.familie.klage.distribusjon

import no.nav.familie.klage.brevmottaker.domain.Brevmottaker
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerOrganisasjon
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonMedIdent
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonUtenIdent
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottakerIdType

object JournalføringUtil {
    fun mapAvsenderMottaker(brevmottaker: Brevmottaker): AvsenderMottaker {
        return when (brevmottaker) {
            is BrevmottakerOrganisasjon -> AvsenderMottaker(
                id = brevmottaker.organisasjonsnummer,
                navn = brevmottaker.navnHosOrganisasjon,
                idType = AvsenderMottakerIdType.ORGNR,
            )

            is BrevmottakerPersonMedIdent -> AvsenderMottaker(
                id = brevmottaker.personIdent,
                navn = brevmottaker.navn,
                idType = AvsenderMottakerIdType.FNR,
            )

            is BrevmottakerPersonUtenIdent -> AvsenderMottaker(
                id = null,
                navn = brevmottaker.navn,
                idType = AvsenderMottakerIdType.NULL,
            )
        }
    }
}
