package no.nav.familie.klage.formkrav

import no.nav.familie.klage.formkrav.FormUtil.formkravErFerdigUtfyllt
import no.nav.familie.klage.formkrav.FormUtil.formkravErOppfylt
import no.nav.familie.klage.formkrav.FormUtil.initiellForm
import no.nav.familie.klage.formkrav.domain.FormVilkår
import no.nav.familie.klage.testutil.DomainUtil.oppfyltForm
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

internal class FormUtilTest {

    @Nested
    inner class formkravErFerdigUtfyllt {

        @Test
        internal fun `alle er ferdigutfylte`() {
            val oppfyltForm = oppfyltForm(UUID.randomUUID())
            assertThat(formkravErFerdigUtfyllt(oppfyltForm)).isTrue
            assertThat(formkravErFerdigUtfyllt(oppfyltForm.copy(klagePart = FormVilkår.SKAL_IKKE_VURDERES))).isTrue
            assertThat(formkravErFerdigUtfyllt(oppfyltForm.copy(klagePart = FormVilkår.IKKE_OPPFYLT))).isTrue
        }

        @Test
        internal fun `et eller flere er ikke utfylt`() {
            val oppfyltForm = oppfyltForm(UUID.randomUUID())
            assertThat(formkravErFerdigUtfyllt(oppfyltForm.copy(klagePart = FormVilkår.IKKE_SATT))).isFalse
            assertThat(formkravErFerdigUtfyllt(initiellForm(UUID.randomUUID()))).isFalse
        }
    }

    @Nested
    inner class formkravErOppfylt {

        @Test
        internal fun `alle er oppfylt`() {
            assertThat(formkravErOppfylt(oppfyltForm(UUID.randomUUID()))).isTrue
        }

        @Test
        internal fun `et eller flere vilkår er ikke oppfylt`() {
            val form = FormUtil.initiellForm(UUID.randomUUID())
            assertThat(formkravErOppfylt(form)).isFalse
            assertThat(formkravErOppfylt(form.copy(klagePart = FormVilkår.OPPFYLT))).isFalse

            assertThat(formkravErOppfylt(oppfyltForm(UUID.randomUUID()).copy(klagePart = FormVilkår.IKKE_OPPFYLT))).isFalse
            assertThat(formkravErOppfylt(oppfyltForm(UUID.randomUUID()).copy(klagePart = FormVilkår.IKKE_SATT))).isFalse
            assertThat(formkravErOppfylt(oppfyltForm(UUID.randomUUID()).copy(klagePart = FormVilkår.SKAL_IKKE_VURDERES))).isFalse
        }
    }
}
