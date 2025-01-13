package no.nav.familie.klage.brev.baks.brevmottaker

import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BrevmottakerRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var brevmottakerRepository: BrevmottakerRepository

    private val fagsak = fagsak()
    private val behandling = behandling(fagsak)

    @BeforeEach
    internal fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagreBehandling(behandling)
    }

    @Nested
    inner class FindByBehandlingIdTest {
        @Test
        fun `skal finne brevmottakere for behandling`() {
            // Arrange
            val detachedBrevmottaker1 = DomainUtil.lagBrevmottaker(behandlingId = behandling.id)
            val detachedBrevmottaker2 = DomainUtil.lagBrevmottaker(behandlingId = behandling.id)

            brevmottakerRepository.insertAll(listOf(detachedBrevmottaker1, detachedBrevmottaker2))

            // Act
            val brevmottakere = brevmottakerRepository.findByBehandlingId(behandling.id)

            // Assert
            assertThat(brevmottakere).hasSize(2)
            assertThat(brevmottakere).anySatisfy {
                assertThat(it.id).isEqualTo(detachedBrevmottaker1.id)
                assertThat(it.behandlingId).isEqualTo(detachedBrevmottaker1.behandlingId)
                assertThat(it.mottakertype).isEqualTo(detachedBrevmottaker1.mottakertype)
                assertThat(it.navn).isEqualTo(detachedBrevmottaker1.navn)
                assertThat(it.adresselinje1).isEqualTo(detachedBrevmottaker1.adresselinje1)
                assertThat(it.adresselinje2).isEqualTo(detachedBrevmottaker1.adresselinje2)
                assertThat(it.poststed).isEqualTo(detachedBrevmottaker1.poststed)
                assertThat(it.postnummer).isEqualTo(detachedBrevmottaker1.postnummer)
                assertThat(it.landkode).isEqualTo(detachedBrevmottaker1.landkode)
                assertThat(it.sporbar).isNotNull()
            }
            assertThat(brevmottakere).anySatisfy {
                assertThat(it.id).isEqualTo(detachedBrevmottaker2.id)
                assertThat(it.behandlingId).isEqualTo(detachedBrevmottaker2.behandlingId)
                assertThat(it.mottakertype).isEqualTo(detachedBrevmottaker2.mottakertype)
                assertThat(it.navn).isEqualTo(detachedBrevmottaker2.navn)
                assertThat(it.adresselinje1).isEqualTo(detachedBrevmottaker2.adresselinje1)
                assertThat(it.adresselinje2).isEqualTo(detachedBrevmottaker2.adresselinje2)
                assertThat(it.poststed).isEqualTo(detachedBrevmottaker2.poststed)
                assertThat(it.postnummer).isEqualTo(detachedBrevmottaker2.postnummer)
                assertThat(it.landkode).isEqualTo(detachedBrevmottaker2.landkode)
                assertThat(it.sporbar).isNotNull()
            }
        }
    }
}
