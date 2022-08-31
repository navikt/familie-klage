package no.nav.familie.klage.behandling

import no.nav.familie.ef.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.ef.klage.testutil.DomainUtil.behandling
import no.nav.familie.ef.klage.testutil.DomainUtil.fagsakDomain
import no.nav.familie.klage.behandling.domain.BehandlingStatus
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.fagsak.domain.PersonIdent
import no.nav.familie.klage.repository.findByIdOrThrow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class BehandlingsRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var behandlingRepository: BehandlingsRepository

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
}
