package no.nav.familie.klage.kabal.domain

import no.nav.familie.klage.testutil.DomainUtil.klageresultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KlageinstansResultatDtoMapperTest {
    @Test
    fun `map klageresultater til dto`() {
        val klageresultat = klageresultat()
        val klageresultat2 = klageresultat()
        val klageresultater = listOf(klageresultat, klageresultat2)

        val klageresultatDtoList = klageresultater.tilDto()

        assertThat(klageresultatDtoList.size).isEqualTo(2)
        assertThat(klageresultatDtoList.first().type).isEqualTo(klageresultat.type)
        assertThat(klageresultatDtoList.first().mottattEllerAvsluttetTidspunkt).isEqualTo(klageresultat.mottattEllerAvsluttetTidspunkt)
        assertThat(klageresultatDtoList.first().utfall).isEqualTo(klageresultat.utfall)
        assertThat(klageresultatDtoList.first().journalpostReferanser).isEqualTo(klageresultat.journalpostReferanser.verdier)
    }
}
