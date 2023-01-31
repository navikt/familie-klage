package no.nav.familie.klage.behandling

import io.mockk.every
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype
import no.nav.familie.klage.behandling.dto.PåklagetVedtakDto
import no.nav.familie.klage.behandling.dto.tilPåklagetVedtakDetaljer
import no.nav.familie.klage.infrastruktur.config.FamilieEFSakClientMock
import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.integrasjoner.FamilieEFSakClient
import no.nav.familie.klage.testutil.BrukerContextUtil
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import no.nav.familie.klage.testutil.DomainUtil.fagsystemVedtak
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.FagsystemType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime.now

internal class BehandlingServiceIntegrasjonTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var efSakClientMock: FamilieEFSakClient

    private val fagsak = fagsak()
    private val behandling = behandling(fagsak)

    @BeforeEach
    internal fun setUp() {
        BrukerContextUtil.mockBrukerContext()
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagreBehandling(behandling)
    }

    @AfterEach
    internal fun tearDown() {
        FamilieEFSakClientMock.resetMock(efSakClientMock)
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    internal fun `skal oppdatere behandlingsresultat og vedtakstidspunkt`() {
        val persistertBehandling = behandlingService.hentBehandling(behandlingId = behandling.id)
        assertThat(persistertBehandling.vedtakDato).isNull()
        assertThat(persistertBehandling.resultat).isEqualTo(BehandlingResultat.IKKE_SATT)

        behandlingService.oppdaterBehandlingMedResultat(behandling.id, BehandlingResultat.IKKE_MEDHOLD, null)
        val oppdatertBehandling = behandlingService.hentBehandling(behandlingId = behandling.id)
        assertThat(oppdatertBehandling.vedtakDato).isEqualToIgnoringMinutes(now())
        assertThat(oppdatertBehandling.resultat).isEqualTo(BehandlingResultat.IKKE_MEDHOLD)
    }

    @Nested
    inner class oppdaterPåklagetVedtak {

        @Test
        internal fun `skal oppdatere påklaget vedtak`() {
            val påklagetBehandlingId = "påklagetBehandlingId"
            val fagsystemVedtak = fagsystemVedtak(eksternBehandlingId = påklagetBehandlingId)
            every { efSakClientMock.hentVedtak(fagsak.eksternId) } returns listOf(fagsystemVedtak)

            val påklagetVedtak = PåklagetVedtakDto(
                eksternFagsystemBehandlingId = påklagetBehandlingId,
                påklagetVedtakstype = PåklagetVedtakstype.VEDTAK,
            )
            behandlingService.oppdaterPåklagetVedtak(behandlingId = behandling.id, påklagetVedtakDto = påklagetVedtak)
            val oppdatertBehandling = behandlingService.hentBehandling(behandling.id)
            val oppdatertPåklagetVedtak = oppdatertBehandling.påklagetVedtak

            assertThat(oppdatertPåklagetVedtak.påklagetVedtakstype).isEqualTo(påklagetVedtak.påklagetVedtakstype)
            assertThat(oppdatertPåklagetVedtak.påklagetVedtakDetaljer).isEqualTo(fagsystemVedtak.tilPåklagetVedtakDetaljer())
        }

        @Test
        internal fun `skal oppdatere påklaget vedtak som gjelder infotrygd`() {
            val påklagetBehandlingId = "påklagetBehandlingId"
            val fagsystemVedtak = fagsystemVedtak(eksternBehandlingId = påklagetBehandlingId)
            every { efSakClientMock.hentVedtak(fagsak.eksternId) } returns listOf(fagsystemVedtak)

            val vedtaksdatoInfotrygd = LocalDate.now()
            val påklagetVedtak = PåklagetVedtakDto(
                eksternFagsystemBehandlingId = null,
                manuellVedtaksdato = vedtaksdatoInfotrygd,
                påklagetVedtakstype = PåklagetVedtakstype.INFOTRYGD_TILBAKEKREVING,
            )
            behandlingService.oppdaterPåklagetVedtak(behandlingId = behandling.id, påklagetVedtakDto = påklagetVedtak)
            val oppdatertBehandling = behandlingService.hentBehandling(behandling.id)
            val oppdatertPåklagetVedtak = oppdatertBehandling.påklagetVedtak

            assertThat(oppdatertPåklagetVedtak.påklagetVedtakstype).isEqualTo(påklagetVedtak.påklagetVedtakstype)
            assertThat(oppdatertPåklagetVedtak.påklagetVedtakDetaljer?.vedtakstidspunkt?.toLocalDate()).isEqualTo(vedtaksdatoInfotrygd)
            assertThat(oppdatertPåklagetVedtak.påklagetVedtakDetaljer?.fagsystemType).isEqualTo(FagsystemType.TILBAKEKREVING)
        }

        @Test
        internal fun `skal oppdatere påklaget vedtak utestengelse`() {
            val påklagetBehandlingId = "påklagetBehandlingId"
            val fagsystemVedtak = fagsystemVedtak(eksternBehandlingId = påklagetBehandlingId)
            every { efSakClientMock.hentVedtak(fagsak.eksternId) } returns listOf(fagsystemVedtak)

            val vedtaksdato = LocalDate.now()
            val påklagetVedtak = PåklagetVedtakDto(
                eksternFagsystemBehandlingId = null,
                manuellVedtaksdato = vedtaksdato,
                påklagetVedtakstype = PåklagetVedtakstype.UTESTENGELSE,
            )
            behandlingService.oppdaterPåklagetVedtak(behandlingId = behandling.id, påklagetVedtakDto = påklagetVedtak)
            val oppdatertBehandling = behandlingService.hentBehandling(behandling.id)
            val oppdatertPåklagetVedtak = oppdatertBehandling.påklagetVedtak

            assertThat(oppdatertPåklagetVedtak.påklagetVedtakstype).isEqualTo(påklagetVedtak.påklagetVedtakstype)
            assertThat(oppdatertPåklagetVedtak.påklagetVedtakDetaljer?.vedtakstidspunkt?.toLocalDate()).isEqualTo(vedtaksdato)
            assertThat(oppdatertPåklagetVedtak.påklagetVedtakDetaljer?.fagsystemType).isEqualTo(FagsystemType.UTESTENGELSE)
        }

        @Test
        internal fun `skal feile hvis påklaget vedtak ikke finnes`() {
            every { efSakClientMock.hentVedtak(fagsak.eksternId) } returns emptyList()
            val påklagetVedtak =
                PåklagetVedtakDto(eksternFagsystemBehandlingId = "finner ikke", påklagetVedtakstype = PåklagetVedtakstype.VEDTAK)
            assertThatThrownBy {
                behandlingService.oppdaterPåklagetVedtak(behandlingId = behandling.id, påklagetVedtakDto = påklagetVedtak)
            }.hasMessageContaining("Finner ikke vedtak for behandling")
        }
    }
}
