package no.nav.familie.klage.infrastruktur.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.familie.klage.infrastruktur.config.JsonMapperProvider.jsonMapper
import no.nav.familie.klage.infrastruktur.sikkerhet.AzureJwtAuthenticationConverter
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Ressurs.Companion.failure
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val azureJwtAuthenticationConverter: AzureJwtAuthenticationConverter,
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
                authorize(anyRequest, hasRole("VEILEDER"))
            }
            oauth2ResourceServer {
                jwt {
                    jwtAuthenticationConverter = azureJwtAuthenticationConverter
                }
            }
            exceptionHandling {
                accessDeniedHandler = accessDeniedHandler()
                authenticationEntryPoint = authenticationEntryPoint()
            }
        }
        return http.build()
    }

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
                        frontendFeilmelding = "En uventet feil oppstod: Kall ikke autorisert",
                    ),
                )
            }
        }

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
