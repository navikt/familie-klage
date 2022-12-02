package no.nav.familie.klage.formkrav

import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype
import no.nav.familie.klage.behandling.dto.PåklagetVedtakDto
import no.nav.familie.klage.formkrav.FormUtil.alleVilkårOppfylt
import no.nav.familie.klage.formkrav.FormUtil.alleVilkårTattStillingTil
import no.nav.familie.klage.formkrav.FormUtil.ferdigUtfylt
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
    val påklagetVedtakUtenVedtak = PåklagetVedtakDto(eksternFagsystemBehandlingId = null, PåklagetVedtakstype.UTEN_VEDTAK)
    val påklagetVedtakIkkeValgt = PåklagetVedtakDto(eksternFagsystemBehandlingId = null, PåklagetVedtakstype.IKKE_VALGT)
    val oppfyltForm = oppfyltForm(UUID.randomUUID())
    val behandlingId = UUID.randomUUID()

    @Nested
    inner class formkravErFerdigUtfyllt {

        @Test
        internal fun `alle formkrav er oppfylt`() {
            assertThat(ferdigUtfylt(oppfyltForm, påklagetVedtakMedBehandling)).isTrue
            assertThat(ferdigUtfylt(oppfyltForm, påklagetVedtakUtenVedtak)).isTrue
            assertThat(
                ferdigUtfylt(
                    oppfyltForm.copy(
                        saksbehandlerBegrunnelse = "Ok",
                        klagePart = FormVilkår.IKKE_OPPFYLT,
                        brevtekst = "brevtekst"
                    ),
                    påklagetVedtakMedBehandling
                )
            ).isTrue
        }

        @Test
        internal fun `alle er ferdigutfylte bortsett fra unntak som burde vært utfylt`() {
            val medUnntak = oppfyltForm.copy(klagefristOverholdt = FormVilkår.IKKE_OPPFYLT)
            assertThat(ferdigUtfylt(medUnntak, påklagetVedtakMedBehandling)).isFalse
        }

        @Test
        internal fun `alle er ferdigutfylte inkludert unntak`() {
            val medUnntak = oppfyltForm.copy(klagefristOverholdt = FormVilkår.IKKE_OPPFYLT, klagefristOverholdtUnntak = FormkravFristUnntak.IKKE_UNNTAK, saksbehandlerBegrunnelse = "Ja", brevtekst = "Ja")
            assertThat(ferdigUtfylt(medUnntak, påklagetVedtakMedBehandling)).isTrue
        }

        @Test
        internal fun `et eller flere er ikke utfylt`() {
            assertThat(ferdigUtfylt(oppfyltForm.copy(klagePart = FormVilkår.IKKE_SATT), påklagetVedtakMedBehandling)).isFalse
            assertThat(ferdigUtfylt(Form(UUID.randomUUID()), påklagetVedtakMedBehandling)).isFalse
            assertThat(ferdigUtfylt(oppfyltForm, påklagetVedtakIkkeValgt)).isFalse
        }
    }

    @Nested
    inner class formkravErOppfylt {

        @Test
        internal fun `alle er oppfylt`() {
            assertThat(alleVilkårOppfylt(oppfyltForm)).isTrue
        }


        @Test
        internal fun `klagefrist er ikke overholdt med unntak`() {
            val kanIkkeLastes = oppfyltForm.copy(klagefristOverholdt = FormVilkår.IKKE_OPPFYLT, klagefristOverholdtUnntak = FormkravFristUnntak.UNNTAK_KAN_IKKE_LASTES)
            val særligGrunn = kanIkkeLastes.copy(klagefristOverholdtUnntak = FormkravFristUnntak.UNNTAK_SÆRLIG_GRUNN)
            assertThat(alleVilkårOppfylt(kanIkkeLastes)).isTrue
            assertThat(alleVilkårOppfylt(særligGrunn)).isTrue
        }

        @Test
        internal fun `klagefrist er ikke overholdt uten unntak`() {
            val ikkeUnntak = oppfyltForm.copy(klagefristOverholdt = FormVilkår.IKKE_OPPFYLT, klagefristOverholdtUnntak = FormkravFristUnntak.IKKE_UNNTAK)
            val unntakIkkeTattStillingTil = ikkeUnntak.copy(klagefristOverholdtUnntak = FormkravFristUnntak.IKKE_SATT)
            assertThat(alleVilkårOppfylt(ikkeUnntak)).isFalse
            assertThat(alleVilkårOppfylt(unntakIkkeTattStillingTil)).isFalse
        }

        @Test
        internal fun `klagefrist ikke overholdt med unntak skal kreve at alle andre vilkår er oppfylt`() {
            val formMedUnntak = oppfyltForm.copy(klageSignert = FormVilkår.IKKE_OPPFYLT, klagefristOverholdtUnntak = FormkravFristUnntak.UNNTAK_KAN_IKKE_LASTES)

            val etFormkravIkkeOppfylt = formMedUnntak.copy(klageKonkret = FormVilkår.IKKE_OPPFYLT)
            val toFormkravIkkeOppfylt = formMedUnntak.copy(klageKonkret = FormVilkår.IKKE_OPPFYLT, klageSignert = FormVilkår.IKKE_OPPFYLT)
            val treFormkravIkkeOppfylt = formMedUnntak.copy(klageKonkret = FormVilkår.IKKE_OPPFYLT, klageSignert = FormVilkår.IKKE_OPPFYLT, klagePart = FormVilkår.IKKE_OPPFYLT)
            assertThat(alleVilkårOppfylt(etFormkravIkkeOppfylt)).isFalse
            assertThat(alleVilkårOppfylt(toFormkravIkkeOppfylt)).isFalse
            assertThat(alleVilkårOppfylt(treFormkravIkkeOppfylt)).isFalse

            val etFormkravIkkeSatt = formMedUnntak.copy(klageKonkret = FormVilkår.IKKE_SATT)
            val toFormkravIkkeSatt = formMedUnntak.copy(klageKonkret = FormVilkår.IKKE_SATT, klageSignert = FormVilkår.IKKE_SATT)
            val treFormkravIkkeSatt = formMedUnntak.copy(klageKonkret = FormVilkår.IKKE_SATT, klageSignert = FormVilkår.IKKE_SATT, klagePart = FormVilkår.IKKE_SATT)
            assertThat(alleVilkårOppfylt(etFormkravIkkeSatt)).isFalse
            assertThat(alleVilkårOppfylt(toFormkravIkkeSatt)).isFalse
            assertThat(alleVilkårOppfylt(treFormkravIkkeSatt)).isFalse
        }

        @Test
        internal fun `et eller flere vilkår er ikke oppfylt`() {
            val form = Form(UUID.randomUUID())
            assertThat(alleVilkårOppfylt(form)).isFalse
            assertThat(alleVilkårOppfylt(form.copy(klagePart = FormVilkår.OPPFYLT))).isFalse
            assertThat(alleVilkårOppfylt(oppfyltForm.copy(klagePart = FormVilkår.IKKE_OPPFYLT))).isFalse
            assertThat(alleVilkårOppfylt(oppfyltForm.copy(klagePart = FormVilkår.IKKE_SATT))).isFalse
        }
    }

    @Nested
    inner class formrkavErTattStillingTil {

        @Test
        internal fun `alle formkrav oppfylt`() {
            val oppfyltForm = oppfyltForm
            assertThat(alleVilkårTattStillingTil(oppfyltForm)).isTrue
        }

        @Test
        internal fun `formkrav ikke oppfylt`() {
            val etFormkravIkkeOppfylt = oppfyltForm.copy(klagePart = FormVilkår.IKKE_OPPFYLT)
            val toFormkravIkkeOppfylt = oppfyltForm.copy(klagePart = FormVilkår.IKKE_OPPFYLT, klageKonkret = FormVilkår.IKKE_OPPFYLT)
            val treFormkravIkkeOppfylt = oppfyltForm.copy(klagePart = FormVilkår.IKKE_OPPFYLT, klageKonkret = FormVilkår.IKKE_OPPFYLT, klageSignert = FormVilkår.IKKE_OPPFYLT)

            assertThat(alleVilkårTattStillingTil(etFormkravIkkeOppfylt)).isTrue
            assertThat(alleVilkårTattStillingTil(toFormkravIkkeOppfylt)).isTrue
            assertThat(alleVilkårTattStillingTil(treFormkravIkkeOppfylt)).isTrue
        }

        @Test
        internal fun `formkrav ikke tatt stilling til`() {
            val etFormkravIkkeTattStillingTil = oppfyltForm.copy(klagePart = FormVilkår.IKKE_SATT)
            val toFormkravIkkeTattStillingTil = oppfyltForm.copy(klagePart = FormVilkår.IKKE_SATT, klagefristOverholdt = FormVilkår.IKKE_SATT)
            val treFormkravIkkeTattStillingTil = oppfyltForm.copy(klagePart = FormVilkår.IKKE_SATT, klagefristOverholdt = FormVilkår.IKKE_SATT, klageKonkret = FormVilkår.IKKE_SATT)
            val fireFormkravIkkeTattStillingTil = oppfyltForm.copy(klagePart = FormVilkår.IKKE_SATT, klagefristOverholdt = FormVilkår.IKKE_SATT, klageKonkret = FormVilkår.IKKE_SATT, klageSignert = FormVilkår.IKKE_SATT)

            assertThat(alleVilkårTattStillingTil(etFormkravIkkeTattStillingTil)).isFalse
            assertThat(alleVilkårTattStillingTil(toFormkravIkkeTattStillingTil)).isFalse
            assertThat(alleVilkårTattStillingTil(treFormkravIkkeTattStillingTil)).isFalse
            assertThat(alleVilkårTattStillingTil(fireFormkravIkkeTattStillingTil)).isFalse
        }

        @Test
        internal fun `klagefrist ikke oppfylt skal kreve at unntak er tatt stilling til`() {
            val unntakErNull = oppfyltForm.copy(klagefristOverholdt = FormVilkår.IKKE_OPPFYLT)
            val unntakIkkeSatt = unntakErNull.copy(klagefristOverholdtUnntak = FormkravFristUnntak.IKKE_SATT)
            val ikkeUnntak = unntakErNull.copy(klagefristOverholdtUnntak = FormkravFristUnntak.IKKE_UNNTAK)
            val særligGrunn = unntakErNull.copy(klagefristOverholdtUnntak = FormkravFristUnntak.UNNTAK_SÆRLIG_GRUNN)
            val kanIkkeLastes = unntakErNull.copy(klagefristOverholdtUnntak = FormkravFristUnntak.UNNTAK_KAN_IKKE_LASTES)

            assertThat(alleVilkårTattStillingTil(unntakErNull)).isFalse
            assertThat(alleVilkårTattStillingTil(unntakIkkeSatt)).isFalse
            assertThat(alleVilkårTattStillingTil(ikkeUnntak)).isTrue
            assertThat(alleVilkårTattStillingTil(særligGrunn)).isTrue
            assertThat(alleVilkårTattStillingTil(kanIkkeLastes)).isTrue
        }
    }
}
