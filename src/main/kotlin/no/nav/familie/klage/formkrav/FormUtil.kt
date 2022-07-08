package no.nav.familie.klage.formkrav


import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.domain.FormVilkår
import java.util.UUID

fun form(
    behandlingId: UUID,
    fagsakId: UUID = UUID.randomUUID(),
    klagePart: FormVilkår = FormVilkår.IKKE_SATT,
    klageKonkret: FormVilkår = FormVilkår.IKKE_SATT,
    klagefristOverholdt: FormVilkår = FormVilkår.IKKE_SATT,
    klageSignert: FormVilkår = FormVilkår.IKKE_SATT,
    saksbehandlerBegrunnelse: String = "begrunnelsen kommer her",

    ): Form =
    Form(
        behandlingId,
        fagsakId,
        klagePart,
        klageKonkret,
        klagefristOverholdt,
        klageSignert,
        saksbehandlerBegrunnelse
    )