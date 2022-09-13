package no.nav.familie.ef.klage.repository

import no.nav.familie.ef.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.ef.klage.testutil.DomainUtil.behandling
import no.nav.familie.ef.klage.testutil.DomainUtil.fagsakDomain
import no.nav.familie.klage.behandling.BehandlingsRepository
import no.nav.familie.klage.fagsak.FagsakRepository
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.fagsak.domain.PersonIdent
import no.nav.familie.klage.felles.domain.Endret
import no.nav.familie.klage.felles.domain.Sporbar
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDateTime
import kotlin.random.Random

internal class FagsakRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingsRepository

    @Test
    internal fun findByFagsakId() {
        val fagsakPersistert = testoppsettService.lagreFagsak(
            fagsakDomain().tilFagsakMedPerson(
                setOf(
                    PersonIdent("12345678901"),
                    PersonIdent("98765432109")
                )
            )
        )
        val fagsak = fagsakRepository.findByIdOrNull(fagsakPersistert.id) ?: error("Finner ikke fagsak med id")

        assertThat(fagsak).isNotNull
        assertThat(fagsak.id).isEqualTo(fagsakPersistert.id)
    }

    @Test
    internal fun `skal hente fagsak på behandlingId`() {
        val fagsak = fagsakDomain().tilFagsakMedPerson(setOf(PersonIdent("1")))

        testoppsettService.lagreFagsak(fagsak)
        val behandling = behandlingRepository.insert(behandling(fagsakId = fagsak.id))

        val fagsakForBehandling = fagsakRepository.finnFagsakForBehandling(behandling.id)!!

        assertThat(fagsakForBehandling.id).isEqualTo(fagsak.id)
        assertThat(fagsakForBehandling.eksternId).isEqualTo(fagsak.eksternId)
    }

    @Test
    internal fun findByEksternIdAndFagsystemAndStønadstype() {
        val eksternId = Random.nextInt().toString()
        val fagsystem = Fagsystem.EF
        val stønadstype = Stønadstype.BARNETILSYN

        val lagretFagsak = testoppsettService.lagreFagsak(
            fagsakDomain(
                eksternId = eksternId,
                fagsystem = fagsystem,
                stønadstype = stønadstype
            ).tilFagsakMedPerson(setOf(PersonIdent("1")))
        )

        val fagsak = fagsakRepository.findByEksternIdAndFagsystemAndStønadstype(
            eksternId = eksternId,
            fagsystem = fagsystem,
            stønadstype = stønadstype
        )!!

        assertThat(lagretFagsak.id).isEqualTo(fagsak.id)
        assertThat(lagretFagsak.eksternId).isEqualTo(fagsak.eksternId)
        assertThat(lagretFagsak.stønadstype).isEqualTo(fagsak.stønadstype)
        assertThat(lagretFagsak.stønadstype).isEqualTo(fagsak.stønadstype)
        assertThat(lagretFagsak.fagsakPersonId).isEqualTo(fagsak.fagsakPersonId)
    }

    private fun opprettFagsakMedFlereIdenter(ident: String = "1", ident2: String = "2", ident3: String = "3"): Fagsak {
        val endret2DagerSiden = Sporbar(endret = Endret(endretTid = LocalDateTime.now().plusDays(2)))
        return fagsakDomain().tilFagsakMedPerson(
            setOf(
                PersonIdent(ident = ident),
                PersonIdent(ident = ident2, sporbar = endret2DagerSiden),
                PersonIdent(ident = ident3)
            )
        )
    }
}
