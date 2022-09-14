package no.nav.familie.klage.behandling

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.klage.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.familie.klage.infrastruktur.config.RolleConfig
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.repository.findByIdOrThrow
import no.nav.familie.klage.testutil.BrukerContextUtil.testWithBrukerContext
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

internal class StegServiceTest {

    val behandlingsRepository = mockk<BehandlingsRepository>()
    val behandlingshistorikkService = mockk<BehandlingshistorikkService>()

    val veilederRolle = "veilederRolle"
    val stegService = StegService(
        behandlingsRepository,
        behandlingshistorikkService,
        RolleConfig(
            beslutterRolle = "",
            saksbehandlerRolle = "",
            veilederRolle = veilederRolle,
            kode6 = "",
            kode7 = ""
        )
    )

    @Test
    fun oppdaterSteg() {
        val behandlingId = UUID.randomUUID()

        val behandling = behandling(id = behandlingId)

        val stegSlot = slot<StegType>()
        val statusSlot = slot<BehandlingStatus>()
        val historikkSlot = slot<Behandlingshistorikk>()

        assertThat(behandling.steg).isEqualTo(StegType.FORMKRAV)

        every { behandlingsRepository.findByIdOrThrow(behandlingId) } returns behandling
        every { behandlingsRepository.updateSteg(behandlingId, capture(stegSlot)) } just Runs
        every { behandlingsRepository.updateStatus(behandlingId, capture(statusSlot)) } just Runs
        every { behandlingshistorikkService.opprettBehandlingshistorikk(capture(historikkSlot)) } returns mockk()

        val nesteSteg = StegType.VURDERING
        stegService.oppdaterSteg(behandlingId, nesteSteg)

        assertThat(stegSlot.captured).isEqualTo(nesteSteg)
        assertThat(statusSlot.captured).isEqualTo(nesteSteg.gjelderStatus)
        assertThat(historikkSlot.captured.behandlingId).isEqualTo(behandling.id)
        assertThat(historikkSlot.captured.steg).isEqualTo(behandling.steg)
    }

    @Test
    fun `skal feile hvis saksbehandler mangler rolle`() {
        every { behandlingsRepository.findByIdOrThrow(any()) } returns mockk()

        testWithBrukerContext(groups = listOf(veilederRolle)) {
            val feil = assertThrows<Feil> { stegService.oppdaterSteg(UUID.randomUUID(), StegType.VURDERING) }

            assertThat(feil.frontendFeilmelding).contains("Saksbehandler har ikke tilgang til å oppdatere behandlingssteg")
        }
    }

    @Test
    fun `skal feile hvis behandling er låst`() {
        every { behandlingsRepository.findByIdOrThrow(any()) } returns behandling(status = BehandlingStatus.FERDIGSTILT)

        val feil = assertThrows<Feil> {
            stegService.oppdaterSteg(
                UUID.randomUUID(),
                StegType.BREV
            )
        }
        assertThat(feil.frontendFeilmelding).contains("Behandlingen er låst for videre behandling")
    }
}
