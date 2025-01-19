package no.nav.familie.klage.distribusjon.baks

import no.nav.familie.klage.brev.baks.brevmottaker.Brevmottaker
import no.nav.familie.klage.brev.baks.brevmottaker.Mottakertype
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker

data class DistribuerbarBrevmottaker(
    val navn: String,
    val adresse: Adresse?,
    val mottakertype: Mottakertype,
) {
    companion object {
        fun opprettForBruker(navn: String): DistribuerbarBrevmottaker {
            return DistribuerbarBrevmottaker(
                navn,
                null,
                Mottakertype.BRUKER,
            )
        }

        fun opprettForBrevmottaker(brevmottaker: Brevmottaker): DistribuerbarBrevmottaker {
            return DistribuerbarBrevmottaker(
                brevmottaker.navn,
                Adresse.opprettForBrevmottaker(brevmottaker),
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
            fun opprettForBrevmottaker(brevmottaker: Brevmottaker): Adresse {
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

fun DistribuerbarBrevmottaker.mapTilAvsenderMottaker(): AvsenderMottaker? {
    return when (mottakertype) {
        Mottakertype.BRUKER_MED_UTENLANDSK_ADRESSE,
        Mottakertype.BRUKER,
        -> null

        Mottakertype.VERGE,
        Mottakertype.FULLMEKTIG,
        Mottakertype.DØDSBO,
        -> AvsenderMottaker(
            id = null,
            idType = null,
            navn = this.navn,
        )
    }
}
