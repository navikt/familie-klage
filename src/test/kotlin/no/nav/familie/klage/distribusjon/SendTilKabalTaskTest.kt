package no.nav.familie.klage.distribusjon

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.http.client.RessursException
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.brevmottaker.domain.Brevmottakere
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.kabal.KabalService
import no.nav.familie.klage.vurdering.VurderingService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException.Conflict
import java.util.UUID
import kotlin.test.Test

class SendTilKabalTaskTest {

    private val fagsakService: FagsakService = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val kabalService: KabalService = mockk()
    private val vurderingService: VurderingService = mockk()
    private val brevService: BrevService = mockk()

    private lateinit var sendTilKabalTask: SendTilKabalTask

    @BeforeEach
    fun setup() {
        sendTilKabalTask = SendTilKabalTask(
            fagsakService,
            behandlingService,
            kabalService,
            vurderingService,
            brevService,
        )
    }

    @Test
    fun `skal ikke kaste unntak ved 409 fra Kabal`() {
        val behandlingId = UUID.randomUUID()
        val task = Task(
            type = SendTilKabalTask.TYPE,
            payload = behandlingId.toString(),
        )

        every { behandlingService.hentBehandling(behandlingId) } returns mockk()
        every { fagsakService.hentFagsakForBehandling(behandlingId) } returns mockk()
        every { vurderingService.hentVurdering(behandlingId) } returns mockk()
        every { brevService.hentBrevmottakere(behandlingId) } returns Brevmottakere()

        every { kabalService.sendTilKabal(any(), any(), any(), any(), any()) } throws RessursException(
            cause = Conflict.create(HttpStatus.CONFLICT, "", HttpHeaders(), byteArrayOf(), null),
            ressurs = Ressurs.failure("feil"),
            httpStatus = HttpStatus.CONFLICT,
        )
        assertDoesNotThrow { sendTilKabalTask.doTask(task) }
    }
}
