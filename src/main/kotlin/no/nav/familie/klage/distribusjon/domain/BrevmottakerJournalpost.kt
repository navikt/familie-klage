package no.nav.familie.klage.distribusjon.domain

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import no.nav.familie.klage.infrastruktur.config.ObjectMapperProvider.objectMapper
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

class BrevmottakerJournalpostDeserializer : JsonDeserializer<BrevmottakerJournalpost>() {
    override fun deserialize(
        jsonParser: JsonParser,
        context: DeserializationContext,
    ): BrevmottakerJournalpost {
        val tree = jsonParser.readValueAsTree<JsonNode>()
        return if (tree.has("ident")) {
            objectMapper.treeToValue(tree, BrevmottakerJournalpostMedIdent::class.java)
        } else {
            objectMapper.treeToValue(tree, BrevmottakerJournalpostUtenIdent::class.java)
        }
    }
}
