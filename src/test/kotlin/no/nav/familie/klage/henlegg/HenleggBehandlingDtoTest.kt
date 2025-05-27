package no.nav.familie.klage.henlegg

import no.nav.familie.klage.brevmottaker.domain.MottakerRolle
import no.nav.familie.klage.infrastruktur.exception.ApiFeil
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.testutil.DtoTestUtil
import no.nav.familie.kontrakter.felles.klage.HenlagtÅrsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class HenleggBehandlingDtoTest {
    @Nested
    inner class Valider {
        @Test
        fun `skal kaste feil hvis henlagt årsak er feilregistrert og man prøver å sende brev samtidig`() {
            // Arrange
            val henleggBehandlingDto =
                HenleggBehandlingDto(
                    årsak = HenlagtÅrsak.FEILREGISTRERT,
                    skalSendeHenleggelsesbrev = true,
                    nyeBrevmottakere = listOf(DtoTestUtil.lagNyBrevmottakerPersonMedIdentDto(mottakerRolle = MottakerRolle.BRUKER)),
                )

            // Act & assert
            val exception =
                assertThrows<ApiFeil> {
                    henleggBehandlingDto.valider()
                }
            assertThat(exception.message).isEqualTo("Kan ikke sende henleggelsesbrev når årsak er FEILREGISTRERT.")
        }

        @Test
        fun `skal kaste feil hvis man skal sende henleggesesbrev og nye brevmottakere er tom`() {
            // Arrange
            val henleggBehandlingDto =
                HenleggBehandlingDto(
                    årsak = HenlagtÅrsak.TRUKKET_TILBAKE,
                    skalSendeHenleggelsesbrev = true,
                    nyeBrevmottakere = emptyList(),
                )

            // Act & assert
            val exception =
                assertThrows<ApiFeil> {
                    henleggBehandlingDto.valider()
                }
            assertThat(exception.message).isEqualTo("Forventer minst en brevmottaker.")
        }

        @Test
        fun `skal kaste feil hvis flere enn 2 nye brevmottakere blir sendt inn`() {
            // Arrange
            val henleggBehandlingDto =
                HenleggBehandlingDto(
                    årsak = HenlagtÅrsak.TRUKKET_TILBAKE,
                    skalSendeHenleggelsesbrev = true,
                    nyeBrevmottakere =
                        listOf(
                            DtoTestUtil.lagNyBrevmottakerPersonMedIdentDto(mottakerRolle = MottakerRolle.BRUKER),
                            DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto(mottakerRolle = MottakerRolle.FULLMAKT),
                            DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto(mottakerRolle = MottakerRolle.VERGE),
                        ),
                )

            // Act & assert
            val exception =
                assertThrows<ApiFeil> {
                    henleggBehandlingDto.valider()
                }
            assertThat(exception.message).isEqualTo("Forventer ikke mer enn 2 brevmottakere.")
        }

        @Test
        fun `skal kaste feil hvis duplikate mottaker roller blir sendt inn`() {
            // Arrange
            val henleggBehandlingDto =
                HenleggBehandlingDto(
                    årsak = HenlagtÅrsak.TRUKKET_TILBAKE,
                    skalSendeHenleggelsesbrev = true,
                    nyeBrevmottakere =
                        listOf(
                            DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto(mottakerRolle = MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE),
                            DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto(mottakerRolle = MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE),
                        ),
                )

            // Act & assert
            val exception =
                assertThrows<ApiFeil> {
                    henleggBehandlingDto.valider()
                }
            assertThat(exception.message).isEqualTo("Forventer ingen duplikate mottaker roller.")
        }

        @Test
        fun `skal kaste feil hvis mottaker roller BRUKER og BRUKER_MED_UTENLANDSK_ADRESSE blir kombinert`() {
            // Arrange
            val henleggBehandlingDto =
                HenleggBehandlingDto(
                    årsak = HenlagtÅrsak.TRUKKET_TILBAKE,
                    skalSendeHenleggelsesbrev = true,
                    nyeBrevmottakere =
                        listOf(
                            DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto(mottakerRolle = MottakerRolle.BRUKER),
                            DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto(mottakerRolle = MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE),
                        ),
                )

            // Act & assert
            val exception =
                assertThrows<ApiFeil> {
                    henleggBehandlingDto.valider()
                }
            assertThat(exception.message).isEqualTo("${MottakerRolle.BRUKER} kan ikke kombineres med ${MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE}.")
        }

        @Test
        fun `skal kaste feil hvis mottaker roller DØDSBO kombineres med en annen mottaker rolle`() {
            // Arrange
            val henleggBehandlingDto =
                HenleggBehandlingDto(
                    årsak = HenlagtÅrsak.TRUKKET_TILBAKE,
                    skalSendeHenleggelsesbrev = true,
                    nyeBrevmottakere =
                        listOf(
                            DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto(mottakerRolle = MottakerRolle.DØDSBO),
                            DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto(mottakerRolle = MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE),
                        ),
                )

            // Act & assert
            val exception =
                assertThrows<ApiFeil> {
                    henleggBehandlingDto.valider()
                }
            assertThat(exception.message).isEqualTo("${MottakerRolle.DØDSBO} kan ikke kombineres med flere mottaker roller.")
        }

        @Test
        fun `skal kaste feil hvis valideringen av en av de nye brevmottakerene feiler`() {
            // Arrange
            val henleggBehandlingDto =
                HenleggBehandlingDto(
                    årsak = HenlagtÅrsak.TRUKKET_TILBAKE,
                    skalSendeHenleggelsesbrev = true,
                    nyeBrevmottakere =
                        listOf(
                            DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto(navn = ""),
                        ),
                )

            // Act & assert
            val exception =
                assertThrows<ApiFeil> {
                    henleggBehandlingDto.valider()
                }
            assertThat(exception.message).isEqualTo("Navn kan ikke være tomt.")
        }

        @Test
        fun `skal ikke kaste feil hvis valideringen er ok for henlagt årsak TRUKKET_TILBAKE`() {
            // Arrange
            val henleggBehandlingDto =
                HenleggBehandlingDto(
                    årsak = HenlagtÅrsak.TRUKKET_TILBAKE,
                    skalSendeHenleggelsesbrev = true,
                    nyeBrevmottakere =
                        listOf(
                            DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto(mottakerRolle = MottakerRolle.DØDSBO),
                        ),
                )

            // Act & assert
            assertDoesNotThrow { henleggBehandlingDto.valider() }
        }

        @Test
        fun `skal ikke kaste feil hvis valideringen er ok for henlagt årsak FEILREGISTRERT`() {
            // Arrange
            val henleggBehandlingDto =
                HenleggBehandlingDto(
                    årsak = HenlagtÅrsak.FEILREGISTRERT,
                    skalSendeHenleggelsesbrev = false,
                    nyeBrevmottakere = emptyList(),
                )

            // Act & assert
            assertDoesNotThrow { henleggBehandlingDto.valider() }
        }
    }

    @Nested
    inner class FinnNyBrevmottakerBruker {
        @Test
        fun `skal finne brevmottaker bruker`() {
            // Arrange
            val bruker = DtoTestUtil.lagNyBrevmottakerPersonMedIdentDto(mottakerRolle = MottakerRolle.BRUKER)
            val verge = DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto(mottakerRolle = MottakerRolle.VERGE)

            val henleggBehandlingDto =
                HenleggBehandlingDto(
                    årsak = HenlagtÅrsak.TRUKKET_TILBAKE,
                    skalSendeHenleggelsesbrev = true,
                    nyeBrevmottakere = listOf(bruker, verge),
                )

            // Act
            val brevmottakerBruker = henleggBehandlingDto.finnNyBrevmottakerBruker()

            // Assert
            assertThat(brevmottakerBruker).isEqualTo(bruker)
        }

        @Test
        fun `skal ikke finne brevmottaker bruker da det ikke er lagt til`() {
            // Arrange
            val bruker = DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto(mottakerRolle = MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE)
            val verge = DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto(mottakerRolle = MottakerRolle.VERGE)

            val henleggBehandlingDto =
                HenleggBehandlingDto(
                    årsak = HenlagtÅrsak.TRUKKET_TILBAKE,
                    skalSendeHenleggelsesbrev = true,
                    nyeBrevmottakere = listOf(bruker, verge),
                )

            // Act
            val brevmottakerBruker = henleggBehandlingDto.finnNyBrevmottakerBruker()

            // Assert
            assertThat(brevmottakerBruker).isNull()
        }

        @Test
        fun `skal kaste exception om man finner flere brukere`() {
            // Arrange
            val bruker1 = DtoTestUtil.lagNyBrevmottakerPersonMedIdentDto(mottakerRolle = MottakerRolle.BRUKER)
            val bruker2 = DtoTestUtil.lagNyBrevmottakerPersonMedIdentDto(mottakerRolle = MottakerRolle.BRUKER)

            val henleggBehandlingDto =
                HenleggBehandlingDto(
                    årsak = HenlagtÅrsak.TRUKKET_TILBAKE,
                    skalSendeHenleggelsesbrev = true,
                    nyeBrevmottakere = listOf(bruker1, bruker2),
                )

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    henleggBehandlingDto.finnNyBrevmottakerBruker()
                }
            assertThat(exception.message).isEqualTo("Forventer ikke mer enn 1 brevmottaker med mottaker rolle ${MottakerRolle.BRUKER}.")
        }
    }
}
