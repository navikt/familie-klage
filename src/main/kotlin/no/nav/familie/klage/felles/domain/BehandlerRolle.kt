package no.nav.familie.klage.felles.domain

@Suppress("unused")
enum class BehandlerRolle(
    val nivå: Int,
) {
    SYSTEM(4),
    BESLUTTER(3),
    SAKSBEHANDLER(2),
    VEILEDER(1),
    UKJENT(0),
}
