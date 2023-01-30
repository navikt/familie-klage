package no.nav.familie.klage.søk.ereg

import java.time.LocalDate

data class OrganisasjonDto(
    val organisasjonsnummer: String,
    val type: String,
    val navn: Navn,
    val organisasjonDetaljer: OrganisasjonDetaljer,
)

data class Navn(
    val bruksperiode: Bruksperiode?,
    val gyldighetsperiode: Gyldighetsperiode?,
    val navnelinje1: String?,
    val navnelinje2: String?,
    val navnelinje3: String?,
    val navnelinje4: String?,
    val navnelinje5: String?,
    val redigertnavn: String?,
)

data class Bruksperiode(
    val fom: LocalDate?,
    val tom: LocalDate?,
)

data class Gyldighetsperiode(
    val fom: LocalDate?,
    val tom: LocalDate?,
)

data class OrganisasjonDetaljer(
    val registreringsdato: LocalDate?,
    val enhetstyper: List<Enhetstype>?,
    val navn: List<Navn>?,
    val forretningsAdresser: List<ForretningsAdresse>?,
    val postAdresser: List<Postadresse>?,
    val sistEndret: LocalDate?,
)

data class Enhetstype(
    val bruksperiode: Bruksperiode?,
    val enhetstype: String?,
    val gyldighetsperiode: Gyldighetsperiode?,
)

data class ForretningsAdresse(
    val adresselinje1: String?,
    val adresselinje2: String?,
    val adresselinje3: String?,
    val bruksperiode: Bruksperiode?,
    val gyldighetsperiode: Gyldighetsperiode?,
    val kommunenr: String?,
    val landkode: String?,
    val postnummer: String?,
    val poststed: String?,
)

data class Postadresse(
    val adresselinje1: String?,
    val adresselinje2: String?,
    val adresselinje3: String?,
    val bruksperiode: Bruksperiode?,
    val gyldighetsperiode: Gyldighetsperiode?,
    val kommunenr: String?,
    val landkode: String?,
    val postnummer: String?,
    val poststed: String?,
)
