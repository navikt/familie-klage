package no.nav.familie.klage.brevmottaker.domain

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import no.nav.familie.klage.infrastruktur.config.ObjectMapperProvider.objectMapper
import java.util.UUID

sealed interface Brevmottaker {
    val mottakerRolle: MottakerRolle?
}

@JsonDeserialize(using = BrevmottakerPersonDeserializer::class)
sealed interface BrevmottakerPerson : Brevmottaker {
    val navn: String
    override val mottakerRolle: MottakerRolle

    companion object {
        fun opprettFra(nyBrevmottakerPerson: NyBrevmottakerPerson): BrevmottakerPerson =
            when (nyBrevmottakerPerson) {
                is NyBrevmottakerPersonMedIdent -> BrevmottakerPersonMedIdent.opprettFra(nyBrevmottakerPerson)
                is NyBrevmottakerPersonUtenIdent -> BrevmottakerPersonUtenIdent.opprettFra(UUID.randomUUID(), nyBrevmottakerPerson)
            }
    }
}

data class BrevmottakerOrganisasjon(
    val organisasjonsnummer: String,
    val organisasjonsnavn: String,
    val navnHosOrganisasjon: String,
    override val mottakerRolle: MottakerRolle? = null,
) : Brevmottaker {
    companion object {
        fun opprettFra(nyBrevmottakerOrganisasjon: NyBrevmottakerOrganisasjon): BrevmottakerOrganisasjon =
            BrevmottakerOrganisasjon(
                organisasjonsnummer = nyBrevmottakerOrganisasjon.organisasjonsnummer,
                organisasjonsnavn = nyBrevmottakerOrganisasjon.organisasjonsnavn,
                navnHosOrganisasjon = nyBrevmottakerOrganisasjon.navnHosOrganisasjon,
                mottakerRolle = nyBrevmottakerOrganisasjon.mottakerRolle,
            )
    }
}

@JsonDeserialize(`as` = BrevmottakerPersonMedIdent::class)
data class BrevmottakerPersonMedIdent(
    val personIdent: String,
    override val navn: String,
    override val mottakerRolle: MottakerRolle,
) : BrevmottakerPerson {
    companion object {
        fun opprettFra(
            nyBrevmottakerPersonMedIdent: NyBrevmottakerPersonMedIdent,
        ): BrevmottakerPersonMedIdent =
            BrevmottakerPersonMedIdent(
                personIdent = nyBrevmottakerPersonMedIdent.personIdent,
                mottakerRolle = nyBrevmottakerPersonMedIdent.mottakerRolle,
                navn = nyBrevmottakerPersonMedIdent.navn,
            )
    }
}

@JsonDeserialize(`as` = BrevmottakerPersonUtenIdent::class)
data class BrevmottakerPersonUtenIdent(
    val id: UUID = UUID.randomUUID(),
    val adresselinje1: String,
    val adresselinje2: String?,
    val postnummer: String?,
    val poststed: String?,
    val landkode: String,
    override val navn: String,
    override val mottakerRolle: MottakerRolle,
) : BrevmottakerPerson {
    companion object {
        fun opprettFra(
            id: UUID = UUID.randomUUID(),
            nyBrevmottakerPersonUtenIdent: NyBrevmottakerPersonUtenIdent,
        ): BrevmottakerPersonUtenIdent =
            BrevmottakerPersonUtenIdent(
                id = id,
                mottakerRolle = nyBrevmottakerPersonUtenIdent.mottakerRolle,
                navn = nyBrevmottakerPersonUtenIdent.navn,
                adresselinje1 = nyBrevmottakerPersonUtenIdent.adresselinje1,
                adresselinje2 = nyBrevmottakerPersonUtenIdent.adresselinje2,
                postnummer = nyBrevmottakerPersonUtenIdent.postnummer,
                poststed = nyBrevmottakerPersonUtenIdent.poststed,
                landkode = nyBrevmottakerPersonUtenIdent.landkode,
            )
    }
}

class BrevmottakerPersonDeserializer : JsonDeserializer<BrevmottakerPerson>() {
    override fun deserialize(
        jsonParser: JsonParser,
        context: DeserializationContext,
    ): BrevmottakerPerson {
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
    MOTTAKER,
    INSTITUSJON,
}
