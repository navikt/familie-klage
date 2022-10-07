package no.nav.familie.klage.brev.dto

data class VedtaksbrevDto(
    val saksbehandlerBrevrequest: String,
    val brevmal: String,
    val saksbehandlersignatur: String,
    val besluttersignatur: String? = null,
    val enhet: String? = null,
    val skjulBeslutterSignatur: Boolean
)
