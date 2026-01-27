package no.nav.familie.klage.brevmottaker

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.brev.BrevRepository
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.infrastruktur.repository.findByIdOrThrow
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class BrevmottakerErstatterTest {
    private val behandlingService: BehandlingService = mockk()
    private val brevRepository: BrevRepository = mockk()
    private val brevmottakerErstatter: BrevmottakerErstatter =
        BrevmottakerErstatter(
            behandlingService = behandlingService,
            brevRepository = brevRepository,
        )

    @BeforeEach
    fun setUp() {
        mockkObject(SikkerhetContext)
        every { SikkerhetContext.hentSaksbehandler(any()) } returns "saksbehandler"
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SikkerhetContext)
    }

    @Nested
    inner class ErstattBrevmottakereTest {
        @Test
        fun `skal kaste exception om man prøver å lagre ned tom brevmottakere`() {
            // Arrange
            val behandling = DomainUtil.behandling(steg = StegType.OPPRETTET)

            val nyeBrevmottakere = DomainUtil.lagBrevmottakere()

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    brevmottakerErstatter.erstattBrevmottakere(behandling.id, nyeBrevmottakere)
                }
            assertThat(exception.message).isEqualTo("Må ha minimum en brevmottaker for behandling ${behandling.id}.")
        }

        @Test
        fun `skal kaste exception om behandling er låst for videre redigering`() {
            // Arrange
            val behandling = DomainUtil.behandling(status = BehandlingStatus.FERDIGSTILT)

            val nyeBrevmottakere =
                DomainUtil.lagBrevmottakere(
                    personer =
                        listOf(
                            DomainUtil.lagBrevmottakerPersonMedIdent(personIdent = "1"),
                            DomainUtil.lagBrevmottakerPersonMedIdent(personIdent = "2"),
                        ),
                )

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    brevmottakerErstatter.erstattBrevmottakere(behandling.id, nyeBrevmottakere)
                }
            assertThat(exception.message).isEqualTo("Behandling ${behandling.id} er låst for videre behandling.")
        }

        @Test
        fun `skal kaste exception om behandling ikke er i brev steg`() {
            // Arrange
            val behandling = DomainUtil.behandling(steg = StegType.OPPRETTET)

            val nyeBrevmottakere =
                DomainUtil.lagBrevmottakere(
                    personer =
                        listOf(
                            DomainUtil.lagBrevmottakerPersonMedIdent(personIdent = "1"),
                            DomainUtil.lagBrevmottakerPersonMedIdent(personIdent = "2"),
                        ),
                )

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    brevmottakerErstatter.erstattBrevmottakere(behandling.id, nyeBrevmottakere)
                }
            assertThat(exception.message).isEqualTo("Behandlingen er i steg ${StegType.OPPRETTET}, forventet steg ${StegType.BREV}.")
        }

        @Test
        fun `skal kaste exception det er duplikate identer for person med ident`() {
            // Arrange
            val behandling = DomainUtil.behandling(steg = StegType.BREV)

            val nyeBrevmottakere =
                DomainUtil.lagBrevmottakere(
                    personer =
                        listOf(
                            DomainUtil.lagBrevmottakerPersonMedIdent(personIdent = "1"),
                            DomainUtil.lagBrevmottakerPersonMedIdent(personIdent = "1"),
                        ),
                )

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    brevmottakerErstatter.erstattBrevmottakere(behandling.id, nyeBrevmottakere)
                }
            assertThat(
                exception.message,
            ).isEqualTo("En person kan bare legges til en gang som brevmottaker for behandling ${behandling.id}.")
        }

        @Test
        fun `skal kaste exception det er duplikate ider for person uten ident`() {
            // Arrange
            val id = UUID.randomUUID()

            val behandling = DomainUtil.behandling(steg = StegType.BREV)

            val nyeBrevmottakere =
                DomainUtil.lagBrevmottakere(
                    personer =
                        listOf(
                            DomainUtil.lagBrevmottakerPersonUtenIdent(id = id),
                            DomainUtil.lagBrevmottakerPersonUtenIdent(id = id),
                        ),
                )

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    brevmottakerErstatter.erstattBrevmottakere(behandling.id, nyeBrevmottakere)
                }
            assertThat(
                exception.message,
            ).isEqualTo("En person kan bare legges til en gang som brevmottaker for behandling ${behandling.id}.")
        }

        @Test
        fun `skal kaste exception det er et duplikat orgnr for organisasjoner`() {
            // Arrange
            val behandling = DomainUtil.behandling(steg = StegType.BREV)

            val nyeBrevmottakere =
                DomainUtil.lagBrevmottakere(
                    organisasjoner =
                        listOf(
                            DomainUtil.lagBrevmottakerOrganisasjon(organisasjonsnummer = "123"),
                            DomainUtil.lagBrevmottakerOrganisasjon(organisasjonsnummer = "123"),
                        ),
                )

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    brevmottakerErstatter.erstattBrevmottakere(behandling.id, nyeBrevmottakere)
                }
            assertThat(
                exception.message,
            ).isEqualTo("En organisasjon kan bare legges til en gang som brevmottaker for behandling ${behandling.id}.")
        }

        @Test
        fun `skal kaste exception om brev ikke finnes`() {
            // Arrange
            val behandling = DomainUtil.behandling(steg = StegType.BREV)

            val nyeBrevmottakere =
                DomainUtil.lagBrevmottakere(
                    personer =
                        listOf(
                            DomainUtil.lagBrevmottakerPersonMedIdent(personIdent = "1"),
                            DomainUtil.lagBrevmottakerPersonMedIdent(personIdent = "2"),
                        ),
                )

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            every {
                brevRepository.findByIdOrThrow(behandling.id)
            } throws IllegalStateException("Ops! Noe gikk galt...")

            // Act & assert
            val exception =
                assertThrows<IllegalStateException> {
                    brevmottakerErstatter.erstattBrevmottakere(behandling.id, nyeBrevmottakere)
                }
            assertThat(exception.message).isEqualTo("Ops! Noe gikk galt...")
        }

        @Test
        fun `skal kaste exception om brevmottakerne for brevet ikke finnes`() {
            // Arrange
            val behandling = DomainUtil.behandling(steg = StegType.BREV)

            val nyeBrevmottakere =
                DomainUtil.lagBrevmottakere(
                    personer =
                        listOf(
                            DomainUtil.lagBrevmottakerPersonMedIdent(personIdent = "1"),
                            DomainUtil.lagBrevmottakerPersonMedIdent(personIdent = "2"),
                        ),
                )

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            val brev = DomainUtil.lagBrev(mottakere = null)

            every {
                brevRepository.findByIdOrThrow(behandling.id)
            } returns brev

            every {
                brevRepository.update(any())
            } returns brev

            // Act & assert
            val exception =
                assertThrows<IllegalStateException> {
                    brevmottakerErstatter.erstattBrevmottakere(behandling.id, nyeBrevmottakere)
                }
            assertThat(exception.message).isEqualTo("Fant ikke brevmottakere for behandling ${behandling.id}.")
        }

        @Test
        fun `skal erstatte brevmottakere`() {
            // Arrange
            val behandling = DomainUtil.behandling(steg = StegType.BREV)

            val nyeBrevmottakere =
                DomainUtil.lagBrevmottakere(
                    personer =
                        listOf(
                            DomainUtil.lagBrevmottakerPersonMedIdent(personIdent = "1"),
                            DomainUtil.lagBrevmottakerPersonMedIdent(personIdent = "2"),
                        ),
                )

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            every {
                brevRepository.findByIdOrThrow(behandling.id)
            } returns DomainUtil.lagBrev(mottakere = null)

            every {
                brevRepository.update(any())
            } returnsArgument 0

            // Act
            val brevmottakere = brevmottakerErstatter.erstattBrevmottakere(behandling.id, nyeBrevmottakere)

            // Act & assert
            assertThat(brevmottakere).isEqualTo(nyeBrevmottakere)
        }
    }
}
