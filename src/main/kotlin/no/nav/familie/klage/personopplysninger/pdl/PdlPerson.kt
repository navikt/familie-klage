package no.nav.familie.klage.personopplysninger.pdl

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.LocalDateTime

data class PdlResponse<T>(
    val data: T,
    val errors: List<PdlError>?,
    val extensions: PdlExtensions?,
) {
    fun harFeil(): Boolean = errors != null && errors.isNotEmpty()

    fun harAdvarsel(): Boolean = !extensions?.warnings.isNullOrEmpty()

    fun errorMessages(): String = errors?.joinToString { it -> it.message } ?: ""
}

data class PdlExtensions(
    val warnings: List<PdlWarning>?,
)

data class PdlWarning(
    val details: Any?,
    val id: String?,
    val message: String?,
    val query: String?,
)

data class PdlBolkResponse<T>(
    val data: PersonBolk<T>?,
    val errors: List<PdlError>?,
    val extensions: PdlExtensions?,
) {
    fun errorMessages(): String = errors?.joinToString { it -> it.message } ?: ""

    fun harAdvarsel(): Boolean = !extensions?.warnings.isNullOrEmpty()
}

data class PdlError(
    val message: String,
    val extensions: PdlErrorExtensions?,
)

data class PdlErrorExtensions(
    val code: String?,
) {
    fun notFound() = code == "not_found"
}

data class PdlSøkerData(
    val person: PdlSøker?,
)

data class PdlIdent(
    val ident: String,
    val historisk: Boolean,
)

data class PdlIdenter(
    val identer: List<PdlIdent>,
) {
    fun gjeldende(): PdlIdent = this.identer.first { !it.historisk }

    fun identer(): Set<String> = this.identer.map { it.ident }.toSet()
}

data class PdlHentIdenter(
    val hentIdenter: PdlIdenter?,
)

data class PersonDataBolk<T>(
    val ident: String,
    val code: String,
    val person: T?,
)

data class PersonBolk<T>(
    val personBolk: List<PersonDataBolk<T>>,
)

data class PdlNavn(
    val navn: List<Navn>,
)

data class PdlSøker(
    val adressebeskyttelse: List<Adressebeskyttelse>,
    @JsonProperty("doedsfall") val dødsfall: List<Dødsfall>,
    @JsonProperty("kjoenn") val kjønn: List<Kjønn>,
    val folkeregisterpersonstatus: List<Folkeregisterpersonstatus>,
    val navn: List<Navn>,
    val vergemaalEllerFremtidsfullmakt: List<VergemaalEllerFremtidsfullmakt>,
)

data class Metadata(
    val historisk: Boolean,
)

data class Folkeregistermetadata(
    val gyldighetstidspunkt: LocalDateTime?,
    @JsonProperty("opphoerstidspunkt") val opphørstidspunkt: LocalDateTime?,
)

data class Adressebeskyttelse(
    val gradering: AdressebeskyttelseGradering,
    val metadata: Metadata,
) {
    fun erStrengtFortrolig(): Boolean =
        this.gradering == AdressebeskyttelseGradering.STRENGT_FORTROLIG ||
            this.gradering == AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND
}

@Suppress("unused")
enum class AdressebeskyttelseGradering {
    STRENGT_FORTROLIG,
    STRENGT_FORTROLIG_UTLAND,
    FORTROLIG,
    UGRADERT,
}

data class Dødsfall(
    @JsonProperty("doedsdato") val dødsdato: LocalDate?,
)

data class Folkeregisterpersonstatus(
    val status: String,
    val forenkletStatus: String,
    val metadata: Metadata,
)

data class Fullmakt(
    val gyldigFraOgMed: LocalDate,
    val gyldigTilOgMed: LocalDate?,
    val motpartsPersonident: String,
    val fullmektigsNavn: String?,
    val motpartsRolle: MotpartsRolle,
    val omraader: List<String>,
)

@Suppress("unused")
enum class MotpartsRolle {
    FULLMAKTSGIVER,
    FULLMEKTIG,
}

data class Kjønn(
    @JsonProperty("kjoenn") val kjønn: KjønnType,
)

@Suppress("unused")
enum class KjønnType {
    KVINNE,
    MANN,
    UKJENT,
}

data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val metadata: Metadata,
)

data class Personnavn(
    val etternavn: String,
    val fornavn: String,
    val mellomnavn: String?,
)

data class VergeEllerFullmektig(
    val motpartsPersonident: String?,
    val navn: Personnavn?,
    val omfang: String?,
    val omfangetErInnenPersonligOmraade: Boolean,
)

data class VergemaalEllerFremtidsfullmakt(
    val embete: String?,
    val folkeregistermetadata: Folkeregistermetadata?,
    val type: String?,
    val vergeEllerFullmektig: VergeEllerFullmektig,
)
