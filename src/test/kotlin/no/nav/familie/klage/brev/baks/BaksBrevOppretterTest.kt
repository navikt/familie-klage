package no.nav.familie.klage.brev.baks

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.brev.FamilieDokumentClient
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

class BaksBrevOppretterTest {
    private val baksBrevRepository: BaksBrevRepository = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val familieDokumentClient: FamilieDokumentClient = mockk()
    private val fritekstbrevHtmlUtleder: FritekstbrevHtmlUtleder = mockk()
    private val baksBrevOppretter: BaksBrevOppretter = BaksBrevOppretter(
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
    inner class BaksBrevOppretterTest {
        @Test
        fun `skal kaste exception om behandlingen er låst for videre behandling`() {
            // Arrange
            val behandling = DomainUtil.behandling(
                id = UUID.randomUUID(),
                status = BehandlingStatus.FERDIGSTILT,
                steg = StegType.BREV,
            )

            every { behandlingService.hentBehandling(behandling.id) } returns behandling

            // Act & assert
            val exception = assertThrows<Feil> {
                baksBrevOppretter.opprettBrev(behandling.id)
            }
            assertThat(exception.message).isEqualTo("Behandlingen ${behandling.id} er låst for videre behandling")
        }

        @Test
        fun `skal kaste exception om behandlingen ikke er i rett steg`() {
            // Arrange
            val behandling = DomainUtil.behandling(
                id = UUID.randomUUID(),
                status = BehandlingStatus.OPPRETTET,
                steg = StegType.FORMKRAV,
            )

            every { behandlingService.hentBehandling(behandling.id) } returns behandling

            // Act & assert
            val exception = assertThrows<Feil> {
                baksBrevOppretter.opprettBrev(behandling.id)
            }
            assertThat(exception.message).isEqualTo("Behandlingen er i steg ${behandling.steg}, forventet steg ${StegType.BREV}")
        }

        @Test
        fun `skal kaste exception om brev allerde finnes for behandlingen`() {
            // Arrange
            val behandling = DomainUtil.behandling(
                id = UUID.randomUUID(),
                status = BehandlingStatus.OPPRETTET,
                steg = StegType.BREV,
            )

            every { behandlingService.hentBehandling(behandling.id) } returns behandling
            every { baksBrevRepository.existsByBehandlingId(behandling.id) } returns true

            // Act & assert
            val exception = assertThrows<Feil> {
                baksBrevOppretter.opprettBrev(behandling.id)
            }
            assertThat(exception.message).isEqualTo("Brev finnes allerede for behandling ${behandling.id}. Oppdater heller brevet.")
        }

        @Test
        fun `skal opprette brev`() {
            // Arrange
            val behandling = DomainUtil.behandling(
                id = UUID.randomUUID(),
                status = BehandlingStatus.OPPRETTET,
                steg = StegType.BREV,
            )

            val html = "<html><p>data</p></html>"
            val pdfBytes = "data".toByteArray()

            every { behandlingService.hentBehandling(behandling.id) } returns behandling
            every { baksBrevRepository.existsByBehandlingId(behandling.id) } returns false
            every { fritekstbrevHtmlUtleder.utledFritekstbrevHtml(behandling) } returns html
            every { familieDokumentClient.genererPdfFraHtml(html) } returns pdfBytes
            every { baksBrevRepository.insert(any()) } returnsArgument 0

            // Act
            val opprettetBrev = baksBrevOppretter.opprettBrev(behandling.id)

            // Assert
            assertThat(opprettetBrev.behandlingId).isEqualTo(behandling.id)
            assertThat(opprettetBrev.html).isEqualTo(html)
            assertThat(opprettetBrev.pdf.bytes).isEqualTo(pdfBytes)
            assertThat(opprettetBrev.sporbar).isNotNull()
        }
    }
}
