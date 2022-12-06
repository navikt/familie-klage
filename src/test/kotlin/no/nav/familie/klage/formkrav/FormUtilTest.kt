package no.nav.familie.klage.formkrav

import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype
import no.nav.familie.klage.behandling.dto.PåklagetVedtakDto
import no.nav.familie.klage.formkrav.FormUtil.alleVilkårOppfylt
import no.nav.familie.klage.formkrav.FormUtil.utledFormresultat
import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.domain.FormVilkår
import no.nav.familie.klage.formkrav.domain.FormkravFristUnntak
import no.nav.familie.klage.testutil.DomainUtil.oppfyltForm
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

internal class FormUtilTest {

    val påklagetVedtakMedBehandling = PåklagetVedtakDto(eksternFagsystemBehandlingId = "123", PåklagetVedtakstype.VEDTAK)
    val påklagetVedtakUtenBehandling = PåklagetVedtakDto(eksternFagsystemBehandlingId = null, PåklagetVedtakstype.UTEN_VEDTAK)
    val påklagetVedtakIkkeValgt = PåklagetVedtakDto(eksternFagsystemBehandlingId = null, PåklagetVedtakstype.IKKE_VALGT)

    val behandlingId = UUID.randomUUID()
    val oppfyltForm = oppfyltForm(behandlingId)
    val ikkeOppfyltForm = oppfyltForm.copy(
        saksbehandlerBegrunnelse = "Ok",
        klagePart = FormVilkår.IKKE_OPPFYLT,
        brevtekst = "brevtekst"
    )
    val ikkeFerdigUtfyltForm = Form(UUID.randomUUID())

    @Nested
    inner class utledFormresultat {

        @Test
        internal fun `alle formkrav er ferdigutfylte`() {
            assertThat(utledFormresultat(oppfyltForm, påklagetVedtakMedBehandling)).isEqualTo(FormVilkår.OPPFYLT)
            assertThat(utledFormresultat(oppfyltForm, påklagetVedtakUtenBehandling)).isEqualTo(FormVilkår.OPPFYLT)
            assertThat(utledFormresultat(ikkeOppfyltForm, påklagetVedtakMedBehandling)).isEqualTo(FormVilkår.IKKE_OPPFYLT)
            assertThat(utledFormresultat(ikkeOppfyltForm, påklagetVedtakUtenBehandling)).isEqualTo(FormVilkår.IKKE_OPPFYLT)
        }

        @Test
        internal fun `et eller flere formkrav er ikke utfylt`() {
            assertThat(utledFormresultat(ikkeOppfyltForm, påklagetVedtakMedBehandling)).isEqualTo(FormVilkår.IKKE_OPPFYLT)
            assertThat(utledFormresultat(ikkeOppfyltForm, påklagetVedtakUtenBehandling)).isEqualTo(FormVilkår.IKKE_OPPFYLT)
            assertThat(utledFormresultat(ikkeOppfyltForm, påklagetVedtakIkkeValgt)).isEqualTo(FormVilkår.IKKE_SATT)
            assertThat(utledFormresultat(ikkeFerdigUtfyltForm, påklagetVedtakMedBehandling)).isEqualTo(FormVilkår.IKKE_SATT)
            assertThat(utledFormresultat(ikkeFerdigUtfyltForm, påklagetVedtakUtenBehandling)).isEqualTo(FormVilkår.IKKE_SATT)
            assertThat(utledFormresultat(ikkeFerdigUtfyltForm, påklagetVedtakIkkeValgt)).isEqualTo(FormVilkår.IKKE_SATT)
        }
    }

    @Nested
    inner class formkravErOppfylt {

        @Test
        internal fun `alle formkrav oppfylt skal returnere true`() {
            assertThat(alleVilkårOppfylt(oppfyltForm)).isTrue
        }

        @Test
        internal fun `et eller flere ikke oppfylte formkrav skal returnere false`() {
            assertThat(alleVilkårOppfylt(oppfyltForm.copy(klageKonkret = FormVilkår.IKKE_OPPFYLT))).isFalse
            assertThat(alleVilkårOppfylt(oppfyltForm.copy(klageSignert = FormVilkår.IKKE_OPPFYLT))).isFalse
            assertThat(alleVilkårOppfylt(oppfyltForm.copy(klagePart = FormVilkår.IKKE_OPPFYLT))).isFalse
        }

        @Test
        internal fun `et eller flere ikke utfylte formkrav skal returnere false`() {
            assertThat(alleVilkårOppfylt(oppfyltForm.copy(klageKonkret = FormVilkår.IKKE_SATT))).isFalse
            assertThat(alleVilkårOppfylt(oppfyltForm.copy(klageSignert = FormVilkår.IKKE_SATT))).isFalse
            assertThat(alleVilkårOppfylt(oppfyltForm.copy(klagePart = FormVilkår.IKKE_SATT))).isFalse
            assertThat(alleVilkårOppfylt(oppfyltForm.copy(klagefristOverholdt = FormVilkår.IKKE_SATT))).isFalse
        }

        @Test
        internal fun `formkrav om overholdt frist er ikke oppfylt`() {
            val ikkeOppfyltFrist = oppfyltForm.copy(klagefristOverholdt = FormVilkår.IKKE_OPPFYLT)
            val toFormKravIkkeOppfylt = ikkeOppfyltFrist.copy(klageKonkret = FormVilkår.IKKE_OPPFYLT)

            assertThat(alleVilkårOppfylt(ikkeOppfyltFrist)).isFalse
            assertThat(alleVilkårOppfylt(ikkeOppfyltFrist.copy(klagefristOverholdtUnntak = FormkravFristUnntak.IKKE_UNNTAK))).isFalse
            assertThat(alleVilkårOppfylt(ikkeOppfyltFrist.copy(klagefristOverholdtUnntak = FormkravFristUnntak.UNNTAK_KAN_IKKE_LASTES))).isTrue
            assertThat(alleVilkårOppfylt(ikkeOppfyltFrist.copy(klagefristOverholdtUnntak = FormkravFristUnntak.UNNTAK_SÆRLIG_GRUNN))).isTrue
            assertThat(alleVilkårOppfylt(toFormKravIkkeOppfylt)).isFalse
            assertThat(alleVilkårOppfylt(toFormKravIkkeOppfylt.copy(klagefristOverholdtUnntak = FormkravFristUnntak.IKKE_UNNTAK))).isFalse
            assertThat(alleVilkårOppfylt(toFormKravIkkeOppfylt.copy(klagefristOverholdtUnntak = FormkravFristUnntak.UNNTAK_KAN_IKKE_LASTES))).isFalse
            assertThat(alleVilkårOppfylt(toFormKravIkkeOppfylt.copy(klagefristOverholdtUnntak = FormkravFristUnntak.UNNTAK_SÆRLIG_GRUNN))).isFalse
        }

        @Test
        internal fun `formkrav om overholdt frist er ikke utfylt`() {
            val ikkeOppfyltFrist = oppfyltForm.copy(klagefristOverholdt = FormVilkår.IKKE_SATT)
            val toFormKravIkkeSatt = ikkeOppfyltFrist.copy(klageKonkret = FormVilkår.IKKE_SATT)

            assertThat(alleVilkårOppfylt(ikkeOppfyltFrist)).isFalse
            assertThat(alleVilkårOppfylt(ikkeOppfyltFrist.copy(klagefristOverholdtUnntak = FormkravFristUnntak.IKKE_UNNTAK))).isFalse
            assertThat(alleVilkårOppfylt(ikkeOppfyltFrist.copy(klagefristOverholdtUnntak = FormkravFristUnntak.UNNTAK_KAN_IKKE_LASTES))).isFalse
            assertThat(alleVilkårOppfylt(ikkeOppfyltFrist.copy(klagefristOverholdtUnntak = FormkravFristUnntak.UNNTAK_SÆRLIG_GRUNN))).isFalse
            assertThat(alleVilkårOppfylt(toFormKravIkkeSatt)).isFalse
            assertThat(alleVilkårOppfylt(toFormKravIkkeSatt.copy(klagefristOverholdtUnntak = FormkravFristUnntak.IKKE_UNNTAK))).isFalse
            assertThat(alleVilkårOppfylt(toFormKravIkkeSatt.copy(klagefristOverholdtUnntak = FormkravFristUnntak.UNNTAK_KAN_IKKE_LASTES))).isFalse
            assertThat(alleVilkårOppfylt(toFormKravIkkeSatt.copy(klagefristOverholdtUnntak = FormkravFristUnntak.UNNTAK_SÆRLIG_GRUNN))).isFalse
        }
    }
}
