package no.nav.familie.klage.brevmottaker.dto

import no.nav.familie.klage.brevmottaker.domain.SlettbarBrevmottaker
import no.nav.familie.klage.brevmottaker.domain.SlettbarBrevmottakerOrganisasjon
import no.nav.familie.klage.brevmottaker.domain.SlettbarBrevmottakerPersonMedIdent
import no.nav.familie.klage.brevmottaker.domain.SlettbarBrevmottakerPersonUtenIdent
import no.nav.familie.klage.infrastruktur.config.JsonMapperProvider.jsonMapper
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.annotation.JsonDeserialize
import tools.jackson.databind.deser.std.StdDeserializer
import java.util.UUID

@JsonDeserialize(using = SlettbarBrevmottakerDtoDeserializer::class)
sealed interface SlettbarBrevmottakerDto {
    val type: Type

    enum class Type {
        PERSON_MED_IDENT,
        PERSON_UTEN_IDENT,
        ORGANISASJON,
    }
}

fun SlettbarBrevmottakerDto.tilSlettbarBrevmottaker(): SlettbarBrevmottaker =
    when (this) {
        is SlettbarBrevmottakerOrganisasjonDto -> SlettbarBrevmottakerOrganisasjon(organisasjonsnummer)
        is SlettbarBrevmottakerPersonMedIdentDto -> SlettbarBrevmottakerPersonMedIdent(personIdent)
        is SlettbarBrevmottakerPersonUtenIdentDto -> SlettbarBrevmottakerPersonUtenIdent(id)
    }

@JsonDeserialize(`as` = SlettbarBrevmottakerPersonUtenIdentDto::class)
data class SlettbarBrevmottakerPersonUtenIdentDto(
    val id: UUID,
) : SlettbarBrevmottakerDto {
    override val type: SlettbarBrevmottakerDto.Type
        get() = SlettbarBrevmottakerDto.Type.PERSON_UTEN_IDENT
}

@JsonDeserialize(`as` = SlettbarBrevmottakerPersonMedIdentDto::class)
data class SlettbarBrevmottakerPersonMedIdentDto(
    val personIdent: String,
) : SlettbarBrevmottakerDto {
    override val type: SlettbarBrevmottakerDto.Type
        get() = SlettbarBrevmottakerDto.Type.PERSON_MED_IDENT
}

@JsonDeserialize(`as` = SlettbarBrevmottakerOrganisasjonDto::class)
data class SlettbarBrevmottakerOrganisasjonDto(
    val organisasjonsnummer: String,
) : SlettbarBrevmottakerDto {
    override val type: SlettbarBrevmottakerDto.Type
        get() = SlettbarBrevmottakerDto.Type.ORGANISASJON
}

class SlettbarBrevmottakerDtoDeserializer : StdDeserializer<SlettbarBrevmottakerDto>(SlettbarBrevmottakerDto::class.java) {
    override fun deserialize(
        jsonParser: JsonParser,
        context: DeserializationContext,
    ): SlettbarBrevmottakerDto {
        val tree = jsonParser.readValueAsTree<JsonNode>()
        val type = SlettbarBrevmottakerDto.Type.valueOf(tree.get("type").asText())
        return when (type) {
            SlettbarBrevmottakerDto.Type.PERSON_MED_IDENT -> {
                jsonMapper.treeToValue(
                    tree,
                    SlettbarBrevmottakerPersonMedIdentDto::class.java,
                )
            }

            SlettbarBrevmottakerDto.Type.PERSON_UTEN_IDENT -> {
                jsonMapper.treeToValue(
                    tree,
                    SlettbarBrevmottakerPersonUtenIdentDto::class.java,
                )
            }

            SlettbarBrevmottakerDto.Type.ORGANISASJON -> {
                jsonMapper.treeToValue(
                    tree,
                    SlettbarBrevmottakerOrganisasjonDto::class.java,
                )
            }
        }
    }
}
