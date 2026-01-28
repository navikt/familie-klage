package no.nav.familie.klage.infrastruktur.sikkerhet

import no.nav.security.token.support.spring.SpringTokenValidationContextHolder

object SikkerhetContext {
    private const val SYSTEM_NAVN = "System"
    const val SYSTEM_FORKORTELSE = "VL"

    fun erMaskinTilMaskinToken(): Boolean {
        val claims = SpringTokenValidationContextHolder().getTokenValidationContext().getClaims("azuread")
        return claims.get("oid") != null &&
            claims.get("oid") == claims.get("sub") &&
            claims.getAsList("roles").contains("access_as_application")
    }

    fun erSystembruker(): Boolean = hentSaksbehandler() == SYSTEM_FORKORTELSE

    /**
     * @param strict hvis true - skal kaste feil hvis token ikke inneholder NAVident
     */
    fun hentSaksbehandler(strict: Boolean = false): String {
        val result =
            Result
                .runCatching { SpringTokenValidationContextHolder().getTokenValidationContext() }
                .fold(
                    onSuccess = {
                        it.getClaims("azuread")?.get("NAVident")?.toString() ?: SYSTEM_FORKORTELSE
                    },
                    onFailure = { SYSTEM_FORKORTELSE },
                )
        if (strict && result == SYSTEM_FORKORTELSE) {
            error("Finner ikke NAVident i token")
        }
        return result
    }

    fun hentSaksbehandlerNavn(strict: Boolean = false): String =
        Result
            .runCatching { SpringTokenValidationContextHolder().getTokenValidationContext() }
            .fold(
                onSuccess = {
                    it.getClaims("azuread")?.get("name")?.toString()
                        ?: if (strict) error("Finner ikke navn i azuread token") else SYSTEM_NAVN
                },
                onFailure = { if (strict) error("Finner ikke navn på innlogget bruker") else SYSTEM_NAVN },
            )

    fun hentGrupperFraToken(): List<String> =
        Result
            .runCatching { SpringTokenValidationContextHolder().getTokenValidationContext() }
            .fold(
                onSuccess = {
                    @Suppress("UNCHECKED_CAST")
                    it.getClaims("azuread")?.get("groups") as List<String>? ?: emptyList()
                },
                onFailure = { emptyList() },
            )

    fun harRolle(rolle: String): Boolean = hentGrupperFraToken().contains(rolle)
}
