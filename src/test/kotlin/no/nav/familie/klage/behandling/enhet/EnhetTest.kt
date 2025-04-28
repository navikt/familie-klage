package no.nav.familie.klage.behandling.enhet

import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class EnhetTest {
    @Nested
    inner class FinnEnhet {
        @ParameterizedTest
        @EnumSource(BarnetrygdEnhet::class)
        fun `skal finne enheter for BA`(barnetrygdEnhet: BarnetrygdEnhet) {
            // Act
            val enhet = Enhet.finnEnhet(Fagsystem.BA, barnetrygdEnhet.enhetsnummer)

            // Assert
            assertThat(enhet.enhetsnummer).isEqualTo(barnetrygdEnhet.enhetsnummer)
            assertThat(enhet.enhetsnavn).isEqualTo(barnetrygdEnhet.enhetsnavn)
        }

        @ParameterizedTest
        @EnumSource(KontantstøtteEnhet::class)
        fun `skal finne enheter for KS`(kontantstøtteEnhet: KontantstøtteEnhet) {
            // Act
            val enhet = Enhet.finnEnhet(Fagsystem.KS, kontantstøtteEnhet.enhetsnummer)

            // Assert
            assertThat(enhet.enhetsnummer).isEqualTo(kontantstøtteEnhet.enhetsnummer)
            assertThat(enhet.enhetsnavn).isEqualTo(kontantstøtteEnhet.enhetsnavn)
        }

        @ParameterizedTest
        @EnumSource(Fagsystem::class, names = ["BA", "KS"], mode = EnumSource.Mode.EXCLUDE)
        fun `skal kaste feil om fagsystem ikke er støttet`(fagsystem: Fagsystem) {
            // Act & assert
            val exception = assertThrows<Feil> {
                Enhet.finnEnhet(fagsystem, "1234")
            }
            assertThat(exception.message).isEqualTo("Støtter ikke endring av enhet for fagsystem $fagsystem")
        }
    }
}