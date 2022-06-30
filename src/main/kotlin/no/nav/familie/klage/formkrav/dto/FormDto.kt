package no.nav.familie.klage.formkrav.dto

import no.nav.familie.klage.formkrav.domain.Form
import java.time.LocalDateTime
import java.util.UUID

data class FormDto (
    val behandlingsId: UUID,
    val fagsakId: UUID,
    val vedtaksdato: LocalDateTime,

    val klageMottat: LocalDateTime,
    val klageÅrsak: String,
    val klageBeskrivelse: String,

    val klagePart: Boolean,
    val klageKonkret: Boolean,
    val klagefristOverholdt: Boolean,
    val klageSignert: Boolean,

    val saksbehandlerBegrunnelse: String,
    val sakSistEndret: LocalDateTime,

    val fullført: Boolean
)

fun Form.tilDto():  FormDto =
    FormDto(
        behandlingsId = this.behandlingsId,
        fagsakId = this.fagsakId,
        vedtaksdato = this.vedtaksdato,

        klageMottat = this.klageMottat,
        klageÅrsak = this.klageÅrsak,
        klageBeskrivelse = this.klageBeskrivelse,

        klagePart = this.klagePart,
        klageKonkret = this.klageKonkret,
        klagefristOverholdt = this.klagefristOverholdt,
        klageSignert = this.klageSignert,

        saksbehandlerBegrunnelse = this.saksbehandlerBegrunnelse,
        sakSistEndret = this.sakSistEndret,

        fullført = this.fullført
    )