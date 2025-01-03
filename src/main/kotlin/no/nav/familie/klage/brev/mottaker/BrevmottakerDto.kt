package no.nav.familie.klage.brev.mottaker

import java.util.UUID

// TODO : Her burde man kanskje sørge for at Dto'en ikke opprettes i en ugyldig tilstand via init metoden?

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
