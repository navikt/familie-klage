package no.nav.familie.klage.oppgave

import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.http.client.RessursException
import no.nav.familie.klage.felles.util.medContentTypeJsonUTF8
import no.nav.familie.klage.infrastruktur.config.IntegrasjonerConfig
import no.nav.familie.klage.infrastruktur.exception.ApiFeil
import no.nav.familie.klage.infrastruktur.exception.IntegrasjonException
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppgave.FinnMappeResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.kontrakter.felles.saksbehandler.Saksbehandler
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class OppgaveClient(
    @Qualifier("azure") restOperations: RestOperations,
    private val integrasjonerConfig: IntegrasjonerConfig,
) : AbstractPingableRestClient(restOperations, "oppgave") {

    override val pingUri: URI = integrasjonerConfig.pingUri
    private val oppgaveUri: URI = integrasjonerConfig.oppgaveUri
    private val saksbehandlerUri: URI = integrasjonerConfig.saksbehandlerUri

    fun opprettOppgave(opprettOppgaveRequest: OpprettOppgaveRequest): Long {
        val uri = URI.create("$oppgaveUri/opprett")

        val respons =
            postForEntity<Ressurs<OppgaveResponse>>(uri, opprettOppgaveRequest, HttpHeaders().medContentTypeJsonUTF8())
        return pakkUtRespons(respons, uri, "opprettOppgave").oppgaveId
    }

    fun finnOppgaveMedId(oppgaveId: Long): Oppgave {
        val uri = URI.create("$oppgaveUri/$oppgaveId")

        val respons = getForEntity<Ressurs<Oppgave>>(uri)
        return pakkUtRespons(respons, uri, "finnOppgaveMedId")
    }

    fun ferdigstillOppgave(oppgaveId: Long) {
        val uri = URI.create("$oppgaveUri/$oppgaveId/ferdigstill")
        val respons = patchForEntity<Ressurs<OppgaveResponse>>(uri, "")
        pakkUtRespons(respons, uri, "ferdigstillOppgave")
    }

    fun oppdaterOppgave(oppgave: Oppgave): Long {
        val uri = URI.create("$oppgaveUri/${oppgave.id!!}/oppdater")
        val respons = patchForEntity<Ressurs<OppgaveResponse>>(
            uri,
            oppgave,
            HttpHeaders().medContentTypeJsonUTF8(),
        )
        return pakkUtRespons(respons, uri, "oppdaterOppgave").oppgaveId
    }

    fun fordelOppgave(
        oppgaveId: Long,
        saksbehandler: String?,
        versjon: Int? = null,
    ): Long {
        var uri = URI.create("$oppgaveUri/$oppgaveId/fordel")

        if (saksbehandler != null) {
            uri =
                UriComponentsBuilder
                    .fromUri(uri)
                    .queryParam("saksbehandler", saksbehandler)
                    .build()
                    .toUri()
        }

        if (versjon != null) {
            uri =
                UriComponentsBuilder
                    .fromUri(uri)
                    .queryParam("versjon", versjon)
                    .build()
                    .toUri()
        }

        try {
            val respons = postForEntity<Ressurs<OppgaveResponse>>(uri, HttpHeaders().medContentTypeJsonUTF8())
            return pakkUtRespons(respons, uri, "fordelOppgave").oppgaveId
        } catch (e: RessursException) {
            when {
                e.ressurs.melding.contains("allerede er ferdigstilt") ->
                    throw ApiFeil(
                        "Oppgaven med id=$oppgaveId er allerede ferdigstilt. Prøv å hente oppgaver på nytt.",
                        HttpStatus.BAD_REQUEST,
                    )

                e.httpStatus == HttpStatus.CONFLICT -> throw ApiFeil(
                    "Oppgaven har endret seg siden du sist hentet oppgaver. For å kunne gjøre endringer må du hente oppgaver på nytt.",
                    HttpStatus.CONFLICT,
                )

                else -> throw e
            }
        }
    }

    fun finnMapper(
        enhetnummer: String,
        limit: Int,
    ): FinnMappeResponseDto {
        val uri =
            UriComponentsBuilder
                .fromUri(oppgaveUri)
                .pathSegment("mappe", "sok")
                .queryParam("enhetsnr", enhetnummer)
                .queryParam("limit", limit)
                .build()
                .toUri()

        val respons = getForEntity<Ressurs<FinnMappeResponseDto>>(uri = uri)
        return pakkUtRespons(respons = respons, uri = uri, metode = "finnMappe")
    }

    fun hentSaksbehandlerInfo(navIdent: String): Saksbehandler {
        val uri = URI.create("$saksbehandlerUri/$navIdent")

        val respons = getForEntity<Ressurs<Saksbehandler>>(uri)
        return pakkUtRespons(respons, uri, "hentSaksbehandlerInfo")
    }

    private fun <T> pakkUtRespons(
        respons: Ressurs<T>,
        uri: URI?,
        metode: String,
    ): T {
        val data = respons.data
        if (respons.status == Ressurs.Status.SUKSESS && data != null) {
            return data
        } else if (respons.status == Ressurs.Status.SUKSESS) {
            throw IntegrasjonException("Ressurs har status suksess, men mangler data")
        } else {
            throw IntegrasjonException(
                "Respons fra $metode feilet med status=${respons.status} melding=${respons.melding}",
                null,
                uri,
                data,
            )
        }
    }
}
