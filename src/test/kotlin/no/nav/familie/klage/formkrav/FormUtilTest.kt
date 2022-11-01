package no.nav.familie.klage.formkrav

import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype
import no.nav.familie.klage.behandling.dto.PåklagetVedtakDto
import no.nav.familie.klage.formkrav.FormUtil.ferdigUtfylt
import no.nav.familie.klage.formkrav.FormUtil.alleVilkårOppfylt
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

    @Nested
    inner class formkravErFerdigUtfyllt {

        @Test
        internal fun `alle er ferdigutfylte`() {
            val oppfyltForm = oppfyltForm(UUID.randomUUID())
            assertThat(ferdigUtfylt(oppfyltForm, påklagetVedtakMedBehandling)).isTrue
            assertThat(ferdigUtfylt(oppfyltForm, påklagetVedtakUtenVedtak)).isTrue
            assertThat(ferdigUtfylt(oppfyltForm.copy(klagePart = FormVilkår.IKKE_OPPFYLT, brevtekst = "brevtekst"), påklagetVedtakMedBehandling)).isTrue
        }

        @Test
        internal fun `et eller flere er ikke utfylt`() {
            val oppfyltForm = oppfyltForm(UUID.randomUUID())
            assertThat(ferdigUtfylt(oppfyltForm.copy(klagePart = FormVilkår.IKKE_SATT), påklagetVedtakMedBehandling)).isFalse
            assertThat(ferdigUtfylt(Form(UUID.randomUUID()), påklagetVedtakMedBehandling)).isFalse
            assertThat(ferdigUtfylt(oppfyltForm, påklagetVedtakIkkeValgt)).isFalse
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
            val form = Form(UUID.randomUUID())
            assertThat(alleVilkårOppfylt(form)).isFalse
            assertThat(alleVilkårOppfylt(form.copy(klagePart = FormVilkår.OPPFYLT))).isFalse

            assertThat(alleVilkårOppfylt(oppfyltForm(UUID.randomUUID()).copy(klagePart = FormVilkår.IKKE_OPPFYLT))).isFalse
            assertThat(alleVilkårOppfylt(oppfyltForm(UUID.randomUUID()).copy(klagePart = FormVilkår.IKKE_SATT))).isFalse
        }
    }
}
