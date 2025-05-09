package no.nav.familie.klage.distribusjon

import no.nav.familie.klage.brevmottaker.domain.Brevmottaker
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerOrganisasjon
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonMedIdent
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonUtenIdent
import no.nav.familie.klage.brevmottaker.domain.Brevmottakere
import no.nav.familie.klage.distribusjon.domain.BrevmottakerJournalpostMedIdent
import no.nav.familie.klage.distribusjon.domain.BrevmottakerJournalpostUtenIdent
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottakerIdType

object JournalføringUtil {
    fun mapAvsenderMottaker(brevmottakere: Brevmottakere): List<AvsenderMottaker> =
        brevmottakere.let { mottakere ->
            val personer =
                mottakere.personer.map {
                    when (it) {
                        is BrevmottakerPersonMedIdent ->
                            AvsenderMottaker(
                                id = it.personIdent,
                                navn = it.navn,
                                idType = AvsenderMottakerIdType.FNR,
                            )

                        is BrevmottakerPersonUtenIdent -> throw IllegalStateException(
                            "BrevmottakerPersonUtenIdent er foreløpig ikke støttet.",
                        )
                    }
                }
            val organisasjoner =
                mottakere.organisasjoner.map {
                    AvsenderMottaker(
                        id = it.organisasjonsnummer,
                        navn = it.navnHosOrganisasjon,
                        idType = AvsenderMottakerIdType.ORGNR,
                    )
                }
            personer + organisasjoner
        }

    fun mapAvsenderMottaker(brevmottaker: Brevmottaker): AvsenderMottaker =
        when (brevmottaker) {
            is BrevmottakerOrganisasjon ->
                AvsenderMottaker(
                    id = brevmottaker.organisasjonsnummer,
                    navn = brevmottaker.navnHosOrganisasjon,
                    idType = AvsenderMottakerIdType.ORGNR,
                )

            is BrevmottakerPersonMedIdent ->
                AvsenderMottaker(
                    id = brevmottaker.personIdent,
                    navn = brevmottaker.navn,
                    idType = AvsenderMottakerIdType.FNR,
                )

            is BrevmottakerPersonUtenIdent ->
                AvsenderMottaker(
                    id = null,
                    navn = brevmottaker.navn,
                    idType = null,
                )
        }

    fun mapBrevmottakerJournalpost(
        brevmottaker: Brevmottaker,
        avsenderMottaker: AvsenderMottaker,
        journalpostId: String,
    ) = when (brevmottaker) {
        is BrevmottakerPersonMedIdent,
        is BrevmottakerOrganisasjon,
        ->
            BrevmottakerJournalpostMedIdent(
                ident = avsenderMottaker.id ?: error("Mangler id for mottaker=$avsenderMottaker"),
                journalpostId = journalpostId,
            )

        is BrevmottakerPersonUtenIdent,
        ->
            BrevmottakerJournalpostUtenIdent(
                id = brevmottaker.id,
                journalpostId = journalpostId,
            )
    }
}
