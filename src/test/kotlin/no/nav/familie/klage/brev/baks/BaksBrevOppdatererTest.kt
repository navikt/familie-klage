package no.nav.familie.klage.brev.baks

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.brev.felles.FamilieDokumentClient
import no.nav.familie.klage.felles.domain.Fil
import no.nav.familie.klage.infrastruktur.exception.Feil
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

class BaksBrevOppdatererTest {
    private val baksBrevRepository: BaksBrevRepository = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val familieDokumentClient: FamilieDokumentClient = mockk()
    private val fritekstbrevHtmlUtleder: FritekstbrevHtmlUtleder = mockk()
    private val baksBrevOppdaterer = BaksBrevOppdaterer(
        baksBrevRepository = baksBrevRepository,
        behandlingService = behandlingService,
        familieDokumentClient = familieDokumentClient,
        fritekstbrevHtmlUtleder = fritekstbrevHtmlUtleder,
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
    inner class BaksBrevOppdatererTest {
        @Test
        fun `skal kaste exception om behandlingen er låst for redigering`() {
            // Arrange
            val behandling = DomainUtil.behandling(
                id = UUID.randomUUID(),
                status = BehandlingStatus.FERDIGSTILT,
                steg = StegType.BREV,
            )

            val baksBrev = DomainUtil.lagBaksBrev(
                behandlingId = behandling.id,
                html = "<html><p>gammelt</p></html>",
                pdf = Fil("gammel data".toByteArray()),
            )

            every { behandlingService.hentBehandling(behandling.id) } returns behandling

            // Act & assert
            val exception = assertThrows<Feil> {
                baksBrevOppdaterer.oppdaterBrev(baksBrev)
            }
            assertThat(exception.message).isEqualTo("Behandlingen ${behandling.id} er låst for videre behandling")
        }

        @Test
        fun `skal kaste exception om behandlingen ikke er i korrekt behandlingssteg`() {
            // Arrange
            val behandling = DomainUtil.behandling(
                id = UUID.randomUUID(),
                status = BehandlingStatus.OPPRETTET,
                steg = StegType.FORMKRAV,
            )

            val baksBrev = DomainUtil.lagBaksBrev(
                behandlingId = behandling.id,
                html = "<html><p>gammelt</p></html>",
                pdf = Fil("gammel data".toByteArray()),
            )

            every { behandlingService.hentBehandling(behandling.id) } returns behandling

            // Act & assert
            val exception = assertThrows<Feil> {
                baksBrevOppdaterer.oppdaterBrev(baksBrev)
            }
            assertThat(exception.message).isEqualTo("Behandlingen er i steg ${behandling.steg}, forventet steg ${StegType.BREV}")
        }

        @Test
        fun `skal kaste exception om det ikke finnes et brev å oppdatere`() {
            // Arrange
            val behandling = DomainUtil.behandling(
                id = UUID.randomUUID(),
                status = BehandlingStatus.OPPRETTET,
                steg = StegType.BREV,
            )

            val baksBrev = DomainUtil.lagBaksBrev(
                behandlingId = behandling.id,
                html = "<html><p>gammelt</p></html>",
                pdf = Fil("gammel data".toByteArray()),
            )

            every { behandlingService.hentBehandling(behandling.id) } returns behandling
            every { baksBrevRepository.existsByBehandlingId(behandling.id) } returns false

            // Act & assert
            val exception = assertThrows<Feil> {
                baksBrevOppdaterer.oppdaterBrev(baksBrev)
            }
            assertThat(exception.message).isEqualTo("Brev finnes ikke for behandling ${behandling.id}. Opprett brevet først.")
        }

        @Test
        fun `skal oppdatere brev`() {
            // Arrange
            val behandling = DomainUtil.behandling(
                id = UUID.randomUUID(),
                status = BehandlingStatus.OPPRETTET,
                steg = StegType.BREV,
            )

            val baksBrev = DomainUtil.lagBaksBrev(
                behandlingId = behandling.id,
                html = "<html><p>gammelt</p></html>",
                pdf = Fil("gammel data".toByteArray()),
            )

            val nyHtml = "<html><p>ny</p></html>"
            val nyPdfBytes = "ny data".toByteArray()

            every { behandlingService.hentBehandling(behandling.id) } returns behandling
            every { baksBrevRepository.existsByBehandlingId(behandling.id) } returns true
            every { fritekstbrevHtmlUtleder.utledFritekstbrevHtml(behandling) } returns nyHtml
            every { familieDokumentClient.genererPdfFraHtml(nyHtml) } returns nyPdfBytes
            every { baksBrevRepository.update(any()) } returnsArgument 0

            // Act
            val oppdatertBrev = baksBrevOppdaterer.oppdaterBrev(baksBrev)

            // Assert
            verify(exactly = 1) { baksBrevRepository.update(any()) }
            assertThat(oppdatertBrev.behandlingId).isEqualTo(behandling.id)
            assertThat(oppdatertBrev.html).isEqualTo(nyHtml)
            assertThat(oppdatertBrev.pdf.bytes).isEqualTo(nyPdfBytes)
            assertThat(oppdatertBrev.sporbar).isNotNull()
        }
    }
}
