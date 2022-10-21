package no.nav.familie.klage.kabal

import no.nav.familie.klage.felles.domain.SporbarUtils
import no.nav.familie.klage.infrastruktur.config.DatabaseConfiguration.StringListWrapper
import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.kabal.domain.Klageresultat
import no.nav.familie.klage.repository.findByIdOrThrow
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import no.nav.familie.kontrakter.felles.klage.BehandlingEventType
import no.nav.familie.kontrakter.felles.klage.ExternalUtfall
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class KlageresultatRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var klageresultatRepository: KlageresultatRepository

    @Test
    internal fun `skal kunne lagre og hente klageresultat`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagreBehandling(behandling(fagsak))
        val klageresultat = Klageresultat(
            eventId = UUID.randomUUID(),
            type = BehandlingEventType.ANKEBEHANDLING_AVSLUTTET,
            utfall = ExternalUtfall.OPPHEVET,
            mottattEllerAvsluttetTidspunkt = SporbarUtils.now(),
            kildereferanse = UUID.randomUUID(),
            journalpostReferanser = StringListWrapper(listOf("ref1", "ref2")),
            behandlingId = behandling.id
        )
        klageresultatRepository.insert(klageresultat)

        val hentetKlageresultat = klageresultatRepository.findByIdOrThrow(klageresultat.eventId)
        assertThat(hentetKlageresultat.eventId).isEqualTo(klageresultat.eventId)
        assertThat(hentetKlageresultat.type).isEqualTo(klageresultat.type)
        assertThat(hentetKlageresultat.utfall).isEqualTo(klageresultat.utfall)
        assertThat(hentetKlageresultat.mottattEllerAvsluttetTidspunkt).isEqualTo(klageresultat.mottattEllerAvsluttetTidspunkt)
        assertThat(hentetKlageresultat.kildereferanse).isEqualTo(klageresultat.kildereferanse)
        assertThat(hentetKlageresultat.journalpostReferanser).isEqualTo(klageresultat.journalpostReferanser)
        assertThat(hentetKlageresultat.journalpostReferanser.verdier).hasSize(2)
        assertThat(hentetKlageresultat.behandlingId).isEqualTo(klageresultat.behandlingId)
    }
}
