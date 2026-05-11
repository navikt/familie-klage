package no.nav.familie.klage.testutil

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.time.Instant
import java.util.UUID

object BrukerContextUtil {
    fun clearBrukerContext() {
        SecurityContextHolder.clearContext()
    }

    fun mockBrukerContext(
        preferredUsername: String = "A",
        groups: List<String> = emptyList(),
    ) {
        val oid = UUID.randomUUID().toString()
        val sub = UUID.randomUUID().toString()
        val jwt =
            Jwt
                .withTokenValue("mock-token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("preferred_username", preferredUsername)
                .claim("NAVident", preferredUsername)
                .claim("name", preferredUsername)
                .claim("oid", oid)
                .claim("sub", sub)
                .claim("groups", groups)
                .build()
        SecurityContextHolder.setContext(SecurityContextImpl(JwtAuthenticationToken(jwt)))
    }

    fun <T> testWithBrukerContext(
        preferredUsername: String = "A",
        groups: List<String> = emptyList(),
        fn: () -> T,
    ): T {
        try {
            mockBrukerContext(preferredUsername, groups)
            return fn()
        } finally {
            clearBrukerContext()
        }
    }
}
