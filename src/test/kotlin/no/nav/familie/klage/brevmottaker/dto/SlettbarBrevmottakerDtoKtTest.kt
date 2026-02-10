package no.nav.familie.klage.brevmottaker.dto

import no.nav.familie.klage.brevmottaker.domain.SlettbarBrevmottakerOrganisasjon
import no.nav.familie.klage.brevmottaker.domain.SlettbarBrevmottakerPersonMedIdent
import no.nav.familie.klage.brevmottaker.domain.SlettbarBrevmottakerPersonUtenIdent
import no.nav.familie.klage.infrastruktur.config.JsonMapperProvider.jsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class SlettbarBrevmottakerDtoKtTest {
    @Nested
    inner class SlettbarBrevmottakerDtoTest {
        @Test
        fun `skal mappe om til SlettbarBrevmottakerOrganisasjon`() {
            // Arrange
            val dto = SlettbarBrevmottakerOrganisasjonDto("123")

            // Act
            val domene = dto.tilSlettbarBrevmottaker()

            // Assert
            assertThat(domene).isInstanceOfSatisfying(SlettbarBrevmottakerOrganisasjon::class.java) {
                assertThat(it.organisasjonsnummer).isEqualTo("123")
            }
        }

        @Test
        fun `skal mappe om til SlettbarBrevmottakerPersonMedIdent`() {
            // Arrange
            val dto = SlettbarBrevmottakerPersonMedIdentDto("123")

            // Act
            val domene = dto.tilSlettbarBrevmottaker()

            // Assert
            assertThat(domene).isInstanceOfSatisfying(SlettbarBrevmottakerPersonMedIdent::class.java) {
                assertThat(it.personIdent).isEqualTo("123")
            }
        }

        @Test
        fun `skal mappe om til SlettbarBrevmottakerPersonUtenIdent`() {
            // Arrange
            val id = UUID.randomUUID()
            val dto = SlettbarBrevmottakerPersonUtenIdentDto(id)

            // Act
            val domene = dto.tilSlettbarBrevmottaker()

            // Assert
            assertThat(domene).isInstanceOfSatisfying(SlettbarBrevmottakerPersonUtenIdent::class.java) {
                assertThat(it.id).isEqualTo(id)
            }
        }
    }

    @Nested
    inner class SlettbarBrevmottakerOrganisasjonDtoTest {
        @Test
        fun `skal ha riktig type`() {
            // Arrange
            val dto = SlettbarBrevmottakerOrganisasjonDto("123")

            // Act
            val type = dto.type

            // Assert
            assertThat(type).isEqualTo(SlettbarBrevmottakerDto.Type.ORGANISASJON)
        }
    }

    @Nested
    inner class SlettbarBrevmottakerPersonMedIdentDtoTest {
        @Test
        fun `skal ha riktig type`() {
            // Arrange
            val dto = SlettbarBrevmottakerPersonMedIdentDto("123")

            // Act
            val type = dto.type

            // Assert
            assertThat(type).isEqualTo(SlettbarBrevmottakerDto.Type.PERSON_MED_IDENT)
        }
    }

    @Nested
    inner class SlettbarBrevmottakerPersonUtenIdentDtoTest {
        @Test
        fun `skal ha riktig type`() {
            // Arrange
            val dto = SlettbarBrevmottakerPersonUtenIdentDto(UUID.randomUUID())

            // Act
            val type = dto.type

            // Assert
            assertThat(type).isEqualTo(SlettbarBrevmottakerDto.Type.PERSON_UTEN_IDENT)
        }
    }

    @Nested
    inner class SlettbarBrevmottakerDtoDeserializerTest {
        private val slettbarBrevmottakerDtoDeserializer: SlettbarBrevmottakerDtoDeserializer =
            SlettbarBrevmottakerDtoDeserializer()

        @Test
        fun `skal deserialisere SlettbarBrevmottakerOrganisasjonDto`() {
            // Arrange
            val json =
                "{" +
                    "\"type\":\"ORGANISASJON\"," +
                    "\"organisasjonsnummer\":\"321\"" +
                    "}"

            val parser = jsonMapper.factory.createParser(json)

            // Act
            val deserialize =
                slettbarBrevmottakerDtoDeserializer.deserialize(
                    parser,
                    jsonMapper.deserializationContext,
                )

            // Assert
            assertThat(deserialize).isInstanceOfSatisfying(SlettbarBrevmottakerOrganisasjonDto::class.java) {
                assertThat(it.organisasjonsnummer).isEqualTo("321")
            }
        }

        @Test
        fun `skal deserialisere SlettbarBrevmottakerPersonMedIdentDto`() {
            // Arrange
            val json =
                "{" +
                    "\"type\":\"PERSON_MED_IDENT\"," +
                    "\"personIdent\":\"123\"" +
                    "}"

            val parser = jsonMapper.factory.createParser(json)

            // Act
            val deserialize =
                slettbarBrevmottakerDtoDeserializer.deserialize(
                    parser,
                    jsonMapper.deserializationContext,
                )

            // Assert
            assertThat(deserialize).isInstanceOfSatisfying(SlettbarBrevmottakerPersonMedIdentDto::class.java) {
                assertThat(it.personIdent).isEqualTo("123")
            }
        }

        @Test
        fun `skal deserialisere SlettbarBrevmottakerPersonUtenIdentDto`() {
            // Arrange
            val id = UUID.randomUUID()

            val json =
                "{" +
                    "\"type\":\"PERSON_UTEN_IDENT\"," +
                    "\"id\":\"${id}\"" +
                    "}"

            val parser = jsonMapper.factory.createParser(json)

            // Act
            val deserialize =
                slettbarBrevmottakerDtoDeserializer.deserialize(
                    parser,
                    jsonMapper.deserializationContext,
                )

            // Assert
            assertThat(deserialize).isInstanceOfSatisfying(SlettbarBrevmottakerPersonUtenIdentDto::class.java) {
                assertThat(it.id).isEqualTo(id)
            }
        }
    }
}
