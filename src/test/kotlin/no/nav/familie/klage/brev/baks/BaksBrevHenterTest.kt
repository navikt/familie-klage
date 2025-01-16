package no.nav.familie.klage.brev.baks

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.klage.felles.domain.Fil
import no.nav.familie.klage.testutil.DomainUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class BaksBrevHenterTest {
    private val baksBrevRepository: BaksBrevRepository = mockk()
    private val baksBrevHenter: BaksBrevHenter = BaksBrevHenter(
        baksBrevRepository = baksBrevRepository,
    )

    @Nested
    inner class HentBrevTest {
        @Test
        fun `skal hente brev`() {
            // Arrange
            val behandlingId = UUID.randomUUID()
            val html = "<html />"
            val pdf = Fil("data".toByteArray())

            every {
                baksBrevHenter.hentBrev(behandlingId)
            } returns DomainUtil.lagBaksBrev(
                behandlingId = behandlingId,
                html = html,
                pdf = pdf,
            )

            // Act
            val baksBrev = baksBrevHenter.hentBrev(behandlingId)

            // Assert
            assertThat(baksBrev.behandlingId).isEqualTo(behandlingId)
            assertThat(baksBrev.html).isEqualTo(html)
            assertThat(baksBrev.pdf).isEqualTo(pdf)
        }
    }

    @Nested
    inner class HentBrevEllerNullTest {
        @Test
        fun `skal hente brev`() {
            // Arrange
            val behandlingId = UUID.randomUUID()
            val html = "<html />"
            val pdf = Fil("data".toByteArray())

            every {
                baksBrevHenter.hentBrevEllerNull(behandlingId)
            } returns DomainUtil.lagBaksBrev(
                behandlingId = behandlingId,
                html = html,
                pdf = pdf,
            )

            // Act
            val baksBrev = baksBrevHenter.hentBrevEllerNull(behandlingId)

            // Assert
            assertThat(baksBrev).isNotNull()
            assertThat(baksBrev?.behandlingId).isEqualTo(behandlingId)
            assertThat(baksBrev?.html).isEqualTo(html)
            assertThat(baksBrev?.pdf).isEqualTo(pdf)
        }

        @Test
        fun `skal returnere null om ingen brev finnes for behandlingen`() {
            // Arrange
            val behandlingId = UUID.randomUUID()

            every {
                baksBrevHenter.hentBrevEllerNull(behandlingId)
            } returns null

            // Act
            val baksBrev = baksBrevHenter.hentBrevEllerNull(behandlingId)

            // Assert
            assertThat(baksBrev).isNull()
        }
    }
}
