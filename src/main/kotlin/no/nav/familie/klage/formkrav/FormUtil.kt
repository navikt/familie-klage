package no.nav.familie.klage.formkrav

import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.domain.FormVilkår
import java.util.UUID

object FormUtil {

    fun initiellForm(
        behandlingId: UUID,
        klagePart: FormVilkår = FormVilkår.IKKE_SATT,
        klageKonkret: FormVilkår = FormVilkår.IKKE_SATT,
        klagefristOverholdt: FormVilkår = FormVilkår.IKKE_SATT,
        klageSignert: FormVilkår = FormVilkår.IKKE_SATT,
        saksbehandlerBegrunnelse: String = ""
    ): Form =
        Form(
            behandlingId,
            klagePart,
            klageKonkret,
            klagefristOverholdt,
            klageSignert,
            saksbehandlerBegrunnelse
        )

    fun formkravErFerdigUtfyllt(form: Form) =
        form.alleSvar().none { it == FormVilkår.IKKE_SATT } &&
            form.saksbehandlerBegrunnelse.isNotBlank()

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
