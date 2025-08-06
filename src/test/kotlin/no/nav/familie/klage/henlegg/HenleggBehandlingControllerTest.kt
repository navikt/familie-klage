package no.nav.familie.klage.henlegg

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle
import no.nav.familie.klage.infrastruktur.exception.ApiFeil
import no.nav.familie.klage.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.klage.testutil.DtoTestUtil
import no.nav.familie.kontrakter.felles.klage.HenlagtÅrsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class HenleggBehandlingControllerTest {
    private val tilgangService = mockk<TilgangService>()
    private val henleggBehandlingService = mockk<HenleggBehandlingService>()
    private val henleggBehandlingValidator = mockk<HenleggBehandlingValidator>()
    private val brevService = mockk<BrevService>()

    private val henleggBehandlingController =
        HenleggBehandlingController(
            tilgangService = tilgangService,
            henleggBehandlingService = henleggBehandlingService,
            henleggBehandlingValidator = henleggBehandlingValidator,
            brevService = brevService,
        )

    @BeforeEach
    fun setup() {
        every { tilgangService.validerTilgangTilBehandling(any(), any()) } just runs
        every { tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(any()) } just runs
        every { henleggBehandlingValidator.validerHenleggBehandlingDto(any(), any()) } just runs
    }

    @Nested
    inner class HenleggBehandling {
        @Test
        fun `skal kaste feil hvis tilgangservice valider tilgang til person med relasjon for behandling feiler`() {
            // Arrange
            val behandlingId = UUID.randomUUID()

            val henleggBehandlingDto =
                HenleggBehandlingDto(
                    årsak = HenlagtÅrsak.TRUKKET_TILBAKE,
                    skalSendeHenleggelsesbrev = false,
                )

            every { tilgangService.validerTilgangTilBehandling(any(), any()) } throws RuntimeException("Ops!")

            // Act & assert
            val exception =
                assertThrows<RuntimeException> {
                    henleggBehandlingController.henleggBehandling(
                        behandlingId = behandlingId,
                        henleggBehandlingDto = henleggBehandlingDto,
                    )
                }
            assertThat(exception.message).isEqualTo("Ops!")
        }

        @Test
        fun `skal kaste feil hvis tilgangservice valider saksbehandlerrole til stønad for behandling feiler`() {
            // Arrange
            val behandlingId = UUID.randomUUID()

            val henleggBehandlingDto =
                HenleggBehandlingDto(
                    årsak = HenlagtÅrsak.TRUKKET_TILBAKE,
                    skalSendeHenleggelsesbrev = false,
                )

            every { tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(any()) } throws RuntimeException("Ops!")

            // Act & assert
            val exception =
                assertThrows<RuntimeException> {
                    henleggBehandlingController.henleggBehandling(
                        behandlingId = behandlingId,
                        henleggBehandlingDto = henleggBehandlingDto,
                    )
                }
            assertThat(exception.message).isEqualTo("Ops!")
        }

        @Test
        fun `skal henlegge behandling men ikke lage brev hvis skalSendeHenleggelsesBrev er false`() {
            // Arrange
            val behandlingId = UUID.randomUUID()

            val henleggBehandlingDto =
                HenleggBehandlingDto(
                    årsak = HenlagtÅrsak.TRUKKET_TILBAKE,
                    skalSendeHenleggelsesbrev = false,
                )

            every { henleggBehandlingService.henleggBehandling(behandlingId, henleggBehandlingDto.årsak) } just runs

            // Act
            henleggBehandlingController.henleggBehandling(
                behandlingId = behandlingId,
                henleggBehandlingDto = henleggBehandlingDto,
            )

            // Assert
            verify(exactly = 0) { brevService.lagHenleggelsesbrevOgOpprettJournalføringstask(any(), any()) }
            verify(exactly = 1) { henleggBehandlingService.henleggBehandling(behandlingId, henleggBehandlingDto.årsak) }
        }

        @Test
        fun `skal henlegge behandling og lage brev hvis skalSendeHenleggelsesBrev er true`() {
            // Arrange
            val behandlingId = UUID.randomUUID()

            val henleggBehandlingDto =
                HenleggBehandlingDto(
                    årsak = HenlagtÅrsak.TRUKKET_TILBAKE,
                    skalSendeHenleggelsesbrev = true,
                    nyeBrevmottakere =
                        listOf(
                            DtoTestUtil.lagNyBrevmottakerPersonMedIdentDto(mottakerRolle = MottakerRolle.BRUKER),
                        ),
                )

            every { brevService.lagHenleggelsesbrevOgOpprettJournalføringstask(eq(behandlingId), any()) } just runs
            every { henleggBehandlingService.henleggBehandling(behandlingId, henleggBehandlingDto.årsak) } just runs

            // Act
            henleggBehandlingController.henleggBehandling(
                behandlingId = behandlingId,
                henleggBehandlingDto = henleggBehandlingDto,
            )

            // Assert
            verify(exactly = 1) { brevService.lagHenleggelsesbrevOgOpprettJournalføringstask(any(), any()) }
            verify(exactly = 1) { henleggBehandlingService.henleggBehandling(behandlingId, henleggBehandlingDto.årsak) }
        }

        @Test
        fun `skal kaste feil hvis dto validering feiler`() {
            // Arrange
            val behandlingId = UUID.randomUUID()

            val henleggBehandlingDto =
                HenleggBehandlingDto(
                    årsak = HenlagtÅrsak.FEILREGISTRERT,
                    skalSendeHenleggelsesbrev = true,
                    nyeBrevmottakere = emptyList(),
                )

            every { henleggBehandlingValidator.validerHenleggBehandlingDto(behandlingId, henleggBehandlingDto) } throws ApiFeil.badRequest("Ops! En feil oppstod.")

            // Act & assert
            val exception =
                assertThrows<ApiFeil> {
                    henleggBehandlingController.henleggBehandling(
                        behandlingId = behandlingId,
                        henleggBehandlingDto = henleggBehandlingDto,
                    )
                }
            assertThat(exception.message).isEqualTo("Ops! En feil oppstod.")
        }
    }
}
