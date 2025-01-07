package no.nav.familie.klage.distribusjon

import no.nav.familie.klage.brev.baks.mottaker.Brevmottaker
import no.nav.familie.klage.brev.baks.mottaker.Mottakertype
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker

data class JournalpostBrevmottaker(
    val navn: String,
    val adresse: Adresse?,
    val mottakertype: Mottakertype,
) {
    companion object {
        fun opprett(navn: String, mottakertype: Mottakertype): JournalpostBrevmottaker {
            return JournalpostBrevmottaker(
                navn,
                null,
                mottakertype,
            )
        }

        fun opprett(brevmottaker: Brevmottaker): JournalpostBrevmottaker {
            return JournalpostBrevmottaker(
                brevmottaker.navn,
                Adresse.opprett(brevmottaker),
                brevmottaker.mottakertype,
            )
        }
    }

    data class Adresse(
        val adresselinje1: String,
        val adresselinje2: String? = null,
        val postnummer: String?,
        val poststed: String?,
        val landkode: String,
    ) {
        companion object {
            fun opprett(brevmottaker: Brevmottaker): Adresse {
                return Adresse(
                    adresselinje1 = brevmottaker.adresselinje1,
                    adresselinje2 = brevmottaker.adresselinje2,
                    postnummer = brevmottaker.postnummer,
                    poststed = brevmottaker.poststed,
                    landkode = brevmottaker.landkode,
                )
            }
        }
    }
}

fun JournalpostBrevmottaker.mapTilAvsenderMottaker(): AvsenderMottaker? {
    // TODO : Må man håndtere Institusjon?
    if (mottakertype == Mottakertype.BRUKER || mottakertype == Mottakertype.BRUKER_MED_UTENLANDSK_ADRESSE) {
        return null
    }
    return AvsenderMottaker(
        id = null,
        idType = null,
        navn = this.navn,
    )
}
