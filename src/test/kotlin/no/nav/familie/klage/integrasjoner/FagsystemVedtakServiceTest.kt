package no.nav.familie.klage.integrasjoner

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.kontrakter.felles.klage.IkkeOpprettetÅrsak
import no.nav.familie.kontrakter.felles.klage.OpprettRevurderingResponse
import no.nav.familie.kontrakter.felles.klage.Opprettet
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class FagsystemVedtakServiceTest {
    private val efSakClient = mockk<FamilieEFSakClient>()
    private val ksSakClient = mockk<FamilieKSSakClient>()
    private val baSakClient = mockk<FamilieBASakClient>()
    private val fagsakService = mockk<FagsakService>()
    private val fagsystemVedtakService =
        FagsystemVedtakService(
            familieEFSakClient = efSakClient,
            familieKSSakClient = ksSakClient,
            familieBASakClient = baSakClient,
            fagsakService = fagsakService,
        )

    private val fagsakEF = DomainUtil.fagsak(stønadstype = Stønadstype.OVERGANGSSTØNAD)
    private val fagsakBA = DomainUtil.fagsak(stønadstype = Stønadstype.BARNETRYGD)
    private val fagsakKS = DomainUtil.fagsak(stønadstype = Stønadstype.KONTANTSTØTTE)

    private val behandlingEF = DomainUtil.behandling(fagsakEF)
    private val behandlingBA = DomainUtil.behandling(fagsakBA)
    private val behandlingKS = DomainUtil.behandling(fagsakKS)

    private val vedtak = DomainUtil.fagsystemVedtak("påklagetBehandlingId")

    @BeforeEach
    fun setUp() {
        every { fagsakService.hentFagsakForBehandling(behandlingEF.id) } returns fagsakEF
        every { fagsakService.hentFagsakForBehandling(behandlingBA.id) } returns fagsakBA
        every { fagsakService.hentFagsakForBehandling(behandlingKS.id) } returns fagsakKS

        every { efSakClient.hentVedtak(fagsakEF.eksternId) } returns listOf(vedtak)
        every { ksSakClient.hentVedtak(fagsakKS.eksternId) } returns listOf(vedtak)
        every { baSakClient.hentVedtak(fagsakBA.eksternId) } returns listOf(vedtak)
    }

    @Nested
    inner class HentFagsystemVedtak {
        @Test
        fun `skal kalle på ef-klient for ef-behandling`() {
            // Act
            fagsystemVedtakService.hentFagsystemVedtak(behandlingEF.id)

            // Assert
            verify { efSakClient.hentVedtak(any()) }
        }

        @Test
        fun `skal kalle på ba-klient for ba-behandling`() {
            // Act
            fagsystemVedtakService.hentFagsystemVedtak(behandlingBA.id)

            // Assert
            verify { baSakClient.hentVedtak(any()) }
        }

        @Test
        fun `skal kalle på ks-klient for ks-behandling`() {
            // Act
            fagsystemVedtakService.hentFagsystemVedtak(behandlingKS.id)

            // Assert
            verify { ksSakClient.hentVedtak(any()) }
        }
    }

    @Nested
    inner class HentFagsystemVedtakForPåklagetBehandlingId {
        @Test
        fun `skal returnere fagsystemVedtak`() {
            // Act
            val fagsystemVedtak = fagsystemVedtakService.hentFagsystemVedtakForPåklagetBehandlingId(behandlingEF.id, "påklagetBehandlingId")

            // Assert
            assertThat(fagsystemVedtak).isNotNull
            verify { efSakClient.hentVedtak(any()) }
        }

        @Test
        fun `skal kaste feil hvis fagsystemVedtak ikke finnes med forventet eksternBehandlingId`() {
            // Act & assert
            assertThatThrownBy {
                fagsystemVedtakService.hentFagsystemVedtakForPåklagetBehandlingId(behandlingEF.id, "finnes ikke")
            }.hasMessageContaining("Finner ikke vedtak for behandling")
        }
    }

    @Nested
    inner class OpprettRevurdering {
        @Test
        fun `skal opprette revurdering for EF`() {
            // Arrange
            val eksternBehandlingId = UUID.randomUUID().toString()
            val opprettet = Opprettet(eksternBehandlingId)
            every { efSakClient.opprettRevurdering(fagsakEF.eksternId) } returns OpprettRevurderingResponse(opprettet)

            // Act
            val opprettRevurderingResponse = fagsystemVedtakService.opprettRevurdering(behandlingEF)

            // Assert
            verify { efSakClient.opprettRevurdering(fagsakEF.eksternId) }
            assertThat(opprettRevurderingResponse.opprettetBehandling).isTrue()
            assertThat(opprettRevurderingResponse.opprettet).isEqualTo(opprettet)
            assertThat(opprettRevurderingResponse.opprettet?.eksternBehandlingId).isEqualTo(eksternBehandlingId)
            assertThat(opprettRevurderingResponse.ikkeOpprettet).isNull()
        }

        @Test
        fun `skal håndtere feil ved oppretting av revurdering for EF`() {
            // Arrange
            every { efSakClient.opprettRevurdering(fagsakEF.eksternId) } throws RuntimeException("Ops! En feil oppstod!")

            // Act
            val opprettRevurderingResponse = fagsystemVedtakService.opprettRevurdering(behandlingEF)

            // Assert
            verify { efSakClient.opprettRevurdering(fagsakEF.eksternId) }
            assertThat(opprettRevurderingResponse.opprettetBehandling).isFalse()
            assertThat(opprettRevurderingResponse.opprettet).isNull()
            assertThat(opprettRevurderingResponse.ikkeOpprettet).isNotNull()
            assertThat(opprettRevurderingResponse.ikkeOpprettet?.årsak).isEqualTo(IkkeOpprettetÅrsak.FEIL)
            assertThat(opprettRevurderingResponse.ikkeOpprettet?.detaljer).isEqualTo("Ukjent feil ved opprettelse av revurdering")
        }

        @Test
        fun `skal opprette revurdering for BA`() {
            // Arrange
            val eksternBehandlingId = UUID.randomUUID().toString()
            val opprettet = Opprettet(eksternBehandlingId)
            every { baSakClient.opprettRevurdering(fagsakBA.eksternId, behandlingBA.eksternBehandlingId) } returns OpprettRevurderingResponse(opprettet)

            // Act
            val opprettRevurderingResponse = fagsystemVedtakService.opprettRevurdering(behandlingBA)

            // Assert
            verify { baSakClient.opprettRevurdering(fagsakBA.eksternId, behandlingBA.eksternBehandlingId) }
            assertThat(opprettRevurderingResponse.opprettetBehandling).isTrue()
            assertThat(opprettRevurderingResponse.opprettet).isEqualTo(opprettet)
            assertThat(opprettRevurderingResponse.opprettet?.eksternBehandlingId).isEqualTo(eksternBehandlingId)
            assertThat(opprettRevurderingResponse.ikkeOpprettet).isNull()
        }

        @Test
        fun `skal håndtere feil ved oppretting av revurdering for BA`() {
            // Arrange
            every { baSakClient.opprettRevurdering(fagsakBA.eksternId, behandlingBA.eksternBehandlingId) } throws RuntimeException("Ops! En feil oppstod!")

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    fagsystemVedtakService.opprettRevurdering(behandlingBA)
                }
            assertThat(exception.message).isEqualTo(
                "Feilet opprettelse av revurdering for behandling=${behandlingBA.id} eksternFagsakId=${fagsakBA.eksternId}",
            )
        }

        @Test
        fun `skal opprette revurdering for KS`() {
            // Arrange
            val eksternBehandlingId = UUID.randomUUID().toString()
            val opprettet = Opprettet(eksternBehandlingId)
            every { ksSakClient.opprettRevurdering(fagsakKS.eksternId, behandlingKS.eksternBehandlingId) } returns OpprettRevurderingResponse(opprettet)

            // Act
            val opprettRevurderingResponse = fagsystemVedtakService.opprettRevurdering(behandlingKS)

            // Assert
            verify { ksSakClient.opprettRevurdering(fagsakKS.eksternId, behandlingKS.eksternBehandlingId) }
            assertThat(opprettRevurderingResponse.opprettetBehandling).isTrue()
            assertThat(opprettRevurderingResponse.opprettet).isEqualTo(opprettet)
            assertThat(opprettRevurderingResponse.opprettet?.eksternBehandlingId).isEqualTo(eksternBehandlingId)
            assertThat(opprettRevurderingResponse.ikkeOpprettet).isNull()
        }

        @Test
        fun `skal håndtere feil ved oppretting av revurdering for KS`() {
            // Arrange
            every { ksSakClient.opprettRevurdering(fagsakKS.eksternId, behandlingKS.eksternBehandlingId) } throws RuntimeException("Ops! En feil oppstod!")

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    fagsystemVedtakService.opprettRevurdering(behandlingKS)
                }
            assertThat(exception.message).isEqualTo(
                "Feilet opprettelse av revurdering for behandling=${behandlingKS.id} eksternFagsakId=${fagsakKS.eksternId}",
            )
        }
    }
}
