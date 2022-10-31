package no.nav.familie.klage.formkrav

import no.nav.familie.klage.behandling.dto.PåklagetVedtakDto
import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.domain.FormVilkår

object FormUtil {

    fun ferdigUtfylt(formkrav: Form, påklagetVedtak: PåklagetVedtakDto) =
            påklagetVedtak.harTattStillingTil() &&
            alleVilkårBesvart(formkrav) &&
            formkrav.saksbehandlerBegrunnelse.isNotBlank()

    fun alleVilkårOppfylt(formkrav: Form): Boolean {
        return formkrav.alleSvar().all { it == FormVilkår.OPPFYLT }
    }

    fun alleVilkårBesvart(formkrav: Form): Boolean {
        return formkrav.alleSvar().none { it == FormVilkår.IKKE_SATT}
    }

    fun begrunnelseUtfylt(formkrav: Form) = formkrav.saksbehandlerBegrunnelse.isNotBlank()

    private fun Form.alleSvar() = setOf(
        klageKonkret,
        klagePart,
        klageSignert,
        klagefristOverholdt
    )
}
