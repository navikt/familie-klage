package no.nav.familie.klage.brevmottaker.dto

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle
import no.nav.familie.klage.brevmottaker.domain.NyBrevmottaker
import no.nav.familie.klage.brevmottaker.domain.NyBrevmottakerOrganisasjon
import no.nav.familie.klage.brevmottaker.domain.NyBrevmottakerPersonMedIdent
import no.nav.familie.klage.brevmottaker.domain.NyBrevmottakerPersonUtenIdent
import no.nav.familie.klage.infrastruktur.config.ObjectMapperProvider.objectMapper
import no.nav.familie.klage.infrastruktur.exception.ApiFeil

private const val LANDKODE_NO = "NO"

@JsonDeserialize(using = NyBrevmottakerDtoDeserializer::class)
sealed interface NyBrevmottakerDto {
    val type: Type

    fun valider()

    enum class Type {
        PERSON_MED_IDENT,
        PERSON_UTEN_IDENT,
        ORGANISASJON,
    }
}

fun NyBrevmottakerDto.tilDomene(): NyBrevmottaker {
    return when (this) {
        is NyBrevmottakerOrganisasjonDto -> {
            NyBrevmottakerOrganisasjon(
                organisasjonsnummer = organisasjonsnummer,
                organisasjonsnavn = organisasjonsnavn,
                navnHosOrganisasjon = navnHosOrganisasjon,
            )
        }

        is NyBrevmottakerPersonMedIdentDto -> {
            NyBrevmottakerPersonMedIdent(
                personIdent = personIdent,
                mottakerRolle = mottakerRolle,
                navn = navn,
            )
        }

        is NyBrevmottakerPersonUtenIdentDto -> {
            NyBrevmottakerPersonUtenIdent(
                mottakerRolle = mottakerRolle,
                navn = navn,
                adresselinje1 = adresselinje1,
                adresselinje2 = adresselinje2,
                postnummer = postnummer,
                poststed = poststed,
                landkode = landkode,
            )
        }
    }
}

@JsonDeserialize(`as` = NyBrevmottakerPersonUtenIdentDto::class)
data class NyBrevmottakerPersonUtenIdentDto(
    val mottakerRolle: MottakerRolle,
    val navn: String,
    val adresselinje1: String,
    val adresselinje2: String?,
    val postnummer: String?,
    val poststed: String?,
    val landkode: String,
) : NyBrevmottakerDto {
    override val type: NyBrevmottakerDto.Type
        get() = NyBrevmottakerDto.Type.PERSON_UTEN_IDENT

    override fun valider() {
        if (mottakerRolle == MottakerRolle.BRUKER) {
            throw ApiFeil.badRequest("Det er ikke mulig å sette ${MottakerRolle.BRUKER} for saksbehandler.")
        }
        if (landkode.length != 2) {
            throw ApiFeil.badRequest("Ugyldig landkode: $landkode.")
        }
        if (navn.isBlank()) {
            throw ApiFeil.badRequest("Navn kan ikke være tomt.")
        }
        if (adresselinje1.isBlank()) {
            throw ApiFeil.badRequest("Adresselinje 1 kan ikke være tomt.")
        }
        if (landkode == LANDKODE_NO) {
            if (postnummer.isNullOrBlank()) {
                throw ApiFeil.badRequest("Når landkode er $LANDKODE_NO (Norge) må postnummer være satt.")
            }
            if (poststed.isNullOrBlank()) {
                throw ApiFeil.badRequest("Når landkode er $LANDKODE_NO (Norge) må poststed være satt.")
            }
            if (postnummer.length != 4 || !postnummer.all { it.isDigit() }) {
                throw ApiFeil.badRequest("Postnummer må være 4 siffer.")
            }
            if (mottakerRolle == MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE) {
                throw ApiFeil.badRequest("Bruker med utenlandsk adresse kan ikke ha landkode $LANDKODE_NO.")
            }
        } else {
            if (!postnummer.isNullOrBlank()) {
                throw ApiFeil.badRequest("Ved utenlandsk landkode må postnummer settes i adresselinje 1.")
            }
            if (!poststed.isNullOrBlank()) {
                throw ApiFeil.badRequest("Ved utenlandsk landkode må poststed settes i adresselinje 1.")
            }
        }
    }
}

@JsonDeserialize(`as` = NyBrevmottakerPersonMedIdentDto::class)
data class NyBrevmottakerPersonMedIdentDto(
    val personIdent: String,
    val mottakerRolle: MottakerRolle,
    val navn: String,
) : NyBrevmottakerDto {
    override val type: NyBrevmottakerDto.Type
        get() = NyBrevmottakerDto.Type.PERSON_MED_IDENT

    override fun valider() {
        if (mottakerRolle == MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE) {
            throw ApiFeil.badRequest("Person med ident kan ikke være ${MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE}")
        }
    }
}

@JsonDeserialize(`as` = NyBrevmottakerOrganisasjonDto::class)
data class NyBrevmottakerOrganisasjonDto(
    val organisasjonsnummer: String,
    val organisasjonsnavn: String,
    val navnHosOrganisasjon: String,
) : NyBrevmottakerDto {
    override val type: NyBrevmottakerDto.Type
        get() = NyBrevmottakerDto.Type.ORGANISASJON

    override fun valider() {
        // Do nothing...
    }
}

class NyBrevmottakerDtoDeserializer : JsonDeserializer<NyBrevmottakerDto>() {
    override fun deserialize(jsonParser: JsonParser, context: DeserializationContext): NyBrevmottakerDto {
        val tree = jsonParser.readValueAsTree<JsonNode>()
        val type = NyBrevmottakerDto.Type.valueOf(tree.get("type").asText())
        return when (type) {
            NyBrevmottakerDto.Type.PERSON_MED_IDENT -> objectMapper.treeToValue(
                tree,
                NyBrevmottakerPersonMedIdentDto::class.java,
            )

            NyBrevmottakerDto.Type.PERSON_UTEN_IDENT -> objectMapper.treeToValue(
                tree,
                NyBrevmottakerPersonUtenIdentDto::class.java,
            )

            NyBrevmottakerDto.Type.ORGANISASJON -> objectMapper.treeToValue(
                tree,
                NyBrevmottakerOrganisasjonDto::class.java,
            )
        }
    }
}
