package no.nav.familie.klage

import no.nav.familie.klage.infrastruktur.config.ApplicationConfig
import no.nav.familie.klage.infrastruktur.exception.Feil
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration
import org.springframework.context.annotation.Import
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Properties

@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class])
@Import(ApplicationConfig::class)
class ApplicationLocalPostgres

fun main(args: Array<String>) {
    val properties = Properties()
    properties["DATASOURCE_URL"] = "jdbc:postgresql://localhost:5433/familie-klage"
    properties["DATASOURCE_USERNAME"] = "postgres"
    properties["DATASOURCE_PASSWORD"] = "test"
    properties["DATASOURCE_DRIVER"] = "org.postgresql.Driver"

    if (!args.contains("--manuellMiljø") && System.getProperty("AZURE_APP_CLIENT_ID") == null) {
        settClientIdOgSecret()
    }

    SpringApplicationBuilder(ApplicationLocalPostgres::class.java)
        .profiles(
            "local",
            "mock-integrasjoner",
            "mock-pdl",
            "mock-brev",
            "mock-dokument",
            "mock-ef-sak",
            "mock-ks-sak",
            "mock-ba-sak",
            "mock-kabal",
            "mock-ereg",
            "mock-inntekt",
            "mock-fullmakt",
            "mock-featuretoggle",
        ).properties(properties)
        .run(*args)
}

private fun settClientIdOgSecret() {
    val cmd = "src/test/resources/hentMiljøvariabler.sh"

    val process = ProcessBuilder(cmd).start()

    if (process.waitFor() != 0) {
        val inputStream = BufferedReader(InputStreamReader(process.inputStream))
        inputStream.lines().forEach { println(it) }
        inputStream.close()
        throw Feil("Klarte ikke hente variabler fra Nais. Er du logget på Naisdevice og gcloud?")
    }

    val inputStream = BufferedReader(InputStreamReader(process.inputStream))
    inputStream.readLine() // "Switched to context dev-gcp"
    inputStream
        .readLine()
        .split(";")
        .map { it.split("=", limit = 2) }
        .forEach { System.setProperty(it[0], it[1]) }
    inputStream.close()
}
