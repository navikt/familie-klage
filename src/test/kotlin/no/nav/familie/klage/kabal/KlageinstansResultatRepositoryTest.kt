package no.nav.familie.klage.kabal

import no.nav.familie.klage.felles.domain.SporbarUtils
import no.nav.familie.klage.infrastruktur.config.DatabaseConfiguration.StringListWrapper
import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.kabal.domain.KlageinstansResultat
import no.nav.familie.klage.repository.findByIdOrThrow
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import no.nav.familie.kontrakter.felles.klage.BehandlingEventType
import no.nav.familie.kontrakter.felles.klage.KlageinstansUtfall
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class KlageinstansResultatRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var klageresultatRepository: KlageresultatRepository

    @Test
    internal fun `skal kunne lagre og hente klageresultat`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagreBehandling(behandling(fagsak))
        val klageinstansResultat =
            KlageinstansResultat(
                eventId = UUID.randomUUID(),
                type = BehandlingEventType.ANKEBEHANDLING_AVSLUTTET,
                utfall = KlageinstansUtfall.OPPHEVET,
                mottattEllerAvsluttetTidspunkt = SporbarUtils.now(),
                kildereferanse = UUID.randomUUID(),
                journalpostReferanser = StringListWrapper(listOf("ref1", "ref2")),
                behandlingId = behandling.id,
            )
        klageresultatRepository.insert(klageinstansResultat)

        val hentetKlageresultat = klageresultatRepository.findByIdOrThrow(klageinstansResultat.eventId)
        assertThat(hentetKlageresultat.eventId).isEqualTo(klageinstansResultat.eventId)
        assertThat(hentetKlageresultat.type).isEqualTo(klageinstansResultat.type)
        assertThat(hentetKlageresultat.utfall).isEqualTo(klageinstansResultat.utfall)
        assertThat(hentetKlageresultat.mottattEllerAvsluttetTidspunkt).isEqualTo(klageinstansResultat.mottattEllerAvsluttetTidspunkt)
        assertThat(hentetKlageresultat.kildereferanse).isEqualTo(klageinstansResultat.kildereferanse)
        assertThat(hentetKlageresultat.journalpostReferanser).isEqualTo(klageinstansResultat.journalpostReferanser)
        assertThat(hentetKlageresultat.journalpostReferanser.verdier).hasSize(2)
        assertThat(hentetKlageresultat.behandlingId).isEqualTo(klageinstansResultat.behandlingId)
    }
}
