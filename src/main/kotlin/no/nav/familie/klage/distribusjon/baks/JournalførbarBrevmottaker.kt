package no.nav.familie.klage.distribusjon.baks

import no.nav.familie.klage.brev.baks.brevmottaker.Brevmottaker
import no.nav.familie.klage.brev.baks.brevmottaker.Mottakertype
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.kontrakter.felles.dokdist.AdresseType
import no.nav.familie.kontrakter.felles.dokdist.ManuellAdresse

private const val LANDKODE_NO = "NO"

data class JournalførbarBrevmottaker(
    val navn: String,
    val adresse: Adresse?,
    val mottakertype: Mottakertype,
) {
    companion object {
        fun opprettForBruker(navn: String): JournalførbarBrevmottaker {
            return JournalførbarBrevmottaker(
                navn,
                null,
                Mottakertype.BRUKER,
            )
        }

        fun opprettForBrevmottaker(brevmottaker: Brevmottaker): JournalførbarBrevmottaker {
            return JournalførbarBrevmottaker(
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

fun JournalførbarBrevmottaker.mapTilAvsenderMottaker(): AvsenderMottaker? {
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

fun JournalførbarBrevmottaker.Adresse.mapTilManuellAdresse(): ManuellAdresse {
    return ManuellAdresse(
        adresseType = if (this.landkode == LANDKODE_NO) AdresseType.norskPostadresse else AdresseType.utenlandskPostadresse,
        adresselinje1 = this.adresselinje1,
        adresselinje2 = this.adresselinje2,
        adresselinje3 = null,
        postnummer = this.postnummer,
        poststed = this.poststed,
        land = this.landkode,
    )
}
