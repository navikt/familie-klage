package no.nav.familie.klage.behandling.domain

enum class BehandlingType(val visningsnavn: String) {
    FØRSTEGANGSBEHANDLING("Førstegangsbehandling"),
    BLANKETT("Blankett"),
    REVURDERING("Revurdering"),
    TEKNISK_OPPHØR("Teknisk opphør")
}

enum class BehandlingResultat(val displayName: String) {
    INNVILGET(displayName = "Innvilget"),
    OPPHØRT(displayName = "Opphørt"),
    AVSLÅTT(displayName = "Avslått"),
    IKKE_SATT(displayName = "Ikke satt"),
    HENLAGT(displayName = "Henlagt"),
}

enum class BehandlingStatus {
    OPPRETTET,
    UTREDES,
    FATTER_VEDTAK,
    IVERKSETTER_VEDTAK,
    FERDIGSTILT,
    SATT_PÅ_VENT,
    ;

    fun behandlingErLåstForVidereRedigering(): Boolean =
            setOf(FATTER_VEDTAK, IVERKSETTER_VEDTAK, FERDIGSTILT).contains(this)
}

enum class BehandlingSteg {
    FORMALKRAV,
    VURDERING,
    KABAL,
    BEHANDLING_FERDIGSTILT,
}