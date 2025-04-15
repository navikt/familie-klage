package no.nav.familie.klage.distribusjon

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.brevmottaker.domain.Brevmottakere
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.felles.util.TaskMetadata.saksbehandlerMetadataKey
import no.nav.familie.klage.kabal.KabalService
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsakDomain
import no.nav.familie.klage.testutil.DomainUtil.lagBrevmottakere
import no.nav.familie.klage.testutil.DomainUtil.tilFagsak
import no.nav.familie.klage.testutil.DomainUtil.vurdering
import no.nav.familie.klage.vurdering.VurderingService
import no.nav.familie.kontrakter.felles.klage.Klagebehandlingsårsak
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import java.util.Properties
import kotlin.test.Test

internal class SendTilKabalTaskTest {

    private val fagsakService: FagsakService = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val kabalService: KabalService = mockk()
    private val vurderingService: VurderingService = mockk()
    private val brevService: BrevService = mockk()

    private val sendTilKabalTask = SendTilKabalTask(
        fagsakService = fagsakService,
        behandlingService = behandlingService,
        kabalService = kabalService,
        vurderingService = vurderingService,
        brevService = brevService,
    )

    @Test
    internal fun `skal ikke kaste unntak ved 409 fra Kabal`() {
        val behandling = behandling(årsak = Klagebehandlingsårsak.ORDINÆR)
        val task = Task(
            type = SendTilKabalTask.TYPE,
            payload = behandling.id.toString(),
        )

        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { fagsakService.hentFagsakForBehandling(behandling.id) } returns mockk()
        every { vurderingService.hentVurdering(behandling.id) } returns mockk()
        every { brevService.hentBrevmottakere(behandling.id) } returns Brevmottakere()

        every { kabalService.sendTilKabal(any(), any(), any(), any(), any()) } throws HttpClientErrorException(HttpStatus.CONFLICT)
        assertDoesNotThrow { sendTilKabalTask.doTask(task) }
    }

    @Test
    internal fun `skal oversende klage til kabal`() {
        val fagsak = fagsakDomain().tilFagsak()
        val behandling = behandling(fagsak = fagsak, årsak = Klagebehandlingsårsak.ORDINÆR)
        val saksbehandlerIdent = "1"
        val vurdering = vurdering(behandlingId = behandling.id)
        val brevmottakere = lagBrevmottakere()
        val task = Task(
            type = SendTilKabalTask.TYPE,
            payload = behandling.id.toString(),
            properties = Properties().apply {
                this[saksbehandlerMetadataKey] = saksbehandlerIdent
            },
        )

        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { fagsakService.hentFagsakForBehandling(behandling.id) } returns fagsak
        every { vurderingService.hentVurdering(behandling.id) } returns vurdering
        every { brevService.hentBrevmottakere(behandling.id) } returns brevmottakere
        every { kabalService.sendTilKabal(any(), any(), any(), any(), any()) } just Runs

        sendTilKabalTask.doTask(task)

        verify(exactly = 1) { kabalService.sendTilKabal(fagsak, behandling, vurdering, saksbehandlerIdent, brevmottakere) }
    }

    @Test
    internal fun `skal ikke oversende brevmottakere når klagebehandlingsårsak er henvendelse fra kabal`() {
        val fagsak = fagsakDomain().tilFagsak()
        val behandling = behandling(fagsak = fagsak, årsak = Klagebehandlingsårsak.HENVENDELSE_FRA_KABAL)
        val saksbehandlerIdent = "1"
        val vurdering = vurdering(behandlingId = behandling.id)
        val task = Task(
            type = SendTilKabalTask.TYPE,
            payload = behandling.id.toString(),
            properties = Properties().apply {
                this[saksbehandlerMetadataKey] = saksbehandlerIdent
            },
        )

        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { fagsakService.hentFagsakForBehandling(behandling.id) } returns fagsak
        every { vurderingService.hentVurdering(behandling.id) } returns vurdering
        every { kabalService.sendTilKabal(any(), any(), any(), any(), any()) } just Runs

        sendTilKabalTask.doTask(task)

        verify(exactly = 0) { brevService.hentBrevmottakere(any()) }
        verify(exactly = 1) { kabalService.sendTilKabal(fagsak, behandling, vurdering, saksbehandlerIdent, null) }
    }
}
