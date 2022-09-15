package no.nav.familie.klage.distribusjon

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.StegService
import no.nav.familie.klage.behandling.domain.BehandlingStatus
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.formkrav.FormService
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.kabal.KabalService
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.klage.testutil.DomainUtil.tilFagsak
import no.nav.familie.klage.testutil.DomainUtil.vurdering
import no.nav.familie.klage.vurdering.VurderingService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

internal class FerdigstillBehandlingServiceTest {

    val fagsakService = mockk<FagsakService>()
    val behandlingService = mockk<BehandlingService>()
    val distribusjonService = mockk<DistribusjonService>()
    val kabalService = mockk<KabalService>()
    val klageresultatService = mockk<KlageresultatService>(relaxed = true)
    val vurderingService = mockk<VurderingService>()
    val formService = mockk<FormService>()
    val stegService = mockk<StegService>()

    val ferdigstillBehandlingService = FerdigstillBehandlingService(
        fagsakService = fagsakService,
        behandlingService = behandlingService,
        distribusjonService = distribusjonService,
        kabalService = kabalService,
        klageresultatService = klageresultatService,
        vurderingService = vurderingService,
        formService = formService,
        stegService = stegService
    )
    val fagsak = DomainUtil.fagsakDomain().tilFagsak()
    val behandling = DomainUtil.behandling(fagsakId = fagsak.id, steg = StegType.BREV, status = BehandlingStatus.UTREDES)
    val vurdering = vurdering(behandlingId = behandling.id)
    val journalpostId = "1234"
    val distribusjonId = "9876"

    @BeforeEach
    internal fun setUp() {
        every { behandlingService.hentBehandling(any()) } returns behandling
        every { fagsakService.hentFagsakForBehandling(any()) } returns fagsak
        every { klageresultatService.hentEllerOpprettKlageresultat(any()) } returns Klageresultat(behandlingId = behandling.id)
        every { distribusjonService.journalførBrev(any()) } returns journalpostId
        every { distribusjonService.distribuerBrev(any()) } returns distribusjonId
        every { vurderingService.hentVurdering(any()) } returns vurdering
        every { kabalService.sendTilKabal(any(), any(), any()) } just Runs
        every { stegService.oppdaterSteg(any(), any()) } just Runs
        every { formService.formkravErOppfyltForBehandling(any()) } returns true
        every { vurderingService.klageTasIkkeTilFølge(any()) } returns true
    }

    @Test
    internal fun `skal ferdigstille behandling`() {
        val stegSlot = slot<StegType>()
        every { stegService.oppdaterSteg(any(), capture(stegSlot)) } just Runs
        ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId = behandling.id)

        assertThat(stegSlot.captured).isEqualTo(StegType.OVERFØRING_TIL_KABAL)
        verify { kabalService.sendTilKabal(fagsak, behandling, vurdering) }
    }

    @Test
    internal fun `skal ikke journalføre på nytt hvis den allerede er journalført`() {
        val stegSlot = slot<StegType>()
        every { klageresultatService.hentEllerOpprettKlageresultat(any()) } returns Klageresultat(
            behandlingId = behandling.id,
            journalpostId = journalpostId
        )
        every { stegService.oppdaterSteg(any(), capture(stegSlot)) } just Runs
        ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId = behandling.id)

        assertThat(stegSlot.captured).isEqualTo(StegType.OVERFØRING_TIL_KABAL)
        verify { kabalService.sendTilKabal(fagsak, behandling, vurdering) }
        verify(exactly = 0) { distribusjonService.journalførBrev(any()) }
    }

    @Test
    internal fun `skal ikke distribuere på nytt hvis den allerede er distribuert`() {
        val stegSlot = slot<StegType>()
        every { klageresultatService.hentEllerOpprettKlageresultat(any()) } returns Klageresultat(
            behandlingId = behandling.id,
            journalpostId = journalpostId,
            distribusjonId = distribusjonId
        )
        every { stegService.oppdaterSteg(any(), capture(stegSlot)) } just Runs
        ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId = behandling.id)

        assertThat(stegSlot.captured).isEqualTo(StegType.OVERFØRING_TIL_KABAL)
        verify { kabalService.sendTilKabal(fagsak, behandling, vurdering) }
        verify(exactly = 0) { distribusjonService.journalførBrev(any()) }
        verify(exactly = 0) { distribusjonService.distribuerBrev(any()) }
    }

    @Test
    internal fun `skal ikke sende til kabal på nytt hvis den allerede er oversendt`() {
        val stegSlot = slot<StegType>()
        every { klageresultatService.hentEllerOpprettKlageresultat(any()) } returns Klageresultat(
            behandlingId = behandling.id,
            journalpostId = journalpostId,
            distribusjonId = distribusjonId,
            oversendtTilKabalTidspunkt = LocalDateTime.now()
        )
        every { stegService.oppdaterSteg(any(), capture(stegSlot)) } just Runs
        ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId = behandling.id)

        assertThat(stegSlot.captured).isEqualTo(StegType.OVERFØRING_TIL_KABAL)
        verify(exactly = 0) { distribusjonService.journalførBrev(any()) }
        verify(exactly = 0) { distribusjonService.distribuerBrev(any()) }
        verify(exactly = 0) { kabalService.sendTilKabal(any(), any(), any()) }
    }

    @Test
    internal fun `skal ikke sende til kabal hvis formkrav ikke er oppfylt`() {
        val stegSlot = slot<StegType>()
        every { stegService.oppdaterSteg(any(), capture(stegSlot)) } just Runs
        every { formService.formkravErOppfyltForBehandling(any()) } returns false
        ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId = behandling.id)
        assertThat(stegSlot.captured).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)

        verify(exactly = 1) { distribusjonService.journalførBrev(any()) }
        verify(exactly = 1) { distribusjonService.distribuerBrev(any()) }
        verify(exactly = 0) { kabalService.sendTilKabal(any(), any(), any()) }
    }

    @Test
    internal fun `skal ikke sende til kabal hvis klage tas til følge`() {
        val stegSlot = slot<StegType>()
        every { stegService.oppdaterSteg(any(), capture(stegSlot)) } just Runs
        every { vurderingService.klageTasIkkeTilFølge(any()) } returns false
        ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId = behandling.id)
        assertThat(stegSlot.captured).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)

        verify(exactly = 1) { distribusjonService.journalførBrev(any()) }
        verify(exactly = 1) { distribusjonService.distribuerBrev(any()) }
        verify(exactly = 0) { kabalService.sendTilKabal(any(), any(), any()) }
    }

    @Test
    internal fun `skal feile dersom behandlingen er på feil steg`() {
        listOf(
            StegType.BEHANDLING_FERDIGSTILT,
            StegType.FORMKRAV,
            StegType.OVERFØRING_TIL_KABAL,
            StegType.VURDERING
        ).forEach { steg ->
            every { behandlingService.hentBehandling(any()) } returns behandling.copy(steg = steg)
            assertThrows<Feil> {
                ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId = behandling.id)
            }
        }
    }

    @Test
    internal fun `skal feile dersom behandlingen har feil status`() {
        listOf(
            BehandlingStatus.FERDIGSTILT,
            BehandlingStatus.VENTER
        ).forEach { status ->
            every { behandlingService.hentBehandling(any()) } returns behandling.copy(status = status)
            assertThrows<Feil> {
                ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId = behandling.id)
            }
        }
    }
}
