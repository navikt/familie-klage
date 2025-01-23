package no.nav.familie.klage.brev.domain

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import no.nav.familie.klage.felles.domain.Fil
import no.nav.familie.klage.felles.domain.Sporbar
import no.nav.familie.klage.infrastruktur.config.ObjectMapperProvider.objectMapper
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class Brev(
    @Id
    val behandlingId: UUID,
    val saksbehandlerHtml: String,
    val pdf: Fil? = null,
    val mottakere: Brevmottakere? = null,
    val mottakereJournalposter: BrevmottakereJournalposter? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
) {

    fun brevPdf() = this.pdf?.bytes ?: error("Mangler brev-pdf for behandling=$behandlingId")
}

data class BrevmottakereJournalposter(
    val journalposter: List<BrevmottakereJournalpost>,
)

data class BrevmottakereJournalpostMedIdent(
    val ident: String?, // Enten personnummer eller orgnummer
    override val journalpostId: String,
    override val distribusjonId: String? = null,
) : BrevmottakereJournalpost

data class BrevmottakereJournalpostUtenIdent(
    val idForBrevmottakereUtenIdent: UUID?,
    override val journalpostId: String,
    override val distribusjonId: String? = null,
) : BrevmottakereJournalpost

sealed interface BrevmottakereJournalpost {
    val journalpostId: String
    val distribusjonId: String?
}

data class Brevmottakere(
    val personer: List<BrevmottakerPerson> = emptyList(),
    val organisasjoner: List<BrevmottakerOrganisasjon> = emptyList(),
)

enum class MottakerRolle {
    BRUKER,
    VERGE,
    FULLMAKT,
    BRUKER_MED_UTENLANDSK_ADRESSE,
    DØDSBO,
}

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

sealed interface Brevmottaker

class BrevmottakerPersonDeserializer : JsonDeserializer<BrevmottakerPerson>() {
    override fun deserialize(jsonParser: JsonParser, context: DeserializationContext): BrevmottakerPerson {
        val node = jsonParser.codec.readTree<JsonNode>(jsonParser)
        return if (node.has("personIdent")) {
            objectMapper.treeToValue(node, BrevmottakerPersonMedIdent::class.java)
        } else {
            objectMapper.treeToValue(node, BrevmottakerPersonUtenIdent::class.java)
        }
    }
}
