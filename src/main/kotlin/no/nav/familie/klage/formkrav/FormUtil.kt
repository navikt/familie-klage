package no.nav.familie.klage.formkrav

import no.nav.familie.klage.behandling.dto.PåklagetVedtakDto
import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.domain.FormVilkår

object FormUtil {

    fun formresultat(formkrav: Form, påklagetVedtak: PåklagetVedtakDto): FormVilkår {

        return when {
            !påklagetVedtak.harTattStillingTil() -> FormVilkår.IKKE_SATT
            !alleVilkårBesvart(formkrav) -> FormVilkår.IKKE_SATT
            alleVilkårOppfylt(formkrav) -> FormVilkår.OPPFYLT
            friteksterUtfylt(formkrav) -> FormVilkår.IKKE_OPPFYLT
            else -> FormVilkår.IKKE_SATT
        }
    }

    fun alleVilkårOppfylt(formkrav: Form): Boolean {
        return formkrav.alleSvar().all { it == FormVilkår.OPPFYLT }
    }

    private fun alleVilkårBesvart(formkrav: Form): Boolean {
        return formkrav.alleSvar().none { it == FormVilkår.IKKE_SATT }
    }

    fun friteksterUtfylt(formkrav: Form) = formkrav.saksbehandlerBegrunnelse != null &&
            formkrav.saksbehandlerBegrunnelse.isNotBlank() &&
            formkrav.brevtekst != null &&
            formkrav.brevtekst.isNotBlank()

    private fun Form.alleSvar() = setOf(
        klageKonkret,
        klagePart,
        klageSignert,
        klagefristOverholdt
    )
}