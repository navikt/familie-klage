package no.nav.familie.klage.distribusjon

import no.nav.familie.klage.brev.domain.BrevmottakerPersonMedIdent
import no.nav.familie.klage.brev.domain.BrevmottakerPersonUtenIdent
import no.nav.familie.klage.brev.domain.Brevmottakere
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottakerIdType

object JournalføringUtil {
    fun mapAvsenderMottaker(brevmottakere: Brevmottakere): List<AvsenderMottaker> {
        return brevmottakere.let { mottakere ->
            val personer = mottakere.personer.map {
                when (it) {
                    is BrevmottakerPersonMedIdent -> AvsenderMottaker(
                        id = it.personIdent,
                        navn = it.navn,
                        idType = AvsenderMottakerIdType.FNR,
                    )

                    is BrevmottakerPersonUtenIdent -> throw IllegalStateException("BrevmottakerPersonUtenIdent er foreløpig ikke støttet.")
                }
            }
            val organisasjoner = mottakere.organisasjoner.map {
                AvsenderMottaker(
                    id = it.organisasjonsnummer,
                    navn = it.navnHosOrganisasjon,
                    idType = AvsenderMottakerIdType.ORGNR,
                )
            }
            personer + organisasjoner
        }
    }
}
