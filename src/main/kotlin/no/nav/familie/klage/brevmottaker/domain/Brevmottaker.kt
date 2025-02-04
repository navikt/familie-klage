package no.nav.familie.klage.brevmottaker.domain

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import no.nav.familie.klage.infrastruktur.config.ObjectMapperProvider.objectMapper
import java.util.UUID

sealed interface Brevmottaker

@JsonDeserialize(using = BrevmottakerPersonDeserializer::class)
sealed interface BrevmottakerPerson : Brevmottaker {
    val navn: String
    val mottakerRolle: MottakerRolle
}

data class BrevmottakerOrganisasjon(
    val organisasjonsnummer: String,
    val organisasjonsnavn: String,
    val navnHosOrganisasjon: String,
) : Brevmottaker

@JsonDeserialize(`as` = BrevmottakerPersonMedIdent::class)
data class BrevmottakerPersonMedIdent(
    val personIdent: String,
    override val mottakerRolle: MottakerRolle,
    override val navn: String,
) : BrevmottakerPerson

@JsonDeserialize(`as` = BrevmottakerPersonUtenIdent::class)
data class BrevmottakerPersonUtenIdent(
    val id: UUID = UUID.randomUUID(),
    override val mottakerRolle: MottakerRolle,
    override val navn: String,
    val adresselinje1: String,
    val adresselinje2: String?,
    val postnummer: String?,
    val poststed: String?,
    val landkode: String,
) : BrevmottakerPerson

class BrevmottakerPersonDeserializer : JsonDeserializer<BrevmottakerPerson>() {
    override fun deserialize(jsonParser: JsonParser, context: DeserializationContext): BrevmottakerPerson {
        val tree = jsonParser.readValueAsTree<JsonNode>()
        return if (tree.has("personIdent")) {
            objectMapper.treeToValue(tree, BrevmottakerPersonMedIdent::class.java)
        } else {
            objectMapper.treeToValue(tree, BrevmottakerPersonUtenIdent::class.java)
        }
    }
}

enum class MottakerRolle {
    BRUKER,
    VERGE,
    FULLMAKT,
    BRUKER_MED_UTENLANDSK_ADRESSE,
    DØDSBO,
}
