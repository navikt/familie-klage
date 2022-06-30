package no.nav.familie.klage.formkrav.dto

import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.domain.FormVilkår
import java.time.LocalDateTime
import java.util.UUID

data class FormDto (
    val id: UUID,
    val fagsakId: UUID,
    val vedtaksdato: LocalDateTime,

    val klageMottat: LocalDateTime,
    val klageÅrsak: String,
    val klageBeskrivelse: String,

    val klagePart: FormVilkår,
    val klageKonkret: FormVilkår,
    val klagefristOverholdt: FormVilkår,
    val klageSignert: FormVilkår,

    val saksbehandlerBegrunnelse: String,
    val sakSistEndret: LocalDateTime,

    val vilkårStatus: FormVilkår
)

fun Form.tilDto():  FormDto =
    FormDto(
        id = this.id,
        fagsakId = this.fagsakId,
        vedtaksdato = this.vedtaksdato,

        klageMottat = this.klageMottat,
        klageÅrsak = this.klageaarsak,
        klageBeskrivelse = this.klageBeskrivelse,

        klagePart = this.klagePart,
        klageKonkret = this.klageKonkret,
        klagefristOverholdt = this.klagefristOverholdt,
        klageSignert = this.klageSignert,

        saksbehandlerBegrunnelse = this.saksbehandlerBegrunnelse,
        sakSistEndret = this.sakSistEndret,

        vilkårStatus = this.vilkaarStatus
    )