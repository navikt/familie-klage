package no.nav.familie.klage.infrastruktur.sikkerhet

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.familie.klage.felles.domain.BehandlerRolle
import no.nav.familie.klage.infrastruktur.exception.ManglerTilgang
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.AsyncHandlerInterceptor

@Component
class TilgangInterceptor(
    private val tilgangService: TilgangService,
) : AsyncHandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean =
        if (tilgangService.harMinimumRolleTversFagsystem(BehandlerRolle.VEILEDER)) {
            super.preHandle(request, response, handler)
        } else {
            logger.warn("Saksbehandler ${SikkerhetContext.hentSaksbehandler()} har ikke tilgang til saksbehandlingsløsningen")
            throw ManglerTilgang(
                melding = "Bruker har ikke tilgang til saksbehandlingsløsningen",
                frontendFeilmelding = "Du mangler tilgang til denne saksbehandlingsløsningen",
            )
        }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}
