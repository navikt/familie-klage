package no.nav.familie.klage.formkrav

import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype
import no.nav.familie.klage.behandling.dto.PåklagetVedtakDto
import no.nav.familie.klage.formkrav.FormUtil.alleVilkårOppfylt
import no.nav.familie.klage.formkrav.FormUtil.formresultat
import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.domain.FormVilkår
import no.nav.familie.klage.testutil.DomainUtil.oppfyltForm
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

internal class FormUtilTest {

    val påklagetVedtakMedBehandling = PåklagetVedtakDto(eksternFagsystemBehandlingId = "123", PåklagetVedtakstype.VEDTAK)
    val påklagetVedtakUtenVedtak = PåklagetVedtakDto(eksternFagsystemBehandlingId = null, PåklagetVedtakstype.UTEN_VEDTAK)
    val påklagetVedtakIkkeValgt = PåklagetVedtakDto(eksternFagsystemBehandlingId = null, PåklagetVedtakstype.IKKE_VALGT)

    val oppfyltForm = oppfyltForm(UUID.randomUUID())
    val ikkeOppfyltForm = oppfyltForm.copy(
        saksbehandlerBegrunnelse = "Ok",
        klagePart = FormVilkår.IKKE_OPPFYLT,
        brevtekst = "brevtekst"
    )
    val ikkeFerdigUtfyltForm = Form(UUID.randomUUID())

    @Nested
    inner class formresultat {

        @Test
        internal fun `alle er ferdigutfylte`() {
            assertThat(formresultat(oppfyltForm, påklagetVedtakMedBehandling)).isEqualTo(FormVilkår.OPPFYLT)
            assertThat(formresultat(oppfyltForm, påklagetVedtakUtenVedtak)).isEqualTo(FormVilkår.OPPFYLT)
            assertThat(formresultat(ikkeOppfyltForm, påklagetVedtakMedBehandling)).isEqualTo(FormVilkår.IKKE_OPPFYLT)
            assertThat(formresultat(ikkeOppfyltForm, påklagetVedtakUtenVedtak)).isEqualTo(FormVilkår.IKKE_OPPFYLT)
        }

        @Test
        internal fun `et eller flere er ikke utfylt`() {
            val formMedEttVilkårSomIkkeErSatt = oppfyltForm.copy(klagePart = FormVilkår.IKKE_SATT)
            assertThat(formresultat(formMedEttVilkårSomIkkeErSatt, påklagetVedtakMedBehandling)).isEqualTo(FormVilkår.IKKE_SATT)
            assertThat(formresultat(ikkeFerdigUtfyltForm, påklagetVedtakMedBehandling)).isEqualTo(FormVilkår.IKKE_SATT)
            assertThat(formresultat(ikkeFerdigUtfyltForm, påklagetVedtakIkkeValgt)).isEqualTo(FormVilkår.IKKE_SATT)
        }

        @Test
        internal fun `ikke tatt stilling til påklagetVedtak`() {
            assertThat(formresultat(oppfyltForm, påklagetVedtakIkkeValgt)).isEqualTo(FormVilkår.IKKE_SATT)
            assertThat(formresultat(ikkeOppfyltForm, påklagetVedtakIkkeValgt)).isEqualTo(FormVilkår.IKKE_SATT)
            assertThat(formresultat(ikkeFerdigUtfyltForm, påklagetVedtakIkkeValgt)).isEqualTo(FormVilkår.IKKE_SATT)
        }
    }

    @Nested
    inner class formkravErOppfylt {

        @Test
        internal fun `alle er oppfylt`() {
            assertThat(alleVilkårOppfylt(oppfyltForm(UUID.randomUUID()))).isTrue
        }

        @Test
        internal fun `et eller flere vilkår er ikke oppfylt`() {
            assertThat(alleVilkårOppfylt(ikkeFerdigUtfyltForm)).isFalse
            assertThat(alleVilkårOppfylt(ikkeFerdigUtfyltForm.copy(klagePart = FormVilkår.OPPFYLT))).isFalse

            assertThat(alleVilkårOppfylt(oppfyltForm.copy(klagePart = FormVilkår.IKKE_OPPFYLT))).isFalse
            assertThat(alleVilkårOppfylt(oppfyltForm.copy(klagePart = FormVilkår.IKKE_SATT))).isFalse
        }
    }
}
