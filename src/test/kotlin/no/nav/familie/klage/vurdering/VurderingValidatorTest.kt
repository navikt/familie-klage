package no.nav.familie.klage.vurdering

import no.nav.familie.klage.testutil.DomainUtil.vurderingDto
import no.nav.familie.klage.vurdering.VurderingValidator.validerVurdering
import no.nav.familie.klage.vurdering.domain.Hjemmel
import no.nav.familie.klage.vurdering.domain.Vedtak
import no.nav.familie.kontrakter.felles.klage.Årsak
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class VurderingValidatorTest {

    @Nested
    inner class OmgjørVedtak {

        @Test
        internal fun `skal validere når man har med årsak, men hjemmel er null`() {
            validerVurdering(vurderingDto(vedtak = Vedtak.OMGJØR_VEDTAK, hjemmel = null, årsak = Årsak.FEIL_I_LOVANDVENDELSE))
        }

        @Test
        internal fun `skal feile når årsak er null`() {
            assertThatThrownBy {
                validerVurdering(vurderingDto(vedtak = Vedtak.OMGJØR_VEDTAK, hjemmel = null, årsak = null))
            }.hasMessage("Mangler årsak på omgjør vedtak")
        }

        @Test
        internal fun `skal feile når hjemmel ikke er null`() {
            assertThatThrownBy {
                validerVurdering(vurderingDto(vedtak = Vedtak.OMGJØR_VEDTAK, hjemmel = Hjemmel.BT_FEM, årsak = Årsak.ANNET))
            }.hasMessage("Kan ikke lagre hjemmel på omgjør vedtak")
        }
    }

    @Nested
    inner class OpprettholdVedtak {

        @Test
        internal fun `skal validere når man har med hjemmel, men årsak er null`() {
            validerVurdering(vurderingDto(vedtak = Vedtak.OPPRETTHOLD_VEDTAK, hjemmel = Hjemmel.BT_FEM, årsak = null))
        }

        @Test
        internal fun `skal feile når hjemmel er null`() {
            assertThatThrownBy {
                validerVurdering(vurderingDto(vedtak = Vedtak.OPPRETTHOLD_VEDTAK, hjemmel = null, årsak = null))
            }.hasMessage("Mangler hjemmel på oppretthold vedtak")
        }

        @Test
        internal fun `skal feile når årsak ikke er null`() {
            assertThatThrownBy {
                validerVurdering(vurderingDto(vedtak = Vedtak.OPPRETTHOLD_VEDTAK, hjemmel = Hjemmel.BT_FEM, årsak = Årsak.ANNET))
            }.hasMessage("Kan ikke lagre årsak på oppretthold vedtak")
        }
    }
}
