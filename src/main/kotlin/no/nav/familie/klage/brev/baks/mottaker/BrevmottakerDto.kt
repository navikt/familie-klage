package no.nav.familie.klage.brev.baks.mottaker

import java.util.UUID

data class BrevmottakerDto(
    val id: UUID,
    val mottakertype: Mottakertype,
    val navn: String,
    val adresselinje1: String,
    val adresselinje2: String?,
    val postnummer: String?,
    val poststed: String?,
    val landkode: String,
)
