package no.nav.familie.klage.distribusjon

import no.nav.familie.klage.brev.domain.Brevmottaker
import no.nav.familie.klage.brev.domain.BrevmottakerJournalpostMedIdent
import no.nav.familie.klage.brev.domain.BrevmottakerJournalpostUtenIdent
import no.nav.familie.klage.brev.domain.BrevmottakerOrganisasjon
import no.nav.familie.klage.brev.domain.BrevmottakerPersonMedIdent
import no.nav.familie.klage.brev.domain.BrevmottakerPersonUtenIdent
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottakerIdType

object JournalføringUtil {
    fun mapAvsenderMottaker(brevmottaker: Brevmottaker): AvsenderMottaker =
        when (brevmottaker) {
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

    fun mapBrevmottakerJournalpost(
        brevmottaker: Brevmottaker,
        avsenderMottaker: AvsenderMottaker,
        journalpostId: String,
    ) = when (brevmottaker) {
        is BrevmottakerPersonMedIdent,
        is BrevmottakerOrganisasjon,
        -> BrevmottakerJournalpostMedIdent(
            ident = avsenderMottaker.id ?: error("Mangler id for mottaker=$avsenderMottaker"),
            journalpostId = journalpostId,
        )

        is BrevmottakerPersonUtenIdent,
        -> BrevmottakerJournalpostUtenIdent(
            id = brevmottaker.id,
            journalpostId = journalpostId,
        )
    }
}
