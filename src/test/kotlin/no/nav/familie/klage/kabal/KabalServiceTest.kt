package no.nav.familie.klage.kabal

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.klage.behandling.domain.PåklagetVedtak
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype
import no.nav.familie.klage.fagsak.domain.PersonIdent
import no.nav.familie.klage.infrastruktur.config.LenkeConfig
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsakDomain
import no.nav.familie.klage.testutil.DomainUtil.påklagetVedtakDetaljer
import no.nav.familie.klage.testutil.DomainUtil.vurdering
import no.nav.familie.klage.vurdering.domain.Hjemmel
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.FagsystemType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class KabalServiceTest {

    val kabalClient = mockk<KabalClient>()
    val lenkeConfig = LenkeConfig(efSakLenke = "BASEURL_EF", baSakLenke = "BASEURL_BA")
    val kabalService = KabalService(kabalClient, lenkeConfig)
    val fagsak = fagsakDomain().tilFagsakMedPerson(setOf(PersonIdent("1")))

    val hjemmel = Hjemmel.FT_FEMTEN_FIRE

    val oversendelseSlot = slot<OversendtKlageAnkeV3>()

    @BeforeEach
    internal fun setUp() {
        every { kabalClient.sendTilKabal(capture(oversendelseSlot)) } just Runs
    }

    @Test
    fun sendTilKabal() {
        val påklagetVedtakDetaljer = påklagetVedtakDetaljer()
        val behandling = behandling(fagsak, påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.VEDTAK, påklagetVedtakDetaljer))
        val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)

        kabalService.sendTilKabal(fagsak, behandling, vurdering)

        val oversendelse = oversendelseSlot.captured
        assertThat(oversendelse.fagsak?.fagsakId).isEqualTo(fagsak.eksternId)
        assertThat(oversendelse.fagsak?.fagsystem).isEqualTo(Fagsystem.EF)
        assertThat(oversendelse.hjemler).containsAll(listOf(hjemmel.kabalHjemmel))
        assertThat(oversendelse.kildeReferanse).isEqualTo(behandling.eksternBehandlingId.toString())
        assertThat(oversendelse.innsynUrl)
            .isEqualTo("${lenkeConfig.efSakLenke}/fagsak/${fagsak.eksternId}/${påklagetVedtakDetaljer.eksternFagsystemBehandlingId}")
        assertThat(oversendelse.forrigeBehandlendeEnhet).isEqualTo(behandling.behandlendeEnhet)
        assertThat(oversendelse.tilknyttedeJournalposter).isEmpty() // TODO: Sjekk for relevante
        assertThat(oversendelse.brukersHenvendelseMottattNavDato).isEqualTo(behandling.klageMottatt)
        assertThat(oversendelse.innsendtTilNav).isEqualTo(behandling.klageMottatt)
        assertThat(oversendelse.klager.id.verdi).isEqualTo(fagsak.hentAktivIdent())
        assertThat(oversendelse.sakenGjelder).isNull()
        assertThat(oversendelse.kilde).isEqualTo(Fagsystem.EF)
        assertThat(oversendelse.ytelse).isEqualTo(Ytelse.ENF_ENF)
        assertThat(oversendelse.kommentar).isNull()
        assertThat(oversendelse.dvhReferanse).isNull()
    }

    @Test
    internal fun `skal sette innsynUrl til saksoversikten hvis påklaget vedtakstype gjelder tilbakekreving`() {
        val påklagetVedtakDetaljer = påklagetVedtakDetaljer(fagsystemType = FagsystemType.TILBAKEKREVING)
        val behandling = behandling(fagsak, påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.VEDTAK, påklagetVedtakDetaljer))
        val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)

        kabalService.sendTilKabal(fagsak, behandling, vurdering)

        assertThat(oversendelseSlot.captured.innsynUrl)
            .isEqualTo("${lenkeConfig.efSakLenke}/fagsak/${fagsak.eksternId}/saksoversikt")
    }

    @Test
    internal fun `skal sette innsynUrl til saksoversikten hvis påklaget vedtak ikke er satt`() {
        val behandling = behandling(fagsak, påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.UTEN_VEDTAK))
        val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)

        kabalService.sendTilKabal(fagsak, behandling, vurdering)

        assertThat(oversendelseSlot.captured.innsynUrl)
            .isEqualTo("${lenkeConfig.efSakLenke}/fagsak/${fagsak.eksternId}/saksoversikt")
    }
}
