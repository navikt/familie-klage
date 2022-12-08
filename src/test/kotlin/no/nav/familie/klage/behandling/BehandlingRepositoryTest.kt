package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.FagsystemRevurdering
import no.nav.familie.klage.behandling.domain.Opprettet
import no.nav.familie.klage.behandling.domain.PåklagetVedtak
import no.nav.familie.klage.behandling.domain.PåklagetVedtakDetaljer
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.fagsak.domain.PersonIdent
import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.repository.findByIdOrThrow
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsakDomain
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.FagsystemType
import no.nav.familie.kontrakter.felles.klage.HenlagtÅrsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime
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

        val påklagetVedtakDetaljer =
            PåklagetVedtakDetaljer(FagsystemType.ORDNIÆR, "1234", "type", "resultat", LocalDateTime.now())
        val fagsystemRevurdering = FagsystemRevurdering(true, Opprettet("id", LocalDateTime.now()), null)
        val behandling = behandlingRepository.insert(
            behandling(
                fagsak = fagsak,
                id = id,
                klageMottatt = LocalDate.now(),
                påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.VEDTAK, påklagetVedtakDetaljer),
                henlagtÅrsak = HenlagtÅrsak.TRUKKET_TILBAKE,
                fagsystemRevurdering = fagsystemRevurdering
            )
        )

        val hentetBehandling = behandlingRepository.findByIdOrThrow(id)

        assertThat(behandling.id).isEqualTo(hentetBehandling.id)
        assertThat(behandling.fagsakId).isEqualTo(hentetBehandling.fagsakId)
        assertThat(behandling.eksternBehandlingId).isEqualTo(hentetBehandling.eksternBehandlingId)
        assertThat(behandling.påklagetVedtak).isEqualTo(hentetBehandling.påklagetVedtak)
        assertThat(behandling.klageMottatt).isEqualTo(hentetBehandling.klageMottatt)
        assertThat(behandling.resultat).isEqualTo(hentetBehandling.resultat)
        assertThat(behandling.henlagtÅrsak).isEqualTo(HenlagtÅrsak.TRUKKET_TILBAKE)
        assertThat(behandling.sporbar.opprettetAv).isEqualTo(hentetBehandling.sporbar.opprettetAv)
        assertThat(behandling.sporbar.opprettetTid).isEqualTo(hentetBehandling.sporbar.opprettetTid)
        assertThat(behandling.sporbar.endret.endretTid).isEqualTo(hentetBehandling.sporbar.endret.endretTid)
        assertThat(behandling.sporbar.endret.endretAv).isEqualTo(hentetBehandling.sporbar.endret.endretAv)
        assertThat(behandling.fagsystemRevurdering).isEqualTo(hentetBehandling.fagsystemRevurdering)
    }

    @Test
    fun updateStatus() {
        val id = UUID.randomUUID()

        val behandling = behandlingRepository.insert(behandling(fagsak, id))

        assertThat(behandling.status).isEqualTo(BehandlingStatus.OPPRETTET)

        val nyStatus = BehandlingStatus.UTREDES

        behandlingRepository.updateStatus(id, nyStatus)

        assertThat(behandlingRepository.findByIdOrThrow(id).status).isEqualTo(nyStatus)
    }

    @Test
    fun updateSteg() {
        val id = UUID.randomUUID()

        val behandling = behandlingRepository.insert(behandling(fagsak, id))

        assertThat(behandling.steg).isEqualTo(StegType.FORMKRAV)

        val nyttSteg = StegType.VURDERING
        behandlingRepository.updateSteg(id, nyttSteg)

        assertThat(behandlingRepository.findByIdOrThrow(id).steg).isEqualTo(nyttSteg)
    }

    @Test
    internal fun `findByEksternBehandlingIdAndFagsystem - forvent treff`() {
        val fagsakPersistert = testoppsettService.lagreFagsak(
            fagsakDomain().tilFagsakMedPerson(
                setOf(PersonIdent("12345678901"))
            )
        )

        val fagsakPersistert2 = testoppsettService.lagreFagsak(
            fagsakDomain().tilFagsakMedPerson(
                setOf(PersonIdent("12345678902"))
            )
        )

        val behandlingPersistert = behandlingRepository.insert(behandling(fagsakPersistert))
        behandlingRepository.insert(behandling(fagsakPersistert2))

        val behandling = behandlingRepository.findByEksternBehandlingId(behandlingPersistert.eksternBehandlingId)
        assertThat(behandling).isNotNull
        assertThat(behandling.id).isEqualTo(behandlingPersistert.id)
        assertThat(fagsakPersistert.id).isEqualTo(behandling.fagsakId)
    }

    @Nested
    inner class FinnKlagebehandlingsresultat {

        @Test
        internal fun `skal returnere tom liste når det ikke finnes noen behandlinger`() {
            assertThat(behandlingRepository.finnKlagebehandlingsresultat(fagsak.eksternId, Fagsystem.EF))
                .isEmpty()
        }

        @Test
        internal fun `skal returnere tom liste når det kun finnes behandlinger på en annen fagsak`() {
            val fagsak2 = testoppsettService.lagreFagsak(fagsakDomain().tilFagsakMedPerson(setOf(PersonIdent("2"))))
            behandlingRepository.insert(behandling(fagsak2))

            assertThat(behandlingRepository.finnKlagebehandlingsresultat(fagsak.eksternId, Fagsystem.EF))
                .isEmpty()
        }

        @Test
        internal fun `skal returnere tom liste når det kun finnes behandlinger et annet fagsystem`() {
            behandlingRepository.insert(behandling(fagsak))

            assertThat(behandlingRepository.finnKlagebehandlingsresultat(fagsak.eksternId, Fagsystem.BA))
                .isEmpty()
        }

        @Test
        internal fun `skal finne alle behandlinger for eksternFagsakId`() {
            val behandling = behandlingRepository.insert(behandling(fagsak))
            val behandling2 = behandlingRepository.insert(behandling(fagsak))

            val behandlinger = behandlingRepository.finnKlagebehandlingsresultat(fagsak.eksternId, Fagsystem.EF)
            assertThat(behandlinger).hasSize(2)
            assertThat(behandlinger.map { it.id }).containsExactlyInAnyOrder(behandling.id, behandling2.id)
        }

        @Test
        internal fun `skal mappe verdier fra repository til klageBehandling`() {
            val behandling = behandlingRepository.insert(behandling(fagsak))
            val behandling2 = behandlingRepository.insert(behandling(fagsak))

            val behandlinger = behandlingRepository.finnKlagebehandlingsresultat(fagsak.eksternId, Fagsystem.EF)
            assertThat(behandlinger).hasSize(2)
            assertThat(behandlinger.map { it.id }).containsExactlyInAnyOrder(behandling.id, behandling2.id)
        }

        // TODO test som sjekker mapping av verdier
    }
}
