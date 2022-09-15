package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.fagsak.domain.PersonIdent
import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.repository.findByIdOrThrow
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsakDomain
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class BehandlingRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var behandlingRepository: BehandlingRepository

    val fagsak = fagsakDomain().tilFagsakMedPerson(setOf(PersonIdent("1")))

    @BeforeEach
    fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
    }

    @Test
    fun insertBehandling() {

        val id = UUID.randomUUID()

        val behandling = behandlingRepository.insert(
            behandling(
                id = id,
                fagsakId = fagsak.id,
                eksternBehandlingId = "123",
                klageMottatt = LocalDate.now()
            )
        )

        val hentetBehandling = behandlingRepository.findByIdOrThrow(id)

        assertThat(behandling.id).isEqualTo(hentetBehandling.id)
        assertThat(behandling.fagsakId).isEqualTo(hentetBehandling.fagsakId)
        assertThat(behandling.eksternBehandlingId).isEqualTo(hentetBehandling.eksternBehandlingId)
        assertThat(behandling.klageMottatt).isEqualTo(hentetBehandling.klageMottatt)
        assertThat(behandling.resultat).isEqualTo(hentetBehandling.resultat)
        assertThat(behandling.sporbar.opprettetAv).isEqualTo(hentetBehandling.sporbar.opprettetAv)
        assertThat(behandling.sporbar.opprettetTid).isEqualTo(hentetBehandling.sporbar.opprettetTid)
        assertThat(behandling.sporbar.endret.endretTid).isEqualTo(hentetBehandling.sporbar.endret.endretTid)
        assertThat(behandling.sporbar.endret.endretAv).isEqualTo(hentetBehandling.sporbar.endret.endretAv)
    }

    @Test
    fun updateStatus() {

        val id = UUID.randomUUID()

        val behandling = behandlingRepository.insert(behandling(id, fagsakId = fagsak.id))

        assertThat(behandling.status).isEqualTo(BehandlingStatus.OPPRETTET)

        val nyStatus = BehandlingStatus.UTREDES

        behandlingRepository.updateStatus(id, nyStatus)

        assertThat(behandlingRepository.findByIdOrThrow(id).status).isEqualTo(nyStatus)
    }

    @Test
    fun updateSteg() {

        val id = UUID.randomUUID()

        val behandling = behandlingRepository.insert(behandling(id, fagsakId = fagsak.id))

        assertThat(behandling.steg).isEqualTo(StegType.FORMKRAV)

        val nyttSteg = StegType.VURDERING
        behandlingRepository.updateSteg(id, nyttSteg)

        assertThat(behandlingRepository.findByIdOrThrow(id).steg).isEqualTo(nyttSteg)
    }

    @Nested
    inner class FinnBehandlingerPåFagsystemOgEksternId {

        @Test
        internal fun `skal returnere tom liste når det ikke finnes noen behandlinger`() {
            assertThat(behandlingRepository.finnBehandlinger(fagsak.eksternId, Fagsystem.EF))
                .isEmpty()
        }

        @Test
        internal fun `skal returnere tom liste når det kun finnes behandlinger på en annen fagsak`() {
            val fagsak2 = testoppsettService.lagreFagsak(fagsakDomain().tilFagsakMedPerson(setOf(PersonIdent("2"))))
            behandlingRepository.insert(behandling(fagsakId = fagsak2.id))

            assertThat(behandlingRepository.finnBehandlinger(fagsak.eksternId, Fagsystem.EF))
                .isEmpty()
        }

        @Test
        internal fun `skal returnere tom liste når det kun finnes behandlinger et annet fagsystem`() {
            behandlingRepository.insert(behandling(fagsakId = fagsak.id))

            assertThat(behandlingRepository.finnBehandlinger(fagsak.eksternId, Fagsystem.BA))
                .isEmpty()
        }

        @Test
        internal fun `skal finne alle behandlinger for eksternFagsakId`() {
            val behandling = behandlingRepository.insert(behandling(fagsakId = fagsak.id))
            val behandling2 = behandlingRepository.insert(behandling(fagsakId = fagsak.id))

            val behandlinger = behandlingRepository.finnBehandlinger(fagsak.eksternId, Fagsystem.EF)
            assertThat(behandlinger).hasSize(2)
            assertThat(behandlinger.map { it.id }).containsExactlyInAnyOrder(behandling.id, behandling2.id)
        }

        @Test
        internal fun `skal finne mappe verdier fra repository til klageBehandling`() {
            val behandling = behandlingRepository.insert(behandling(fagsakId = fagsak.id))
            val behandling2 = behandlingRepository.insert(behandling(fagsakId = fagsak.id))

            val behandlinger = behandlingRepository.finnBehandlinger(fagsak.eksternId, Fagsystem.EF)
            assertThat(behandlinger).hasSize(2)
            assertThat(behandlinger.map { it.id }).containsExactlyInAnyOrder(behandling.id, behandling2.id)
        }

    }
}
