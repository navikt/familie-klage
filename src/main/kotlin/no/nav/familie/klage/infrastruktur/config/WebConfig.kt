package no.nav.familie.klage.infrastruktur.config

import no.nav.familie.klage.infrastruktur.sikkerhet.TilgangInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val tilgangInterceptor: TilgangInterceptor,
) : WebMvcConfigurer {
    private val excludePatterns =
        listOf(
            "/api/task/**",
            "/api/v2/task/**",
            "/internal/**",
            "/swagger-resources/**",
            "/swagger-resources",
            "/swagger-ui/**",
            "/swagger-ui",
            "/v2/api-docs/**",
            "/v2/api-docs",
        )

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(tilgangInterceptor).excludePathPatterns(excludePatterns)
        super.addInterceptors(registry)
    }
}
