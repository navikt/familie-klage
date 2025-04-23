package no.nav.familie.klage.formkrav

import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype
import no.nav.familie.klage.behandling.dto.PåklagetVedtakDto
import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.domain.FormVilkår
import no.nav.familie.klage.formkrav.domain.FormkravFristUnntak

object FormUtil {
    fun utledFormresultat(
        formkrav: Form,
        påklagetVedtak: PåklagetVedtakDto,
    ): FormVilkår {
        if (påklagetVedtak.påklagetVedtakstype === PåklagetVedtakstype.UTEN_VEDTAK && friteksterUtfylt(formkrav)) {
            return FormVilkår.IKKE_OPPFYLT
        }

        return when {
            !påklagetVedtak.harTattStillingTil() -> FormVilkår.IKKE_SATT
            !alleVilkårBesvart(formkrav) -> FormVilkår.IKKE_SATT
            alleVilkårOppfylt(formkrav) -> FormVilkår.OPPFYLT
            friteksterUtfylt(formkrav) -> FormVilkår.IKKE_OPPFYLT
            else -> FormVilkår.IKKE_SATT
        }
    }

    fun alleVilkårOppfylt(formkrav: Form): Boolean =
        formkrav.alleSvar().all { it == FormVilkår.OPPFYLT } ||
            (alleVilkårOppfyltUntattKlagefrist(formkrav) && klagefristUnntakOppfylt(formkrav.klagefristOverholdtUnntak))

    private fun alleVilkårOppfyltUntattKlagefrist(formkrav: Form) = formkrav.alleSvarBortsettFraFrist().all { it == FormVilkår.OPPFYLT } && formkrav.klagefristOverholdt == FormVilkår.IKKE_OPPFYLT

    private fun klagefristUnntakOppfylt(unntak: FormkravFristUnntak) =
        when (unntak) {
            FormkravFristUnntak.IKKE_UNNTAK, FormkravFristUnntak.IKKE_SATT -> false
            FormkravFristUnntak.UNNTAK_SÆRLIG_GRUNN, FormkravFristUnntak.UNNTAK_KAN_IKKE_LASTES -> true
        }

    private fun alleVilkårBesvart(formkrav: Form): Boolean =
        formkrav.alleSvar().none {
            it == FormVilkår.IKKE_SATT
        } &&
            klagefristUnntakBesvart(formkrav)

    private fun klagefristUnntakBesvart(formkrav: Form) =
        formkrav.klagefristOverholdt === FormVilkår.OPPFYLT ||
            (
                formkrav.klagefristOverholdt === FormVilkår.IKKE_OPPFYLT &&
                    formkrav.klagefristOverholdtUnntak != FormkravFristUnntak.IKKE_SATT
            )

    private fun friteksterUtfylt(formkrav: Form) =
        formkrav.saksbehandlerBegrunnelse != null &&
            formkrav.saksbehandlerBegrunnelse.isNotBlank() &&
            formkrav.brevtekst != null &&
            formkrav.brevtekst.isNotBlank()

    private fun Form.alleSvarBortsettFraFrist() =
        setOf(
            klageKonkret,
            klagePart,
            klageSignert,
        )

    private fun Form.alleSvar() =
        setOf(
            klageKonkret,
            klagePart,
            klageSignert,
            klagefristOverholdt,
        )
}
