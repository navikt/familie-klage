package no.nav.familie.klage.brevmottaker.domain

import no.nav.familie.klage.infrastruktur.config.ObjectMapperProvider.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class BrevmottakerKtTest {
    private val brevmottakerPersonDeserializer: BrevmottakerPersonDeserializer = BrevmottakerPersonDeserializer()

    @Nested
    inner class BrevmottakerPersonDeserializerTest {
        @Test
        fun `skal deserialisere BrevmottakerPersonMedIdent`() {
            // Arrange
            val brevmottakerPersonMedIdent = BrevmottakerPersonMedIdent(
                personIdent = "01492350318",
                mottakerRolle = MottakerRolle.BRUKER,
                navn = "Fornavn mellomnavn Etternavn",
            )

            val json = objectMapper.writeValueAsString(brevmottakerPersonMedIdent)
            val parser = objectMapper.factory.createParser(json)

            // Act
            val deserialize = brevmottakerPersonDeserializer.deserialize(parser, objectMapper.deserializationContext)

            // Assert
            assertThat(deserialize).isInstanceOfSatisfying(BrevmottakerPersonMedIdent::class.java) {
                assertThat(it.personIdent).isEqualTo(brevmottakerPersonMedIdent.personIdent)
                assertThat(it.mottakerRolle).isEqualTo(brevmottakerPersonMedIdent.mottakerRolle)
                assertThat(it.navn).isEqualTo(brevmottakerPersonMedIdent.navn)
            }
        }

        @Test
        fun `skal deserialisere en norsk BrevmottakerPersonUtenIdent`() {
            // Arrange
            val id = UUID.randomUUID()

            val brevmottakerPersonMedIdent = BrevmottakerPersonUtenIdent(
                id = id,
                mottakerRolle = MottakerRolle.FULLMAKT,
                navn = "navn",
                adresselinje1 = "Adresselinje 1",
                adresselinje2 = "Adresselinje 2",
                postnummer = "0010",
                poststed = "Oslo",
                landkode = "NO",
            )

            val json = objectMapper.writeValueAsString(brevmottakerPersonMedIdent)
            val parser = objectMapper.factory.createParser(json)

            // Act
            val deserialize = brevmottakerPersonDeserializer.deserialize(parser, objectMapper.deserializationContext)

            // Assert
            assertThat(deserialize).isInstanceOfSatisfying(BrevmottakerPersonUtenIdent::class.java) {
                assertThat(it.id).isEqualTo(id)
                assertThat(it.mottakerRolle).isEqualTo(brevmottakerPersonMedIdent.mottakerRolle)
                assertThat(it.navn).isEqualTo(brevmottakerPersonMedIdent.navn)
                assertThat(it.adresselinje1).isEqualTo(brevmottakerPersonMedIdent.adresselinje1)
                assertThat(it.adresselinje2).isEqualTo(brevmottakerPersonMedIdent.adresselinje2)
                assertThat(it.postnummer).isEqualTo(brevmottakerPersonMedIdent.postnummer)
                assertThat(it.poststed).isEqualTo(brevmottakerPersonMedIdent.poststed)
                assertThat(it.landkode).isEqualTo(brevmottakerPersonMedIdent.landkode)
            }
        }

        @Test
        fun `skal deserialisere en utenlandsk BrevmottakerPersonUtenIdent`() {
            // Arrange
            val id = UUID.randomUUID()

            val brevmottakerPersonMedIdent = BrevmottakerPersonUtenIdent(
                id = id,
                mottakerRolle = MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE,
                navn = "navn",
                adresselinje1 = "Adresselinje 1, København, 999",
                adresselinje2 = "Adresselinje 1, København, 999",
                postnummer = null,
                poststed = null,
                landkode = "DK",
            )

            val json = objectMapper.writeValueAsString(brevmottakerPersonMedIdent)
            val parser = objectMapper.factory.createParser(json)

            // Act
            val deserialize = brevmottakerPersonDeserializer.deserialize(parser, objectMapper.deserializationContext)

            // Assert
            assertThat(deserialize).isInstanceOfSatisfying(BrevmottakerPersonUtenIdent::class.java) {
                assertThat(it.id).isEqualTo(id)
                assertThat(it.mottakerRolle).isEqualTo(brevmottakerPersonMedIdent.mottakerRolle)
                assertThat(it.navn).isEqualTo(brevmottakerPersonMedIdent.navn)
                assertThat(it.adresselinje1).isEqualTo(brevmottakerPersonMedIdent.adresselinje1)
                assertThat(it.adresselinje2).isEqualTo(brevmottakerPersonMedIdent.adresselinje2)
                assertThat(it.postnummer).isEqualTo(brevmottakerPersonMedIdent.postnummer)
                assertThat(it.poststed).isEqualTo(brevmottakerPersonMedIdent.poststed)
                assertThat(it.landkode).isEqualTo(brevmottakerPersonMedIdent.landkode)
            }
        }
    }
}
