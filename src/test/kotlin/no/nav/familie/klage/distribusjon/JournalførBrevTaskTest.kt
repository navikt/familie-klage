package no.nav.familie.klage.distribusjon

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.Properties
import java.util.UUID

internal class JournalførBrevTaskTest {

    val behandlingService = mockk<BehandlingService>()
    val taskRepository = mockk<TaskRepository>()
    val distribusjonService = mockk<DistribusjonService>()

    val journalførBrevTask = JournalførBrevTask(
        distribusjonService = distribusjonService,
        taskRepository = taskRepository,
        behandlingService = behandlingService
    )

    val behandlingId = UUID.randomUUID()
    val journalpostId = "12345678"
    val propertiesMedJournalpostId = Properties().apply {
        this["journalpostId"] = journalpostId
    }

    @Test
    internal fun `skal journalføre og sette journalpostId på tasken`() {
        every { distribusjonService.journalførBrev(any(), any()) } returns journalpostId
        val task = Task(payload = behandlingId.toString(), type = JournalførBrevTask.TYPE)
        journalførBrevTask.doTask(task)
        assertThat(task.metadata["journalpostId"]).isEqualTo(journalpostId)
        verify { distribusjonService.journalførBrev(behandlingId, any()) }
    }

    @Test
    internal fun `skal ikke opprette sendTilKabalTask hvis behandlingen har annen status enn IKKE_MEDHOLD`() {
        val taskSlots = mutableListOf<Task>()
        every { taskRepository.save(capture(taskSlots)) } answers { firstArg() }
        every { behandlingService.hentBehandling(any()) } returns DomainUtil.behandling(resultat = BehandlingResultat.IKKE_MEDHOLD_FORMKRAV_AVVIST)

        journalførBrevTask.onCompletion(Task(JournalførBrevTask.TYPE, behandlingId.toString(), propertiesMedJournalpostId))

        assertThat(taskSlots).hasSize(1)
        assertThat(taskSlots.first().type).isEqualTo(DistribuerBrevTask.TYPE)
    }

    @Test
    internal fun `skal opprette sendTilKabalTask og distribuerBrevTask hvis behandlingsresultatet er IKKE_MEDHOLD`() {
        val taskSlots = mutableListOf<Task>()
        every { taskRepository.save(capture(taskSlots)) } answers { firstArg() }
        every { behandlingService.hentBehandling(any()) } returns DomainUtil.behandling(resultat = BehandlingResultat.IKKE_MEDHOLD)

        journalførBrevTask.onCompletion(Task(JournalførBrevTask.TYPE, behandlingId.toString(), propertiesMedJournalpostId))

        assertThat(taskSlots).hasSize(2)
        assertThat(taskSlots.first().type).isEqualTo(SendTilKabalTask.TYPE)
        assertThat(taskSlots.last().type).isEqualTo(DistribuerBrevTask.TYPE)
    }
}
