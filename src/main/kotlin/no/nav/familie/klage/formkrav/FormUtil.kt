package no.nav.familie.klage.formkrav

import no.nav.familie.klage.behandling.dto.PåklagetVedtakDto
import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.domain.FormVilkår

object FormUtil {

    fun ferdigUtfylt(formkrav: Form, påklagetVedtak: PåklagetVedtakDto) =
            påklagetVedtak.harTattStillingTil() &&
            alleVilkårBesvart(formkrav) &&
            (alleVilkårOppfylt(formkrav) || friteksterUtfylt(formkrav))

    fun alleVilkårOppfylt(formkrav: Form): Boolean {
        return formkrav.alleSvar().all { it == FormVilkår.OPPFYLT }
    }

    fun alleVilkårBesvart(formkrav: Form): Boolean {
        return formkrav.alleSvar().none { it == FormVilkår.IKKE_SATT}
    }

    fun friteksterUtfylt(formkrav: Form) = formkrav.saksbehandlerBegrunnelse != null
                                           && formkrav.saksbehandlerBegrunnelse.isNotBlank()
                                           && formkrav.brevtekst != null
                                           && formkrav.brevtekst.isNotBlank()
    private fun Form.alleSvar() = setOf(
        klageKonkret,
        klagePart,
        klageSignert,
        klagefristOverholdt
    )
}
