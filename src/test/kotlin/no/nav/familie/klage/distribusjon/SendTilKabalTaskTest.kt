package no.nav.familie.klage.distribusjon

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandlingsstatistikk.BehandlingsstatistikkHendelse
import no.nav.familie.klage.behandlingsstatistikk.BehandlingsstatistikkTask
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.brevmottaker.domain.Brevmottakere
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.felles.util.TaskMetadata.SAKSBEHANDLER_METADATA_KEY
import no.nav.familie.klage.kabal.KabalService
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsakDomain
import no.nav.familie.klage.testutil.DomainUtil.lagBrevmottakere
import no.nav.familie.klage.testutil.DomainUtil.tilFagsak
import no.nav.familie.klage.testutil.DomainUtil.vurdering
import no.nav.familie.klage.vurdering.VurderingService
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.Klagebehandlingsårsak
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import java.time.LocalDateTime
import java.util.Properties
import kotlin.test.Test

internal class SendTilKabalTaskTest {
    private val fagsakService: FagsakService = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val kabalService: KabalService = mockk()
    private val vurderingService: VurderingService = mockk()
    private val brevService: BrevService = mockk()
    private val taskService: TaskService = mockk(relaxed = true)

    private val sendTilKabalTask =
        SendTilKabalTask(
            fagsakService = fagsakService,
            behandlingService = behandlingService,
            kabalService = kabalService,
            vurderingService = vurderingService,
            brevService = brevService,
            taskService = taskService,
        )

    @Test
    internal fun `skal ikke kaste unntak ved 409 fra Kabal`() {
        val behandling = behandling(årsak = Klagebehandlingsårsak.ORDINÆR)
        val task =
            Task(
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
        val task =
            Task(
                type = SendTilKabalTask.TYPE,
                payload = behandling.id.toString(),
                properties =
                    Properties().apply {
                        this[SAKSBEHANDLER_METADATA_KEY] = saksbehandlerIdent
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
        val task =
            Task(
                type = SendTilKabalTask.TYPE,
                payload = behandling.id.toString(),
                properties =
                    Properties().apply {
                        this[SAKSBEHANDLER_METADATA_KEY] = saksbehandlerIdent
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

    @Test
    internal fun `skal sende statistikk om oversendelse til KA når klagen faktisk er oversendt`() {
        // Arrange
        val fagsak = fagsakDomain().tilFagsak()
        val behandling = behandling(fagsak = fagsak, resultat = BehandlingResultat.IKKE_MEDHOLD)
        val saksbehandlerIdent = "Z999999"
        val statistikkTaskSlot = slot<Task>()
        val task =
            Task(
                type = SendTilKabalTask.TYPE,
                payload = behandling.id.toString(),
                properties =
                    Properties().apply {
                        this[SAKSBEHANDLER_METADATA_KEY] = saksbehandlerIdent
                    },
            )

        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { fagsakService.hentFagsakForBehandling(behandling.id) } returns fagsak
        every { taskService.save(capture(statistikkTaskSlot)) } answers { firstArg() }
        val førOversendelse = LocalDateTime.now()

        // Act
        sendTilKabalTask.onCompletion(task)

        // Assert
        assertThat(statistikkTaskSlot.captured.type).isEqualTo(BehandlingsstatistikkTask.TYPE)
        assertThat(statistikkTaskSlot.captured.metadata["hendelse"])
            .isEqualTo(BehandlingsstatistikkHendelse.SENDT_TIL_KA.name)
        assertThat(statistikkTaskSlot.captured.metadata["saksbehandler"]).isEqualTo(saksbehandlerIdent)
        val hendelseTidspunkt = LocalDateTime.parse(statistikkTaskSlot.captured.metadata["hendelseTidspunkt"].toString())
        assertThat(hendelseTidspunkt).isAfterOrEqualTo(førOversendelse)
    }

    @Test
    internal fun `skal ikke sende statistikk om oversendelse til KA når resultatet ikke er ikke medhold`() {
        // Arrange
        val fagsak = fagsakDomain().tilFagsak()
        val behandling =
            behandling(
                fagsak = fagsak,
                årsak = Klagebehandlingsårsak.HENVENDELSE_FRA_KABAL,
                resultat = BehandlingResultat.IKKE_MEDHOLD_FORMKRAV_AVVIST,
            )
        val task =
            Task(
                type = SendTilKabalTask.TYPE,
                payload = behandling.id.toString(),
                properties =
                    Properties().apply {
                        this[SAKSBEHANDLER_METADATA_KEY] = "Z999999"
                    },
            )

        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { fagsakService.hentFagsakForBehandling(behandling.id) } returns fagsak

        // Act
        sendTilKabalTask.onCompletion(task)

        // Assert
        verify(exactly = 0) { taskService.save(any()) }
    }
}
