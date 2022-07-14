package no.nav.familie.klage.personopplysninger.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
data class Personopplysninger(
    @Id
    val personId: String,
    val navn: String,
    @Column("kjonn")
    val kjønn: Kjønn,
    val telefonnummer: String,
    val adresse: String,
)

data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val visningsnavn: String
)

enum class Kjønn {
    KVINNE,
    MANN,
    UKJENT
}

data class Telefonnummer(
    val landskode: String,
    val nummer: String
)
