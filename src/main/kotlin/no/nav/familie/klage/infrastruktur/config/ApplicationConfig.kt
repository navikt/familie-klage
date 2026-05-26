package no.nav.familie.klage.infrastruktur.config

import no.nav.familie.log.NavSystemtype
import no.nav.familie.log.filter.LogFilter
import no.nav.familie.log.filter.RequestTimeFilter
import no.nav.familie.sikkerhet.context.FamilieFellesSpringSecurityKonfigurasjon
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.converter.HttpMessageConverters
import org.springframework.http.converter.yaml.MappingJackson2YamlHttpMessageConverter
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@SpringBootConfiguration
@ConfigurationPropertiesScan
@ComponentScan(
    "no.nav.familie.prosessering",
    "no.nav.familie.klage",
    "no.nav.familie.sikkerhet",
    "no.nav.familie.unleash",
    "no.nav.familie.felles.tokenklient",
)
@Import(FamilieFellesSpringSecurityKonfigurasjon::class)
@EnableScheduling
class ApplicationConfig {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Bean
    @Primary
    fun objectMapper() = JsonMapperProvider.jsonMapper

    @Bean
    fun logFilter(): FilterRegistrationBean<LogFilter> {
        logger.info("Registering LogFilter filter")
        val filterRegistration = FilterRegistrationBean<LogFilter>()
        filterRegistration.setFilter(LogFilter(systemtype = NavSystemtype.NAV_INTEGRASJON))
        filterRegistration.order = 1
        return filterRegistration
    }

    @Bean
    fun requestTimeFilter(): FilterRegistrationBean<RequestTimeFilter> {
        logger.info("Registering RequestTimeFilter filter")
        val filterRegistration = FilterRegistrationBean<RequestTimeFilter>()
        filterRegistration.setFilter(RequestTimeFilter())
        filterRegistration.order = 2
        return filterRegistration
    }

    @Bean
    fun removeYamlConverter(): WebMvcConfigurer =
        object : WebMvcConfigurer {
            override fun configureMessageConverters(builder: HttpMessageConverters.ServerBuilder) {
                builder.configureMessageConvertersList { converters ->
                    converters.removeIf { it is MappingJackson2YamlHttpMessageConverter }
                }
            }
        }
}
