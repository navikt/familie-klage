package no.nav.familie.klage.distribusjon.domain

import no.nav.familie.klage.infrastruktur.config.ObjectMapperProvider.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class BrevmottakerJournalpostDeserializerTest {
    private val brevmottakerJournalpostDeserializer = BrevmottakerJournalpostDeserializer()

    @Nested
    inner class DeserializeTest {
        @Test
        fun `skal deserialisere BrevmottakerJournalpostMedIdent`() {
            // Arrange
            val brevmottakerJournalpostMedIdent = BrevmottakerJournalpostMedIdent(
                ident = "12345678910",
                journalpostId = "journalpostId",
                distribusjonId = "distribusjonId",
            )

            val json = objectMapper.writeValueAsString(brevmottakerJournalpostMedIdent)
            val parser = objectMapper.factory.createParser(json)

            // Act
            val deserialize =
                brevmottakerJournalpostDeserializer.deserialize(parser, objectMapper.deserializationContext)

            // Assert
            assertThat(deserialize).isInstanceOfSatisfying(BrevmottakerJournalpostMedIdent::class.java) {
                assertThat(it.ident).isEqualTo(brevmottakerJournalpostMedIdent.ident)
                assertThat(it.journalpostId).isEqualTo(brevmottakerJournalpostMedIdent.journalpostId)
                assertThat(it.distribusjonId).isEqualTo(brevmottakerJournalpostMedIdent.distribusjonId)
            }
        }

        @Test
        fun `skal deserialisere BrevmottakerJournalpostUtenIdent`() {
            // Arrange
            val brevmottakerJournalpostMedIdent = BrevmottakerJournalpostUtenIdent(
                id = UUID.randomUUID(),
                journalpostId = "journalpostId",
                distribusjonId = "distribusjonId",
            )

            val json = objectMapper.writeValueAsString(brevmottakerJournalpostMedIdent)
            val parser = objectMapper.factory.createParser(json)

            // Act
            val deserialize =
                brevmottakerJournalpostDeserializer.deserialize(parser, objectMapper.deserializationContext)

            // Assert
            assertThat(deserialize).isInstanceOfSatisfying(BrevmottakerJournalpostUtenIdent::class.java) {
                assertThat(it.id).isEqualTo(brevmottakerJournalpostMedIdent.id)
                assertThat(it.journalpostId).isEqualTo(brevmottakerJournalpostMedIdent.journalpostId)
                assertThat(it.distribusjonId).isEqualTo(brevmottakerJournalpostMedIdent.distribusjonId)
            }
        }
    }
}
