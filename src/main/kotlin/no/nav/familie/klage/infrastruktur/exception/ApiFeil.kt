package no.nav.familie.klage.infrastruktur.exception

import org.springframework.http.HttpStatus

class ApiFeil(val feilmelding: String, val httpStatus: HttpStatus) : RuntimeException(feilmelding) {
    companion object Fabrikk {
        fun badRequest(feilmelding: String): ApiFeil {
            return ApiFeil(feilmelding, HttpStatus.BAD_REQUEST)
        }
    }
}
