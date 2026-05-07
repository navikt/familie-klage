package no.nav.familie.klage.infrastruktur.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.familie.klage.infrastruktur.config.JsonMapperProvider.jsonMapper
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Ressurs.Companion.failure
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.access.intercept.RequestAuthorizationContext

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val rolleConfig: RolleConfig,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() }
            authorizeHttpRequests {
                authorize("/internal/**", permitAll)
                authorize("/swagger-ui.html", permitAll)
                authorize("/swagger-ui/**", permitAll)
                authorize("/v3/api-docs", permitAll)
                authorize("/v3/api-docs/**", permitAll)
                authorize("/api/ping", permitAll)
                authorize(anyRequest, authorizationManager())
            }
            oauth2ResourceServer {
                jwt { }
            }
            exceptionHandling {
                accessDeniedHandler = accessDeniedHandler()
                authenticationEntryPoint = authenticationEntryPoint()
            }
        }
        return http.build()
    }

    /** Gir tilgang hvis JWT-tokenet tilhører en kjent AD-gruppe (veileder/saksbehandler/beslutter) eller er et maskin-til-maskin-token. */
    private fun authorizationManager(): AuthorizationManager<RequestAuthorizationContext> =
        AuthorizationManager { authSupplier, _ ->
            val jwt =
                (authSupplier.get() as? JwtAuthenticationToken)?.token
                    ?: return@AuthorizationManager AuthorizationDecision(false)

            val grupper = jwt.getClaimAsStringList("groups") ?: emptyList()
            val oid = jwt.getClaimAsString("oid")
            val sub = jwt.getClaimAsString("sub")
            val roles = jwt.getClaimAsStringList("roles") ?: emptyList()

            val erMaskinTilMaskin = oid != null && oid == sub && "access_as_application" in roles
            val harInternTilgang =
                erMaskinTilMaskin ||
                    grupper.any { it in rolleConfig.veilederRoller } ||
                    grupper.any { it in rolleConfig.saksbehandlerRoller } ||
                    grupper.any { it in rolleConfig.beslutterRoller }

            AuthorizationDecision(harInternTilgang)
        }

    /** Returnerer 401 med strukturert feilmelding når forespørselen mangler eller har ugyldig token. */
    private fun authenticationEntryPoint(): AuthenticationEntryPoint =
        AuthenticationEntryPoint { _: HttpServletRequest, response: HttpServletResponse, _: AuthenticationException ->
            response.apply {
                status = HttpServletResponse.SC_UNAUTHORIZED
                contentType = MediaType.APPLICATION_JSON_VALUE
                characterEncoding = "UTF-8"
                jsonMapper.writeValue(
                    writer,
                    failure<String>(
                        errorMessage = "401 Unauthorized",
                        frontendFeilmelding = "Kall ikke autorisert",
                    ),
                )
            }
        }

    /** Returnerer 403 med strukturert feilmelding når autentisert bruker mangler tilgang. */
    private fun accessDeniedHandler(): AccessDeniedHandler =
        AccessDeniedHandler { _: HttpServletRequest, response: HttpServletResponse, _: AccessDeniedException ->
            response.apply {
                status = HttpServletResponse.SC_FORBIDDEN
                contentType = MediaType.APPLICATION_JSON_VALUE
                characterEncoding = "UTF-8"
                jsonMapper.writeValue(
                    writer,
                    Ressurs(
                        data = null,
                        status = Ressurs.Status.IKKE_TILGANG,
                        melding = "Bruker har ikke tilgang til saksbehandlingsløsningen",
                        frontendFeilmelding = "Du mangler tilgang til denne saksbehandlingsløsningen",
                        stacktrace = null,
                    ),
                )
            }
        }
}
