package no.nav.familie.klage

import no.nav.familie.klage.infrastruktur.config.ApplicationConfig
import no.nav.familie.klage.infrastruktur.db.DbContainerInitializer
import no.nav.familie.klage.infrastruktur.exception.Feil
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration
import java.io.BufferedReader
import java.io.InputStreamReader

@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class])
class ApplicationLocal

fun main(args: Array<String>) {
    if (!args.contains("--manuellMiljø") && System.getProperty("AZURE_APP_CLIENT_ID") == null) {
        settClientIdOgSecret()
    }

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
            "mock-inntekt",
            "mock-fullmakt",
            "mock-featuretoggle",
        ).run(*args)
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
