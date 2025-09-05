package no.nav.familie.klage.repository

import no.nav.familie.klage.behandling.BehandlingRepository
import no.nav.familie.klage.fagsak.FagsakRepository
import no.nav.familie.klage.fagsak.domain.PersonIdent
import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import no.nav.familie.klage.testutil.DomainUtil.fagsakDomain
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.relational.core.conversion.DbActionExecutionException
import org.springframework.data.repository.findByIdOrNull
import java.util.UUID
import kotlin.random.Random

internal class FagsakRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Test
    internal fun findByFagsakId() {
        val fagsakPersistert =
            testoppsettService.lagreFagsak(
                fagsakDomain().tilFagsakMedPerson(
                    setOf(
                        PersonIdent("12345678901"),
                        PersonIdent("98765432109"),
                    ),
                ),
            )
        val fagsak = fagsakRepository.findByIdOrNull(fagsakPersistert.id) ?: error("Finner ikke fagsak med id")

        assertThat(fagsak).isNotNull
        assertThat(fagsak.id).isEqualTo(fagsakPersistert.id)
    }

    @Test
    internal fun `skal hente fagsak på behandlingId`() {
        val fagsak = fagsakDomain().tilFagsakMedPerson(setOf(PersonIdent("1")))

        testoppsettService.lagreFagsak(fagsak)
        val behandling = behandlingRepository.insert(behandling(fagsak))

        val fagsakForBehandling = fagsakRepository.finnFagsakForBehandlingId(behandling.id)!!

        assertThat(fagsakForBehandling.id).isEqualTo(fagsak.id)
        assertThat(fagsakForBehandling.eksternId).isEqualTo(fagsak.eksternId)
    }

    @Test
    internal fun findByEksternIdAndFagsystemAndStønadstype() {
        val eksternId = Random.nextInt().toString()
        val fagsystem = Fagsystem.EF
        val stønadstype = Stønadstype.BARNETILSYN

        val lagretFagsak =
            testoppsettService.lagreFagsak(
                fagsakDomain(
                    eksternId = eksternId,
                    fagsystem = fagsystem,
                    stønadstype = stønadstype,
                ).tilFagsakMedPerson(setOf(PersonIdent("1"))),
            )

        val fagsak =
            fagsakRepository.findByEksternIdAndFagsystemAndStønadstype(
                eksternId = eksternId,
                fagsystem = fagsystem,
                stønadstype = stønadstype,
            )!!

        assertThat(lagretFagsak.id).isEqualTo(fagsak.id)
        assertThat(lagretFagsak.eksternId).isEqualTo(fagsak.eksternId)
        assertThat(lagretFagsak.stønadstype).isEqualTo(fagsak.stønadstype)
        assertThat(lagretFagsak.stønadstype).isEqualTo(fagsak.stønadstype)
        assertThat(lagretFagsak.fagsakPersonId).isEqualTo(fagsak.fagsakPersonId)
    }

    @Nested
    inner class InsertAll {
        @Test
        fun `skal kunne lagre to fagsaker fra samme person, stønadstype, og fagsystem men som har forskjellige eksterne fagsak kilder`() {
            // Arrange
            val person = testoppsettService.opprettPerson("01010199999")

            val fagsak1 =
                fagsakDomain(
                    personId = person.id,
                    eksternId = UUID.randomUUID().toString(),
                    stønadstype = Stønadstype.BARNETRYGD,
                    fagsystem = Fagsystem.BA,
                )

            val fagsak2 =
                fagsakDomain(
                    personId = person.id,
                    eksternId = UUID.randomUUID().toString(),
                    stønadstype = Stønadstype.BARNETRYGD,
                    fagsystem = Fagsystem.BA,
                )

            // Act
            val lagret = fagsakRepository.insertAll(listOf(fagsak1, fagsak2))

            // Assert
            assertThat(lagret).hasSize(2)
            assertThat(lagret).anySatisfy {
                assertThat(it.fagsakPersonId).isEqualTo(person.id)
                assertThat(it.eksternId).isEqualTo(fagsak1.eksternId)
                assertThat(it.stønadstype).isEqualTo(Stønadstype.BARNETRYGD)
                assertThat(it.fagsystem).isEqualTo(Fagsystem.BA)
            }
            assertThat(lagret).anySatisfy {
                assertThat(it.fagsakPersonId).isEqualTo(person.id)
                assertThat(it.fagsakPersonId).isEqualTo(person.id)
                assertThat(it.eksternId).isEqualTo(fagsak2.eksternId)
                assertThat(it.stønadstype).isEqualTo(Stønadstype.BARNETRYGD)
                assertThat(it.fagsystem).isEqualTo(Fagsystem.BA)
            }
        }

        @Test
        fun `skal ikke kunne lagre to fagsaker som er helt identiske`() {
            // Arrange
            val person = testoppsettService.opprettPerson("01010199999")
            val eksternId = UUID.randomUUID().toString()

            val fagsak1 =
                fagsakDomain(
                    personId = person.id,
                    eksternId = eksternId,
                    stønadstype = Stønadstype.BARNETRYGD,
                    fagsystem = Fagsystem.BA,
                )

            val fagsak2 =
                fagsakDomain(
                    personId = person.id,
                    eksternId = eksternId,
                    stønadstype = Stønadstype.BARNETRYGD,
                    fagsystem = Fagsystem.BA,
                )

            // Act & asseret
            val exception =
                assertThrows<DbActionExecutionException> {
                    fagsakRepository.insertAll(listOf(fagsak1, fagsak2))
                }
            assertThat(exception.message).contains("Failed to execute InsertRoot")
        }
    }
}
