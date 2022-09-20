package no.nav.familie.klage.kabal

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.klage.fagsak.domain.PersonIdent
import no.nav.familie.klage.infrastruktur.config.LenkeConfig
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsakDomain
import no.nav.familie.klage.testutil.DomainUtil.vurdering
import no.nav.familie.klage.vurdering.domain.Hjemmel
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class KabalServiceTest {

    val kabalClient = mockk<KabalClient>()
    val lenkeConfig = LenkeConfig(efSakLenke = "BASEURL_EF", baSakLenke = "BASEURL_BA")
    val kabalService = KabalService(kabalClient, lenkeConfig)

    @Test
    fun sendTilKabal() {
        val oversendelseSlot = slot<OversendtKlageAnkeV3>()

        val fagsak = fagsakDomain().tilFagsakMedPerson(setOf(PersonIdent("1")))
        val behandling = behandling(fagsak)
        val hjemmel = Hjemmel.FT_FEMTEN_FIRE
        val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)
        every { kabalClient.sendTilKabal(capture(oversendelseSlot)) } just Runs

        kabalService.sendTilKabal(fagsak, behandling, vurdering)

        assertThat(oversendelseSlot.captured.fagsak?.fagsakId).isEqualTo(fagsak.eksternId)
        assertThat(oversendelseSlot.captured.fagsak?.fagsystem).isEqualTo(Fagsystem.EF)
        assertThat(oversendelseSlot.captured.hjemler).containsAll(listOf(hjemmel.kabalHjemmel))
        assertThat(oversendelseSlot.captured.kildeReferanse).isEqualTo(behandling.eksternBehandlingId.toString())
        assertThat(oversendelseSlot.captured.innsynUrl).isEqualTo("${lenkeConfig.efSakLenke}/fagsak/${fagsak.eksternId}/${behandling.eksternFagsystemBehandlingId}")
        assertThat(oversendelseSlot.captured.forrigeBehandlendeEnhet).isEqualTo(behandling.behandlendeEnhet)
        assertThat(oversendelseSlot.captured.tilknyttedeJournalposter).isEmpty() // TODO: Sjekk for relevante
        assertThat(oversendelseSlot.captured.brukersHenvendelseMottattNavDato).isEqualTo(behandling.klageMottatt)
        assertThat(oversendelseSlot.captured.innsendtTilNav).isEqualTo(behandling.klageMottatt)
        assertThat(oversendelseSlot.captured.klager.id.verdi).isEqualTo(fagsak.hentAktivIdent())
        assertThat(oversendelseSlot.captured.sakenGjelder).isNull()
        assertThat(oversendelseSlot.captured.kilde).isEqualTo(Fagsystem.EF)
        assertThat(oversendelseSlot.captured.ytelse).isEqualTo(Ytelse.ENF_ENF)
        assertThat(oversendelseSlot.captured.kommentar).isNull()
        assertThat(oversendelseSlot.captured.dvhReferanse).isNull()
    }
}
