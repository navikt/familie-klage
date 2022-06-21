package no.nav.familie.ef.sak.brev.dto


data class VedtaksbrevDto(
    val saksbehandlerBrevrequest: String,
    val brevmal: String,
    val saksbehandlersignatur: String,
    val besluttersignatur: String? = null,
    val enhet: String? = null,
    val skjulBeslutterSignatur: Boolean
)
