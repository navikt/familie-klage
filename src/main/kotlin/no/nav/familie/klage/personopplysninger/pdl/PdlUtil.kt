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
    dataMapper: (DATA) -> T?
): T {
    if (pdlResponse.harFeil()) {
        if (pdlResponse.errors?.any { it.extensions?.notFound() == true } == true) {
            throw PdlNotFoundException()
        }
        secureLogger.error("Feil ved henting av ${T::class} fra PDL: ${pdlResponse.errorMessages()}")
        throw PdlRequestException("Feil ved henting av ${T::class} fra PDL. Se secure logg for detaljer.")
    }

    val data = dataMapper.invoke(pdlResponse.data)
    if (data == null) {
        val errorMelding = if (ident != null) "Feil ved oppslag på ident $ident. " else "Feil ved oppslag på person."
        secureLogger.error(
            errorMelding +
                "PDL rapporterte ingen feil men returnerte tomt datafelt"
        )
        throw PdlRequestException("Manglende ${T::class} ved feilfri respons fra PDL. Se secure logg for detaljer.")
    }
    return data
}
