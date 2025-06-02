package no.nav.familie.klage.henlegg

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.klage.brevmottaker.BrevmottakerService
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle
import no.nav.familie.klage.infrastruktur.exception.ApiFeil
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.klage.infrastruktur.featuretoggle.Toggle
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.klage.testutil.DtoTestUtil
import no.nav.familie.kontrakter.felles.klage.HenlagtÅrsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class HenleggBehandlingValidatorTest {
    private val brevmottakerService = mockk<BrevmottakerService>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val henleggBehandlingValidator =
        HenleggBehandlingValidator(
            brevmottakerService = brevmottakerService,
            featureToggleService = featureToggleService,
        )

    @BeforeEach
    internal fun setUp() {
        every { featureToggleService.isEnabled(any()) } returns true
    }

    @Nested
    inner class ValiderHenleggBehandlingDto {
        @Test
        fun `skal kaste feil om årsak er feilregistrert men skalSendeHenleggelsesbrev er true og toggle er skrudd av`() {
            // Arrange
            val behandlingId = UUID.randomUUID()

            val henleggBehandlingDto =
                HenleggBehandlingDto(
                    årsak = HenlagtÅrsak.FEILREGISTRERT,
                    skalSendeHenleggelsesbrev = true,
                    nyeBrevmottakere =
                        listOf(
                            DtoTestUtil.lagNyBrevmottakerPersonMedIdentDto(
                                personIdent = "1",
                                mottakerRolle = MottakerRolle.BRUKER,
                                navn = "navn",
                            ),
                        ),
                )

            every { featureToggleService.isEnabled(Toggle.BRUK_NY_HENLEGG_BEHANDLING_MODAL) } returns false

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    henleggBehandlingValidator.validerHenleggBehandlingDto(behandlingId, henleggBehandlingDto)
                }
            assertThat(exception.message).isEqualTo("Skal ikke sende brev hvis type er ulik trukket tilbake")
        }

        @Test
        fun `skal ikke kaste feil om dto er OK og toggle er skrudd av`() {
            // Arrange
            val behandlingId = UUID.randomUUID()

            val henleggBehandlingDto =
                HenleggBehandlingDto(
                    årsak = HenlagtÅrsak.FEILREGISTRERT,
                    skalSendeHenleggelsesbrev = false,
                    nyeBrevmottakere = emptyList(),
                )

            every { featureToggleService.isEnabled(Toggle.BRUK_NY_HENLEGG_BEHANDLING_MODAL) } returns false

            // Act & assert
            assertDoesNotThrow {
                henleggBehandlingValidator.validerHenleggBehandlingDto(behandlingId, henleggBehandlingDto)
            }
        }

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
