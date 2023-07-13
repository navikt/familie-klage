package no.nav.familie.klage.felles.domain

import jakarta.servlet.http.HttpServletRequest
import no.nav.familie.klage.felles.dto.Tilgang
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.log.mdc.MDCConstants
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * [custom1], [custom2], [custom3] brukes for å logge ekstra felter, eks fagsak, behandling,
 * disse logges til cs3,cs5,cs6 då cs1,cs2 og cs4 er til internt bruk
 * Kan brukes med eks CustomKeyValue(key=fagsak, value=fagsakId)
 */
data class Sporingsdata(
    val event: AuditLoggerEvent,
    val personIdent: String,
    val tilgang: Tilgang,
    val custom1: CustomKeyValue? = null,
    val custom2: CustomKeyValue? = null,
    val custom3: CustomKeyValue? = null,
)

enum class AuditLoggerEvent(val type: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    ACCESS("access"),
}

data class CustomKeyValue(val key: String, val value: String)

@Component
class AuditLogger(@Value("\${NAIS_APP_NAME}") private val applicationName: String) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val audit = LoggerFactory.getLogger("auditLogger")

    private val regexFlereSpaces = "\\s+".toRegex()

    fun log(data: Sporingsdata) {
        val request = getRequest() ?: throw IllegalArgumentException("Ikke brukt i context av en HTTP request")

        if (!SikkerhetContext.erMaskinTilMaskinToken()) {
            audit.info(createAuditLogString(data, request))
        } else {
            logger.debug("Maskin til maskin token i request")
        }
    }

    private fun getRequest(): HttpServletRequest? {
        return RequestContextHolder.getRequestAttributes()
            ?.takeIf { it is ServletRequestAttributes }
            ?.let { it as ServletRequestAttributes }
            ?.request
    }

    private fun createAuditLogString(data: Sporingsdata, request: HttpServletRequest): String {
        val timestamp = System.currentTimeMillis()
        val name = "Saksbehandling"
        return "CEF:0|Familie|$applicationName|1.0|audit:${data.event.type}|$name|INFO|end=$timestamp " +
            "suid=${SikkerhetContext.hentSaksbehandler(strict = true)} " +
            "duid=${data.personIdent} " +
            "sproc=${getCallId()} " +
            "requestMethod=${request.method} " +
            "request=${request.requestURI} " +
            "flexString1Label=Decision flexString1=${formatHarTilgang(data.tilgang)} " +
            formatDenyPolicy(data.tilgang) +
            createCustomString(data)
    }

    private fun formatHarTilgang(tilgang: Tilgang): String = if (tilgang.harTilgang) "Permit" else "Deny"

    private fun formatDenyPolicy(tilgang: Tilgang): String {
        val begrunnelse = tilgang.begrunnelse
        return if (!tilgang.harTilgang && begrunnelse != null) {
            val denyPolicy = begrunnelse.replace(regexFlereSpaces, " ").split(" ").joinToString("_")
            "flexString2Label=deny_policy flexString2=$denyPolicy "
        } else {
            ""
        }
    }

    private fun createCustomString(data: Sporingsdata): String {
        return listOfNotNull(
            data.custom1?.let { "cs3Label=${it.key} cs3=${it.value}" },
            data.custom2?.let { "cs5Label=${it.key} cs5=${it.value}" },
            data.custom3?.let { "cs6Label=${it.key} cs6=${it.value}" },
        )
            .joinToString(" ")
    }

    private fun getCallId(): String {
        return MDC.get(MDCConstants.MDC_CALL_ID) ?: throw IllegalStateException("Mangler callId")
    }
}
