package no.nav.familie.klage.formkrav

import no.nav.familie.klage.behandling.dto.PåklagetVedtakDto
import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.domain.FormVilkår
import no.nav.familie.klage.formkrav.domain.FormkravFristUnntak

object FormUtil {

    fun ferdigUtfylt(formkrav: Form, påklagetVedtak: PåklagetVedtakDto) =
        påklagetVedtak.harTattStillingTil() &&
            alleVilkårBesvart(formkrav) &&
            (alleVilkårOppfylt(formkrav) || friteksterUtfylt(formkrav))

    fun alleVilkårOppfylt(formkrav: Form): Boolean {
        return formkrav.alleSvar().all { it == FormVilkår.OPPFYLT } || oppfyllerMedUnntak(formkrav)
    }

    private fun oppfyllerMedUnntak(formkrav: Form): Boolean {
        val andreErOppfylt = formkrav.alleSvarBortsettFraFrist().all { it == FormVilkår.OPPFYLT }
        val fristIkkeOppfylt = formkrav.klagefristOverholdt == FormVilkår.IKKE_OPPFYLT
        return andreErOppfylt && fristIkkeOppfylt && unntakOppfylt(formkrav)
    }

    private fun unntakOppfylt(formkrav: Form) =
        formkrav.klagefristOverholdtUnntak == FormkravFristUnntak.UNNTAK_SÆRLIG_GRUNN || formkrav.klagefristOverholdtUnntak == FormkravFristUnntak.UNNTAK_KAN_IKKE_LASTES

    fun alleVilkårBesvart(formkrav: Form): Boolean {
        return formkrav.alleSvar().none { it == FormVilkår.IKKE_SATT } && unntakBesvartDersomFristIkkeOppfylt(formkrav)
    }

    private fun unntakBesvartDersomFristIkkeOppfylt(formkrav: Form): Boolean {
        val andreErBesvart = formkrav.alleSvarBortsettFraFrist().none { it == FormVilkår.IKKE_SATT }

        val hvisFristIkkeErOppfyltMåUnntakVæreBesvart = when (formkrav.klagefristOverholdt) {
            FormVilkår.IKKE_OPPFYLT -> formkrav.klagefristOverholdtUnntak != FormkravFristUnntak.IKKE_SATT
            else -> true
        }
        return andreErBesvart && hvisFristIkkeErOppfyltMåUnntakVæreBesvart
    }

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
