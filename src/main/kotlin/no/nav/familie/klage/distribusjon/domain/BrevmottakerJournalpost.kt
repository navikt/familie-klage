package no.nav.familie.klage.distribusjon.domain

import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.annotation.JsonDeserialize
import no.nav.familie.klage.infrastruktur.config.JsonMapperProvider.jsonMapper
import java.util.UUID

@JsonDeserialize(using = BrevmottakerJournalpostDeserializer::class)
sealed interface BrevmottakerJournalpost {
    val journalpostId: String
    val distribusjonId: String?

    fun medDistribusjonsId(distribusjonId: String): BrevmottakerJournalpost
}

@JsonDeserialize(`as` = BrevmottakerJournalpostMedIdent::class)
data class BrevmottakerJournalpostMedIdent(
    val ident: String,
    override val journalpostId: String,
    override val distribusjonId: String? = null,
) : BrevmottakerJournalpost {
    override fun medDistribusjonsId(distribusjonId: String) = copy(distribusjonId = distribusjonId)
}

@JsonDeserialize(`as` = BrevmottakerJournalpostUtenIdent::class)
data class BrevmottakerJournalpostUtenIdent(
    val id: UUID,
    override val journalpostId: String,
    override val distribusjonId: String? = null,
) : BrevmottakerJournalpost {
    override fun medDistribusjonsId(distribusjonId: String) = copy(distribusjonId = distribusjonId)
}

class BrevmottakerJournalpostDeserializer : ValueDeserializer<BrevmottakerJournalpost>() {
    override fun deserialize(
        jsonParser: JsonParser,
        context: DeserializationContext,
    ): BrevmottakerJournalpost {
        val tree = jsonParser.readValueAsTree<JsonNode>()
        return if (tree.has("ident")) {
            jsonMapper.treeToValue(tree, BrevmottakerJournalpostMedIdent::class.java)
        } else {
            jsonMapper.treeToValue(tree, BrevmottakerJournalpostUtenIdent::class.java)
        }
    }
}
