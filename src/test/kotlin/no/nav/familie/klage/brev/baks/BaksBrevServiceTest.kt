package no.nav.familie.klage.brev.baks

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.klage.testutil.DomainUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class BaksBrevServiceTest {
    private val baksBrevHenter: BaksBrevHenter = mockk()
    private val baksBrevOppdaterer: BaksBrevOppdaterer = mockk()
    private val baksBrevOppretter: BaksBrevOppretter = mockk()
    private val baksBrevService: BaksBrevService = BaksBrevService(
        baksBrevHenter = baksBrevHenter,
        baksBrevOppretter = baksBrevOppretter,
        baksBrevOppdaterer = baksBrevOppdaterer,
    )

    @Nested
    inner class HentBrevTest {
        @Test
        fun `skal hente brev`() {
            // Arrange
            val behandlingId = UUID.randomUUID()
            val baksBrev = DomainUtil.lagBaksBrev(behandlingId)
            every { baksBrevHenter.hentBrev(behandlingId) } returns baksBrev

            // Act
            val hentetBaksBrev = baksBrevService.hentBrev(behandlingId)

            // Assert
            assertThat(hentetBaksBrev).isEqualTo(baksBrev)
        }
    }

    @Nested
    inner class OpprettEllerOppdaterBrevTest {
        @Test
        fun `skal oppdatere brev`() {
            // Arrange
            val behandlingId = UUID.randomUUID()
            val baksBrev = DomainUtil.lagBaksBrev(behandlingId)
            every { baksBrevHenter.hentBrevEllerNull(behandlingId) } returns baksBrev
            every { baksBrevOppdaterer.oppdaterBrev(any()) } returnsArgument 0

            // Act
            val oppdatertBaksBrev = baksBrevService.opprettEllerOppdaterBrev(behandlingId)

            // Assert
            verify(exactly = 1) { baksBrevOppdaterer.oppdaterBrev(baksBrev) }
            verify(exactly = 0) { baksBrevOppretter.opprettBrev(baksBrev.behandlingId) }
            assertThat(oppdatertBaksBrev).isEqualTo(baksBrev)
        }

        @Test
        fun `skal opprette brev`() {
            // Arrange
            val behandlingId = UUID.randomUUID()
            val baksBrev = DomainUtil.lagBaksBrev(behandlingId)
            every { baksBrevHenter.hentBrevEllerNull(behandlingId) } returns null
            every { baksBrevOppretter.opprettBrev(any()) } returns baksBrev

            // Act
            val opprettetBaksBrev = baksBrevService.opprettEllerOppdaterBrev(behandlingId)

            // Assert
            verify(exactly = 1) { baksBrevOppretter.opprettBrev(baksBrev.behandlingId) }
            verify(exactly = 0) { baksBrevOppdaterer.oppdaterBrev(baksBrev) }
            assertThat(opprettetBaksBrev).isEqualTo(baksBrev)
        }
    }
}
