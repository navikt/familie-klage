package no.nav.familie.klage.formkrav.dto

import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.domain.FormVilkår
import java.time.LocalDateTime
import java.util.UUID

data class FormDto(
    val behandlingId: UUID,
    val fagsakId: UUID,
    val klagePart: FormVilkår,
    val klageKonkret: FormVilkår,
    val klagefristOverholdt: FormVilkår,
    val klageSignert: FormVilkår,
    val saksbehandlerBegrunnelse: String,
    val endretTid: LocalDateTime
)

fun Form.tilDto(): FormDto =
    FormDto(
        behandlingId = this.behandlingId,
        fagsakId = this.fagsakId,
        klagePart = this.klagePart,
        klageKonkret = this.klageKonkret,
        klagefristOverholdt = this.klagefristOverholdt,
        klageSignert = this.klageSignert,
        saksbehandlerBegrunnelse = this.saksbehandlerBegrunnelse,
        endretTid = this.sporbar.endret.endretTid
    )
