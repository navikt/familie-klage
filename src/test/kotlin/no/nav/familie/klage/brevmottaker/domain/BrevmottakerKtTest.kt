package no.nav.familie.klage.brevmottaker.domain

import no.nav.familie.klage.infrastruktur.config.JsonMapperProvider.jsonMapper
import no.nav.familie.klage.testutil.DomainUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class BrevmottakerKtTest {
    @Nested
    inner class BrevmottakerPersonTest {
        @Test
        fun `skal opprette BrevmottakerPerson fra NyBrevmottakerPersonMedIdent`() {
            // Arrange
            val nyBrevmottakerPersonMedIdent = DomainUtil.lagNyBrevmottakerPersonMedIdent()

            // Act
            val brevmottakerPerson = BrevmottakerPerson.opprettFra(nyBrevmottakerPersonMedIdent)

            // Assert
            assertThat(brevmottakerPerson).isInstanceOfSatisfying(BrevmottakerPersonMedIdent::class.java) {
                assertThat(it.personIdent).isEqualTo(nyBrevmottakerPersonMedIdent.personIdent)
                assertThat(it.mottakerRolle).isEqualTo(nyBrevmottakerPersonMedIdent.mottakerRolle)
                assertThat(it.navn).isEqualTo(nyBrevmottakerPersonMedIdent.navn)
            }
        }

        @Test
        fun `skal opprette BrevmottakerPerson fra NyBrevmottakerPersonUtenIdent`() {
            // Arrange
            val nyBrevmottakerPersonUtenIdent = DomainUtil.lagNyBrevmottakerPersonUtenIdent()

            // Act
            val brevmottakerPerson = BrevmottakerPerson.opprettFra(nyBrevmottakerPersonUtenIdent)

            // Assert
            assertThat(brevmottakerPerson).isInstanceOfSatisfying(BrevmottakerPersonUtenIdent::class.java) {
                assertThat(it.id).isNotNull()
                assertThat(it.mottakerRolle).isEqualTo(nyBrevmottakerPersonUtenIdent.mottakerRolle)
                assertThat(it.navn).isEqualTo(nyBrevmottakerPersonUtenIdent.navn)
                assertThat(it.adresselinje1).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje1)
                assertThat(it.adresselinje2).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje2)
                assertThat(it.postnummer).isEqualTo(nyBrevmottakerPersonUtenIdent.postnummer)
                assertThat(it.poststed).isEqualTo(nyBrevmottakerPersonUtenIdent.poststed)
                assertThat(it.landkode).isEqualTo(nyBrevmottakerPersonUtenIdent.landkode)
            }
        }
    }

    @Nested
    inner class BrevmottakerPersonMedIdentTest {
        @Test
        fun `skal opprette BrevmottakerPersonMedIdent fra NyBrevmottakerPersonMedIdent`() {
            // Arrange
            val nyBrevmottakerPersonMedIdent = DomainUtil.lagNyBrevmottakerPersonMedIdent()

            // Act
            val brevmottakerPersonMedIdent = BrevmottakerPersonMedIdent.opprettFra(nyBrevmottakerPersonMedIdent)

            // Assert
            assertThat(brevmottakerPersonMedIdent.personIdent).isEqualTo(nyBrevmottakerPersonMedIdent.personIdent)
            assertThat(brevmottakerPersonMedIdent.navn).isEqualTo(nyBrevmottakerPersonMedIdent.navn)
            assertThat(brevmottakerPersonMedIdent.mottakerRolle).isEqualTo(nyBrevmottakerPersonMedIdent.mottakerRolle)
        }
    }

    @Nested
    inner class BrevmottakerPersonUtenIdentTest {
        @Test
        fun `skal opprette BrevmottakerPersonUtenIdent fra NyBrevmottakerPersonUtenIdent med oppgitt ID`() {
            // Arrange
            val id = UUID.randomUUID()

            val nyBrevmottakerPersonUtenIdent =
                DomainUtil.lagNyBrevmottakerPersonUtenIdent(
                    mottakerRolle = MottakerRolle.FULLMAKT,
                    navn = "Navn Navnesen",
                    adresselinje1 = "Adresselinje 1",
                    adresselinje2 = "Adresselinje 2",
                    postnummer = "0010",
                    poststed = "Oslo",
                    landkode = "NO",
                )

            // Act
            val brevmottakerPersonUtenIdent =
                BrevmottakerPersonUtenIdent.opprettFra(
                    id = id,
                    nyBrevmottakerPersonUtenIdent = nyBrevmottakerPersonUtenIdent,
                )

            // Assert
            assertThat(brevmottakerPersonUtenIdent.id).isEqualTo(id)
            assertThat(brevmottakerPersonUtenIdent.mottakerRolle).isEqualTo(nyBrevmottakerPersonUtenIdent.mottakerRolle)
            assertThat(brevmottakerPersonUtenIdent.navn).isEqualTo(nyBrevmottakerPersonUtenIdent.navn)
            assertThat(brevmottakerPersonUtenIdent.adresselinje1).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje1)
            assertThat(brevmottakerPersonUtenIdent.adresselinje2).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje2)
            assertThat(brevmottakerPersonUtenIdent.postnummer).isEqualTo(nyBrevmottakerPersonUtenIdent.postnummer)
            assertThat(brevmottakerPersonUtenIdent.poststed).isEqualTo(nyBrevmottakerPersonUtenIdent.poststed)
            assertThat(brevmottakerPersonUtenIdent.landkode).isEqualTo(nyBrevmottakerPersonUtenIdent.landkode)
        }

        @Test
        fun `skal opprette BrevmottakerPersonUtenIdent fra NyBrevmottakerPersonUtenIdent med default ID`() {
            // Arrange
            val nyBrevmottakerPersonUtenIdent =
                DomainUtil.lagNyBrevmottakerPersonUtenIdent(
                    mottakerRolle = MottakerRolle.FULLMAKT,
                    navn = "Navn Navnesen",
                    adresselinje1 = "Adresselinje 1",
                    adresselinje2 = "Adresselinje 2",
                    postnummer = "0010",
                    poststed = "Oslo",
                    landkode = "NO",
                )

            // Act
            val brevmottakerPersonUtenIdent =
                BrevmottakerPersonUtenIdent.opprettFra(
                    nyBrevmottakerPersonUtenIdent = nyBrevmottakerPersonUtenIdent,
                )

            // Assert
            assertThat(brevmottakerPersonUtenIdent.id).isNotNull()
            assertThat(brevmottakerPersonUtenIdent.mottakerRolle).isEqualTo(nyBrevmottakerPersonUtenIdent.mottakerRolle)
            assertThat(brevmottakerPersonUtenIdent.navn).isEqualTo(nyBrevmottakerPersonUtenIdent.navn)
            assertThat(brevmottakerPersonUtenIdent.adresselinje1).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje1)
            assertThat(brevmottakerPersonUtenIdent.adresselinje2).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje2)
            assertThat(brevmottakerPersonUtenIdent.postnummer).isEqualTo(nyBrevmottakerPersonUtenIdent.postnummer)
            assertThat(brevmottakerPersonUtenIdent.poststed).isEqualTo(nyBrevmottakerPersonUtenIdent.poststed)
            assertThat(brevmottakerPersonUtenIdent.landkode).isEqualTo(nyBrevmottakerPersonUtenIdent.landkode)
        }
    }

    @Nested
    inner class BrevmottakerPersonDeserializerTest {
        private val brevmottakerPersonDeserializer: BrevmottakerPersonDeserializer = BrevmottakerPersonDeserializer()

        @Test
        fun `skal deserialisere BrevmottakerPersonMedIdent`() {
            // Arrange
            val brevmottakerPersonMedIdent =
                BrevmottakerPersonMedIdent(
                    personIdent = "01492350318",
                    mottakerRolle = MottakerRolle.BRUKER,
                    navn = "Fornavn mellomnavn Etternavn",
                )

            val json = jsonMapper.writeValueAsString(brevmottakerPersonMedIdent)
            val parser = jsonMapper.factory.createParser(json)

            // Act
            val deserialize = brevmottakerPersonDeserializer.deserialize(parser, jsonMapper.deserializationContext)

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

            val brevmottakerPersonMedIdent =
                BrevmottakerPersonUtenIdent(
                    id = id,
                    mottakerRolle = MottakerRolle.FULLMAKT,
                    navn = "navn",
                    adresselinje1 = "Adresselinje 1",
                    adresselinje2 = "Adresselinje 2",
                    postnummer = "0010",
                    poststed = "Oslo",
                    landkode = "NO",
                )

            val json = jsonMapper.writeValueAsString(brevmottakerPersonMedIdent)
            val parser = jsonMapper.factory.createParser(json)

            // Act
            val deserialize = brevmottakerPersonDeserializer.deserialize(parser, jsonMapper.deserializationContext)

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

            val brevmottakerPersonMedIdent =
                BrevmottakerPersonUtenIdent(
                    id = id,
                    mottakerRolle = MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE,
                    navn = "navn",
                    adresselinje1 = "Adresselinje 1, København, 999",
                    adresselinje2 = "Adresselinje 1, København, 999",
                    postnummer = null,
                    poststed = null,
                    landkode = "DK",
                )

            val json = jsonMapper.writeValueAsString(brevmottakerPersonMedIdent)
            val parser = jsonMapper.factory.createParser(json)

            // Act
            val deserialize = brevmottakerPersonDeserializer.deserialize(parser, jsonMapper.deserializationContext)

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
