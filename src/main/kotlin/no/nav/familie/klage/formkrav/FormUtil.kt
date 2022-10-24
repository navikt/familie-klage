package no.nav.familie.klage.formkrav

import no.nav.familie.klage.behandling.dto.PåklagetVedtakDto
import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.domain.FormVilkår

object FormUtil {

    fun formkravErFerdigUtfyllt(form: Form, påklagetVedtak: PåklagetVedtakDto) =
        form.alleSvar().none { it == FormVilkår.IKKE_SATT } &&
            form.saksbehandlerBegrunnelse.isNotBlank() &&
            påklagetVedtak.harTattStillingTil()

    fun formkravErOppfylt(form: Form): Boolean {
        return form.alleSvar().all { it == FormVilkår.OPPFYLT }
    }

    private fun Form.alleSvar() = setOf(
        klageKonkret,
        klagePart,
        klageSignert,
        klagefristOverholdt
    )
}
