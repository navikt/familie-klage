package no.nav.familie.klage.vurdering

import no.nav.familie.klage.testutil.DomainUtil.vurderingDto
import no.nav.familie.klage.vurdering.VurderingValidator.validerVurdering
import no.nav.familie.klage.vurdering.domain.Hjemmel
import no.nav.familie.klage.vurdering.domain.Vedtak
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.Årsak
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class VurderingValidatorTest {

    @Nested
    inner class OmgjørVedtak {

        @ParameterizedTest
        @EnumSource(value = Fagsystem::class)
        internal fun `skal validere når man har med årsak, men hjemmel er null`(fagsystem: Fagsystem) {
            validerVurdering(
                vurdering = vurderingDto(
                    vedtak = Vedtak.OMGJØR_VEDTAK,
                    hjemmel = null,
                    årsak = Årsak.FEIL_I_LOVANDVENDELSE,
                    begrunnelseOmgjøring = "begrunnelse",
                ),
                fagsystem = fagsystem,
            )
        }

        @ParameterizedTest
        @EnumSource(value = Fagsystem::class)
        internal fun `skal feile når årsak er null`(fagsystem: Fagsystem) {
            assertThatThrownBy {
                validerVurdering(
                    vurdering = vurderingDto(vedtak = Vedtak.OMGJØR_VEDTAK, hjemmel = null, årsak = null),
                    fagsystem = fagsystem,
                )
            }.hasMessage("Mangler årsak på omgjør vedtak")
        }

        @ParameterizedTest
        @EnumSource(value = Fagsystem::class)
        internal fun `skal feile når hjemmel ikke er null`(fagsystem: Fagsystem) {
            assertThatThrownBy {
                validerVurdering(
                    vurdering = vurderingDto(
                        vedtak = Vedtak.OMGJØR_VEDTAK,
                        hjemmel = Hjemmel.BT_FEM,
                        årsak = Årsak.ANNET,
                        begrunnelseOmgjøring = "begrunnelse",
                    ),
                    fagsystem = fagsystem,
                )
            }.hasMessage("Kan ikke lagre hjemmel på omgjør vedtak")
        }

        @ParameterizedTest
        @EnumSource(value = Fagsystem::class)
        internal fun `skal feile når begrunnelse for omgjøring er null`(fagsystem: Fagsystem) {
            assertThatThrownBy {
                validerVurdering(
                    vurdering = vurderingDto(
                        vedtak = Vedtak.OMGJØR_VEDTAK,
                        årsak = Årsak.ANNET,
                        begrunnelseOmgjøring = null,
                    ),
                    fagsystem = fagsystem,
                )
            }.hasMessage("Mangler begrunnelse for omgjøring på omgjør vedtak")
        }
    }

    @Nested
    inner class OpprettholdVedtak {

        @ParameterizedTest
        @EnumSource(value = Fagsystem::class)
        internal fun `skal validere når man har med hjemmel, men årsak er null`(fagsystem: Fagsystem) {
            validerVurdering(
                vurdering = vurderingDto(
                    vedtak = Vedtak.OPPRETTHOLD_VEDTAK,
                    hjemmel = Hjemmel.BT_FEM,
                    årsak = null,
                ),
                fagsystem = fagsystem,
            )
        }

        @ParameterizedTest
        @EnumSource(value = Fagsystem::class)
        internal fun `skal feile når hjemmel er null`(fagsystem: Fagsystem) {
            assertThatThrownBy {
                validerVurdering(
                    vurdering = vurderingDto(
                        vedtak = Vedtak.OPPRETTHOLD_VEDTAK,
                        hjemmel = null,
                        årsak = null,
                    ),
                    fagsystem = fagsystem,
                )
            }.hasMessage("Mangler hjemmel på oppretthold vedtak")
        }

        @ParameterizedTest
        @EnumSource(value = Fagsystem::class)
        internal fun `skal feile når årsak ikke er null`(fagsystem: Fagsystem) {
            assertThatThrownBy {
                validerVurdering(
                    vurdering = vurderingDto(
                        vedtak = Vedtak.OPPRETTHOLD_VEDTAK,
                        hjemmel = Hjemmel.BT_FEM,
                        årsak = Årsak.ANNET,
                    ),
                    fagsystem = fagsystem,
                )
            }.hasMessage("Kan ikke lagre årsak på oppretthold vedtak")
        }

        @ParameterizedTest
        @EnumSource(value = Fagsystem::class)
        internal fun `skal feile når begrunnelse for omgjøring ikke er null`(fagsystem: Fagsystem) {
            assertThatThrownBy {
                validerVurdering(
                    vurdering = vurderingDto(
                        vedtak = Vedtak.OPPRETTHOLD_VEDTAK,
                        hjemmel = Hjemmel.BT_FEM,
                        begrunnelseOmgjøring = "begrunnelse",
                    ),
                    fagsystem = fagsystem,
                )
            }.hasMessage("Kan ikke lagre begrunnelse på oppretthold vedtak")
        }

        @Test
        internal fun `skal feile når innstillingKlageinstans ikke er satt når fagsystem er EF`() {
            assertThatThrownBy {
                validerVurdering(
                    vurdering = vurderingDto(
                        vedtak = Vedtak.OPPRETTHOLD_VEDTAK,
                        hjemmel = Hjemmel.BT_FEM,
                        innstillingKlageinstans = null,
                    ),
                    fagsystem = Fagsystem.EF,
                )
            }.hasMessage("Må skrive innstilling til klageinstans ved opprettholdelse av vedtak")
        }

        @ParameterizedTest
        @EnumSource(value = Fagsystem::class, mode = EnumSource.Mode.EXCLUDE, names = ["EF"])
        internal fun `skal feile når dokumentasjonOgUtredning ikke er satt når fagsystem er BA og KS`(fagsystem: Fagsystem) {
            assertThatThrownBy {
                validerVurdering(
                    vurdering = vurderingDto(
                        vedtak = Vedtak.OPPRETTHOLD_VEDTAK,
                        hjemmel = Hjemmel.BT_FEM,
                        dokumentasjonOgUtredning = null,
                    ),
                    fagsystem = fagsystem,
                )
            }.hasMessage("Feltet 'Dokumentasjon og utredning' må fylles ut ved opprettholdelse av vedtak.")
        }

        @ParameterizedTest
        @EnumSource(value = Fagsystem::class, mode = EnumSource.Mode.EXCLUDE, names = ["EF"])
        internal fun `skal slå sammen felter i feilmelding om innstilling når fagsystem er BA og KS`(fagsystem: Fagsystem) {
            assertThatThrownBy {
                validerVurdering(
                    vurdering = vurderingDto(
                        vedtak = Vedtak.OPPRETTHOLD_VEDTAK,
                        hjemmel = Hjemmel.BT_FEM,
                        dokumentasjonOgUtredning = null,
                        spørsmåletISaken = null,
                        aktuelleRettskilder = null,
                        klagersAnførsler = null,
                        vurderingAvKlagen = null,
                    ),
                    fagsystem = fagsystem,
                )
            }.hasMessage(
                "Feltene 'Dokumentasjon og utredning', 'Spørsmålet i saken', 'Aktuelle rettskilder', " +
                    "'Klagers anførsler' og 'Vurdering av klagen' må fylles ut ved opprettholdelse av vedtak.",
            )
        }
    }
}
