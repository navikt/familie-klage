package no.nav.familie.klage.brev.baks

import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BaksBrevRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var baksBrevRepository: BaksBrevRepository

    private val fagsak = fagsak()
    private val behandling = behandling(fagsak)

    @BeforeEach
    internal fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagreBehandling(behandling)
    }

    @Nested
    inner class ExistsByBehandlingIdTest {
        @Test
        fun `skal returnere true om brev finnes for behandling`() {
            // Arrange
            val detachedBaksBrev = DomainUtil.lagBaksBrev(behandlingId = behandling.id)

            baksBrevRepository.insert(detachedBaksBrev)

            // Act
            val eksisterer = baksBrevRepository.existsByBehandlingId(behandling.id)

            // Assert
            assertThat(eksisterer).isTrue()
        }

        @Test
        fun `skal returnere false om brev ikke finnes for behandling`() {
            // Act
            val eksisterer = baksBrevRepository.existsByBehandlingId(behandling.id)

            // Assert
            assertThat(eksisterer).isFalse()
        }
    }
}
