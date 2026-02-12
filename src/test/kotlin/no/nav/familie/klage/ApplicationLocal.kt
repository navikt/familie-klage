package no.nav.familie.klage

import no.nav.familie.klage.infrastruktur.config.ApplicationConfig
import no.nav.familie.klage.infrastruktur.db.DbContainerInitializer
import no.nav.familie.kontrakter.felles.jsonMapper
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.web.client.RestTemplate
import java.time.Duration
import java.time.temporal.ChronoUnit

@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class])
class ApplicationLocal

fun main(args: Array<String>) {
    SpringApplicationBuilder(ApplicationConfig::class.java)
        .initializers(DbContainerInitializer())
        .profiles(
            "local",
            "mock-integrasjoner",
            "mock-auditlogger",
            "mock-pdl",
            "mock-brev",
            "mock-ef-sak",
            "mock-ks-sak",
            "mock-ba-sak",
            "mock-dokument",
            "mock-kabal",
            "mock-ereg",
            "mock-fullmakt",
        ).run(*args)
}

/**
 * Overskriver felles sin som bruker proxy, som ikke skal brukes på gcp.
 */
@Bean
@Primary
fun restTemplateBuilder(): RestTemplateBuilder {
    val jacksonJsonHttpMessageConverter = JacksonJsonHttpMessageConverter(jsonMapper)
    return RestTemplateBuilder()
        .connectTimeout(Duration.of(2, ChronoUnit.SECONDS))
        .readTimeout(Duration.of(30, ChronoUnit.SECONDS))
        .additionalMessageConverters(listOf(jacksonJsonHttpMessageConverter) + RestTemplate().messageConverters)
}
