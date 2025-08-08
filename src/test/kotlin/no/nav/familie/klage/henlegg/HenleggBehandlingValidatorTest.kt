package no.nav.familie.klage.henlegg

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.klage.brevmottaker.BrevmottakerService
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle
import no.nav.familie.klage.infrastruktur.exception.ApiFeil
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.klage.testutil.DtoTestUtil
import no.nav.familie.kontrakter.felles.klage.HenlagtÅrsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class HenleggBehandlingValidatorTest {
    private val brevmottakerService = mockk<BrevmottakerService>()
    private val henleggBehandlingValidator = HenleggBehandlingValidator(brevmottakerService = brevmottakerService)

    @Nested
    inner class ValiderHenleggBehandlingDto {
        @Test
        fun `skal ikke kaste exception om dto er validert OK og inneholder bruker`() {
            // Arrange
            val behandlingId = UUID.randomUUID()

            val brukerFraDto =
                DtoTestUtil.lagNyBrevmottakerPersonMedIdentDto(
                    personIdent = "1",
                    mottakerRolle = MottakerRolle.BRUKER,
                    navn = "navn",
                )

            val brukerFraBehandling =
                DomainUtil.lagBrevmottakerPersonMedIdent(
                    personIdent = "1",
                    mottakerRolle = MottakerRolle.BRUKER,
                    navn = "navn",
                )

            val henleggBehandlingDto =
                HenleggBehandlingDto(
                    årsak = HenlagtÅrsak.TRUKKET_TILBAKE,
                    skalSendeHenleggelsesbrev = true,
                    nyeBrevmottakere =
                        listOf(
                            brukerFraDto,
                        ),
                )

            every { brevmottakerService.utledBrevmottakerBrukerFraBehandling(behandlingId) } returns brukerFraBehandling

            // Act & assert
            assertDoesNotThrow {
                henleggBehandlingValidator.validerHenleggBehandlingDto(behandlingId, henleggBehandlingDto)
            }
        }

        @Test
        fun `skal ikke kaste exception om dto er validert OK og inneholder ikke bruker`() {
            // Arrange
            val behandlingId = UUID.randomUUID()

            val brukerMedUtenlandskAdresseFraDto =
                DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto(
                    mottakerRolle = MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE,
                    navn = "navn",
                    adresselinje1 = "Adresse 1, 0001, Nordpolen",
                    adresselinje2 = null,
                    poststed = null,
                    postnummer = null,
                    landkode = "DK",
                )

            val brukerFraBehandling =
                DomainUtil.lagBrevmottakerPersonMedIdent(
                    personIdent = "1",
                    mottakerRolle = MottakerRolle.BRUKER,
                    navn = "navn",
                )

            val henleggBehandlingDto =
                HenleggBehandlingDto(
                    årsak = HenlagtÅrsak.TRUKKET_TILBAKE,
                    skalSendeHenleggelsesbrev = true,
                    nyeBrevmottakere =
                        listOf(
                            brukerMedUtenlandskAdresseFraDto,
                        ),
                )

            every { brevmottakerService.utledBrevmottakerBrukerFraBehandling(behandlingId) } returns brukerFraBehandling

            // Act & assert
            assertDoesNotThrow {
                henleggBehandlingValidator.validerHenleggBehandlingDto(behandlingId, henleggBehandlingDto)
            }
        }

        @Test
        fun `skal kaste exception bruker fra dto og bruker fra behandling er ulike`() {
            // Arrange
            val behandlingId = UUID.randomUUID()

            val brukerFraDto =
                DtoTestUtil.lagNyBrevmottakerPersonMedIdentDto(
                    personIdent = "1",
                    mottakerRolle = MottakerRolle.BRUKER,
                    navn = "navn",
                )

            val brukerFraBehandling =
                DomainUtil.lagBrevmottakerPersonMedIdent(
                    personIdent = "2",
                    mottakerRolle = MottakerRolle.BRUKER,
                    navn = "navn",
                )

            val henleggBehandlingDto =
                HenleggBehandlingDto(
                    årsak = HenlagtÅrsak.TRUKKET_TILBAKE,
                    skalSendeHenleggelsesbrev = true,
                    nyeBrevmottakere =
                        listOf(
                            brukerFraDto,
                        ),
                )

            every { brevmottakerService.utledBrevmottakerBrukerFraBehandling(behandlingId) } returns brukerFraBehandling

            // Act & assert
            val exception =
                assertThrows<ApiFeil> {
                    henleggBehandlingValidator.validerHenleggBehandlingDto(behandlingId, henleggBehandlingDto)
                }
            assertThat(exception.message).isEqualTo("Innsendt bruker samsvarer ikke med bruker utledet fra behandlingen.")
        }
    }
}
