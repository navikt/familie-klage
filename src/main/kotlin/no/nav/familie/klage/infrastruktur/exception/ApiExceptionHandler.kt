package no.nav.familie.klage.infrastruktur.exception

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.exceptions.JwtTokenMissingException
import org.slf4j.LoggerFactory
import org.springframework.core.NestedExceptionUtils
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.async.AsyncRequestNotUsableException

@ControllerAdvice
class ApiExceptionHandler {
    private val logger = LoggerFactory.getLogger(ApiExceptionHandler::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    private fun rootCause(throwable: Throwable): String = NestedExceptionUtils.getMostSpecificCause(throwable).javaClass.simpleName

    @ExceptionHandler(Throwable::class)
    fun handleThrowable(throwable: Throwable): ResponseEntity<Ressurs<Nothing>> {
        val metodeSomFeiler = finnMetodeSomFeiler(throwable)
        secureLogger.error("Uventet feil: $metodeSomFeiler ${rootCause(throwable)}", throwable)
        logger.error("Uventet feil: $metodeSomFeiler ${rootCause(throwable)} ")

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Ressurs.failure(errorMessage = "Uventet feil", frontendFeilmelding = "En uventet feil oppstod."))
    }

    @ExceptionHandler(JwtTokenMissingException::class)
    fun handleJwtTokenMissingException(jwtTokenMissingException: JwtTokenMissingException): ResponseEntity<Ressurs<Nothing>> =
        ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(
                Ressurs.failure(
                    errorMessage = "401 Unauthorized JwtTokenMissingException",
                    frontendFeilmelding = "En uventet feil oppstod: Kall ikke autorisert",
                ),
            )

    @ExceptionHandler(ApiFeil::class)
    fun handleThrowable(feil: ApiFeil): ResponseEntity<Ressurs<Nothing>> {
        val metodeSomFeiler = finnMetodeSomFeiler(feil)
        secureLogger.info("En håndtert feil har oppstått(${feil.httpStatus}): ${feil.feilmelding}", feil)
        logger.info(
            "En håndtert feil har oppstått(${feil.httpStatus}) metode=$metodeSomFeiler exception=${rootCause(feil)}: ${feil.message} ",
        )
        return ResponseEntity.status(feil.httpStatus).body(
            Ressurs.funksjonellFeil(
                frontendFeilmelding = feil.feilmelding,
                melding = feil.feilmelding,
            ),
        )
    }

    @ExceptionHandler(Feil::class)
    fun handleThrowable(feil: Feil): ResponseEntity<Ressurs<Nothing>> {
        val metodeSomFeiler = finnMetodeSomFeiler(feil)
        secureLogger.error("En håndtert feil har oppstått(${feil.httpStatus}): ${feil.frontendFeilmelding}", feil)
        logger.error(
            "En håndtert feil har oppstått(${feil.httpStatus}) metode=$metodeSomFeiler exception=${rootCause(feil)}: ${feil.message} ",
        )
        return ResponseEntity.status(feil.httpStatus).body(Ressurs.failure(frontendFeilmelding = feil.frontendFeilmelding))
    }

    @ExceptionHandler(PdlNotFoundException::class)
    fun handleThrowable(feil: PdlNotFoundException): ResponseEntity<Ressurs<Nothing>> {
        logger.warn("Finner ikke personen i PDL")
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Ressurs.failure(frontendFeilmelding = "Finner ikke personen"))
    }

    @ExceptionHandler(ManglerTilgang::class)
    fun handleThrowable(manglerTilgang: ManglerTilgang): ResponseEntity<Ressurs<Nothing>> {
        secureLogger.warn("En håndtert tilgangsfeil har oppstått - ${manglerTilgang.melding}", manglerTilgang)
        logger.warn("En håndtert tilgangsfeil har oppstått")
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(
                Ressurs(
                    data = null,
                    status = Ressurs.Status.IKKE_TILGANG,
                    frontendFeilmelding = manglerTilgang.frontendFeilmelding,
                    melding = manglerTilgang.melding,
                    stacktrace = null,
                ),
            )
    }

    @ExceptionHandler(IntegrasjonException::class)
    fun handleThrowable(feil: IntegrasjonException): ResponseEntity<Ressurs<Nothing>> {
        secureLogger.error("Feil mot integrasjonsclienten har oppstått: uri={} data={}", feil.uri, feil.data, feil)
        logger.error("Feil mot integrasjonsclienten har oppstått exception=${rootCause(feil)}")
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Ressurs.failure(frontendFeilmelding = feil.message))
    }

    fun finnMetodeSomFeiler(e: Throwable): String {
        val firstElement =
            e.stackTrace.firstOrNull {
                it.className.startsWith("no.nav.familie.klage") &&
                    !it.className.contains("$") &&
                    !it.className.contains("InsertUpdateRepositoryImpl")
            }
        if (firstElement != null) {
            val className = firstElement.className.split(".").lastOrNull()
            return "$className::${firstElement.methodName}(${firstElement.lineNumber})"
        }
        return e.cause?.let { finnMetodeSomFeiler(it) } ?: "(Ukjent metode som feiler)"
    }

    /**
     * AsyncRequestNotUsableException er en exception som blir kastet når en async request blir avbrutt. Velger
     * å skjule denne exceptionen fra loggen da den ikke er interessant for oss.
     */
    @ExceptionHandler(AsyncRequestNotUsableException::class)
    fun handlAsyncRequestNotUsableException(e: AsyncRequestNotUsableException): ResponseEntity<Any> {
        logger.info("En AsyncRequestNotUsableException har oppstått, som skjer når en async request blir avbrutt", e)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
    }
}
