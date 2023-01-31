package no.nav.familie.klage.testutil

import com.nimbusds.jwt.JWTClaimsSet
import io.mockk.every
import io.mockk.mockk
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import javax.servlet.http.HttpServletRequest

object BrukerContextUtil {

    fun clearBrukerContext() {
        RequestContextHolder.resetRequestAttributes()
    }

    fun mockBrukerContext(
        preferredUsername: String = "A",
        groups: List<String> = emptyList(),
        servletRequest: HttpServletRequest = MockHttpServletRequest(),
    ) {
        val tokenValidationContext = mockk<TokenValidationContext>()
        val jwtTokenClaims = JwtTokenClaims(
            JWTClaimsSet.Builder()
                .claim("preferred_username", preferredUsername)
                .claim("NAVident", preferredUsername)
                .claim("name", preferredUsername)
                .claim("groups", groups)
                .build(),
        )
        val requestAttributes = ServletRequestAttributes(servletRequest)

        RequestContextHolder.setRequestAttributes(requestAttributes)
        requestAttributes.setAttribute(
            SpringTokenValidationContextHolder::class.java.name,
            tokenValidationContext,
            RequestAttributes.SCOPE_REQUEST,
        )
        every { tokenValidationContext.getClaims("azuread") } returns jwtTokenClaims
    }

    fun <T> testWithBrukerContext(preferredUsername: String = "A", groups: List<String> = emptyList(), fn: () -> T): T {
        try {
            mockBrukerContext(preferredUsername, groups)
            return fn()
        } finally {
            clearBrukerContext()
        }
    }
}
