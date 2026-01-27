package no.nav.familie.klage.personopplysninger.dto

import java.time.LocalDate
import no.nav.familie.klage.personopplysninger.pdl.Folkeregisterpersonstatus as PdlFolkeregisterpersonstatus

data class PersonopplysningerDto(
    val personIdent: String,
    val navn: String,
    val kjønn: Kjønn,
    val adressebeskyttelse: Adressebeskyttelse?,
    val folkeregisterpersonstatus: Folkeregisterpersonstatus?,
    val dødsdato: LocalDate?,
    val fullmakt: List<FullmaktDto>,
    val egenAnsatt: Boolean,
    val vergemål: List<VergemålDto>,
)

data class FullmaktDto(
    val gyldigFraOgMed: LocalDate,
    val gyldigTilOgMed: LocalDate?,
    val motpartsPersonident: String,
    val navn: String?,
    val områder: List<String>,
)

enum class Adressebeskyttelse {
    STRENGT_FORTROLIG,
    STRENGT_FORTROLIG_UTLAND,
    FORTROLIG,
    UGRADERT,
    ;

    fun erStrengtFortrolig() = this == STRENGT_FORTROLIG || this == STRENGT_FORTROLIG_UTLAND
}

enum class Kjønn {
    KVINNE,
    MANN,
    UKJENT,
}

data class VergemålDto(
    val embete: String?,
    val type: String?,
    val motpartsPersonident: String?,
    val navn: String?,
    val omfang: String?,
)

enum class Folkeregisterpersonstatus(
    private val pdlStatus: String,
) {
    BOSATT("bosatt"),
    UTFLYTTET("utflyttet"),
    FORSVUNNET("forsvunnet"),
    DØD("doed"),
    OPPHØRT("opphoert"),
    FØDSELSREGISTRERT("foedselsregistrert"),
    MIDLERTIDIG("midlertidig"),
    INAKTIV("inaktiv"),
    UKJENT("ukjent"),
    ;

    companion object {
        private val map = entries.associateBy(Folkeregisterpersonstatus::pdlStatus)

        fun fraPdl(status: PdlFolkeregisterpersonstatus) = map.getOrDefault(status.status, UKJENT)
    }
}
