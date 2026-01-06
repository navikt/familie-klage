package no.nav.familie.klage.infrastruktur.exception

import org.springframework.http.HttpStatus
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

open class Feil(
    message: String,
    val frontendFeilmelding: String? = null,
    val httpStatus: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    throwable: Throwable? = null,
) : RuntimeException(message, throwable) {
    constructor(message: String, throwable: Throwable?, httpStatus: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR) :
        this(message, null, httpStatus, throwable)
}

@OptIn(ExperimentalContracts::class)
inline fun feilHvis(
    boolean: Boolean,
    httpStatus: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    lazyMessage: () -> String,
) {
    contract {
        returns() implies !boolean
    }
    if (boolean) {
        throw Feil(message = lazyMessage(), frontendFeilmelding = lazyMessage(), httpStatus)
    }
}

@OptIn(ExperimentalContracts::class)
inline fun brukerfeilHvis(
    boolean: Boolean,
    httpStatus: HttpStatus = HttpStatus.BAD_REQUEST,
    lazyMessage: () -> String,
) {
    contract {
        returns() implies !boolean
    }
    if (boolean) {
        throw ApiFeil(feilmelding = lazyMessage(), httpStatus = httpStatus)
    }
}

inline fun feilHvisIkke(
    boolean: Boolean,
    httpStatus: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    lazyMessage: () -> String,
) {
    feilHvis(!boolean, httpStatus) { lazyMessage() }
}

inline fun brukerfeilHvisIkke(
    boolean: Boolean,
    httpStatus: HttpStatus = HttpStatus.BAD_REQUEST,
    lazyMessage: () -> String,
) {
    brukerfeilHvis(!boolean, httpStatus) { lazyMessage() }
}

class ManglerTilgang(
    val melding: String,
    val frontendFeilmelding: String,
) : RuntimeException(melding)
