package no.nav.familie.klage.formkrav.domain

import java.time.LocalDateTime
import java.util.UUID

data class Form(
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
) {

    fun oppfyllerFormkrav(): Boolean = klagePart && klageKonkret && klagefristOverholdt && klageSignert

}