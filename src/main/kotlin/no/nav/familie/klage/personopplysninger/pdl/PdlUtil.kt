package no.nav.familie.klage.personopplysninger.pdl

import no.nav.familie.klage.infrastruktur.exception.PdlNotFoundException
import no.nav.familie.klage.infrastruktur.exception.PdlRequestException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")
val logger: Logger = LoggerFactory.getLogger(PdlClient::class.java)

inline fun <reified DATA : Any, reified T : Any> feilsjekkOgReturnerData(
    ident: String?,
    pdlResponse: PdlResponse<DATA>,
    dataMapper: (DATA) -> T?,
): T {
    if (pdlResponse.harFeil()) {
        if (pdlResponse.errors?.any { it.extensions?.notFound() == true } == true) {
            throw PdlNotFoundException()
        }
        secureLogger.error("Feil ved henting av ${T::class} fra PDL: ${pdlResponse.errorMessages()}")
        throw PdlRequestException("Feil ved henting av ${T::class} fra PDL. Se secure logg for detaljer.")
    }
    if (pdlResponse.harAdvarsel()) {
        logger.warn("Advarsel ved henting av ${T::class} fra PDL. Se securelogs for detaljer.")
        secureLogger.warn("Advarsel ved henting av ${T::class} fra PDL: ${pdlResponse.extensions?.warnings}")
    }

    val data = dataMapper.invoke(pdlResponse.data)
    if (data == null) {
        val errorMelding = if (ident != null) "Feil ved oppslag på ident $ident. " else "Feil ved oppslag på person."
        secureLogger.error(
            errorMelding +
                "PDL rapporterte ingen feil men returnerte tomt datafelt",
        )
        throw PdlRequestException("Manglende ${T::class} ved feilfri respons fra PDL. Se secure logg for detaljer.")
    }
    return data
}

inline fun <reified T : Any> feilsjekkOgReturnerData(pdlResponse: PdlBolkResponse<T>): Map<String, T> {
    if (pdlResponse.data == null) {
        secureLogger.error("Data fra pdl er null ved bolkoppslag av ${T::class} fra PDL: ${pdlResponse.errorMessages()}")
        throw PdlRequestException("Data er null fra PDL -  ${T::class}. Se secure logg for detaljer.")
    }
    if (pdlResponse.harAdvarsel()) {
        logger.warn("Advarsel ved henting av ${T::class} fra PDL. Se securelogs for detaljer.")
        secureLogger.warn("Advarsel ved henting av ${T::class} fra PDL: ${pdlResponse.extensions?.warnings}")
    }
    val feil =
        pdlResponse.data.personBolk
            .filter { it.code != "ok" }
            .associate { it.ident to it.code }
    if (feil.isNotEmpty()) {
        secureLogger.error("Feil ved henting av ${T::class} fra PDL: $feil")
        throw PdlRequestException("Feil ved henting av ${T::class} fra PDL. Se secure logg for detaljer.")
    }
    return pdlResponse.data.personBolk.associateBy({ it.ident }, { it.person!! })
}
