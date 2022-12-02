package no.nav.familie.klage.formkrav

import no.nav.familie.klage.behandling.dto.PåklagetVedtakDto
import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.domain.FormVilkår
import no.nav.familie.klage.formkrav.domain.FormkravFristUnntak

object FormUtil {

    fun ferdigUtfylt(formkrav: Form, påklagetVedtak: PåklagetVedtakDto) =
            påklagetVedtak.harTattStillingTil() &&
            alleVilkårTattStillingTil(formkrav) &&
            (alleVilkårOppfylt(formkrav) || friteksterUtfylt(formkrav))

    fun alleVilkårOppfylt(formkrav: Form): Boolean {
        return formkrav.alleSvar().all { it == FormVilkår.OPPFYLT } ||
               (alleVilkårOppfyltUntattKlagefrist(formkrav) && klagefristUnntakErValgtOgOppfylt(formkrav.klagefristOverholdtUnntak))
    }

    private fun klagefristUnntakErValgtOgOppfylt(unntak: FormkravFristUnntak?) =
            unntak !== null && klagefristUnntakOppfylt(unntak)


    private fun alleVilkårOppfyltUntattKlagefrist(formkrav: Form) =
        formkrav.alleSvarBortsettFraFrist().all { it == FormVilkår.OPPFYLT } && formkrav.klagefristOverholdt == FormVilkår.IKKE_OPPFYLT

    private fun klagefristUnntakOppfylt(unntak: FormkravFristUnntak) = when(unntak) {
        FormkravFristUnntak.IKKE_UNNTAK, FormkravFristUnntak.IKKE_SATT -> false
        FormkravFristUnntak.UNNTAK_SÆRLIG_GRUNN, FormkravFristUnntak.UNNTAK_KAN_IKKE_LASTES -> true
    }

    fun alleVilkårTattStillingTil(formkrav: Form): Boolean {
        return formkrav.alleSvar().none { it == FormVilkår.IKKE_SATT } && klagefristUnntakTattStillingTil(formkrav)
    }

    private fun klagefristUnntakTattStillingTil(formkrav: Form) =
        formkrav.klagefristOverholdt === FormVilkår.OPPFYLT ||
        (formkrav.klagefristOverholdt === FormVilkår.IKKE_OPPFYLT &&
         formkrav.klagefristOverholdtUnntak != null &&
         formkrav.klagefristOverholdtUnntak != FormkravFristUnntak.IKKE_SATT)

    fun friteksterUtfylt(formkrav: Form) = formkrav.saksbehandlerBegrunnelse != null &&
        formkrav.saksbehandlerBegrunnelse.isNotBlank() &&
        formkrav.brevtekst != null &&
        formkrav.brevtekst.isNotBlank()

    private fun Form.alleSvarBortsettFraFrist() = setOf(
        klageKonkret,
        klagePart,
        klageSignert
    )

    private fun Form.alleSvar() = setOf(
        klageKonkret,
        klagePart,
        klageSignert,
        klagefristOverholdt
    )
}
