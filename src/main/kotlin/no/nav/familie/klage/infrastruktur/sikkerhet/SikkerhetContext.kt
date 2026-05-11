package no.nav.familie.klage.infrastruktur.sikkerhet

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

object SikkerhetContext {
    private const val SYSTEM_NAVN = "System"
    const val SYSTEM_FORKORTELSE = "VL"

    fun erMaskinTilMaskinToken(): Boolean {
        val oid = hentClaimFraToken<String>("oid")
        val sub = hentClaimFraToken<String>("sub")
        val roles = hentClaimFraToken<List<String>>("roles")
        return oid != null && oid == sub && roles?.contains("access_as_application") == true
    }

    fun erSystembruker(): Boolean = hentSaksbehandler() == SYSTEM_FORKORTELSE

    /**
     * @param strict hvis true - skal kaste feil hvis token ikke inneholder NAVident
     */
    fun hentSaksbehandler(strict: Boolean = false): String =
        hentClaimFraToken<String>("NAVident")
            ?: if (strict) error("Finner ikke NAVident i token") else SYSTEM_FORKORTELSE

    fun hentSaksbehandlerNavn(strict: Boolean = false): String =
        hentClaimFraToken<String>("name")
            ?: if (strict) error("Finner ikke navn på innlogget bruker") else SYSTEM_NAVN

    fun hentGrupperFraToken(): List<String> = hentClaimFraToken<List<String>>("groups") ?: emptyList()

    fun <T> hentClaimFraToken(claim: String): T? = runCatching { hentJwt()!!.getClaim<T>(claim)!! }.getOrNull()

    fun hentJwt(): Jwt? = (SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken)?.token

    fun harRolle(rolle: String): Boolean = hentGrupperFraToken().contains(rolle)
}
