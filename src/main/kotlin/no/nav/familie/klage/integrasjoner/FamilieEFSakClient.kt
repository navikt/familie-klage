package no.nav.familie.klage.integrasjoner

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import no.nav.familie.kontrakter.felles.klage.KanOppretteRevurderingResponse
import no.nav.familie.kontrakter.felles.klage.OpprettRevurderingResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class FamilieEFSakClient(
    @Qualifier("efSakRestClient") private val restClient: RestClient,
    @Value("\${FAMILIE_EF_SAK_URL}") private val familieEfSakUri: URI,
) {
    fun hentVedtak(fagsystemEksternFagsakId: String): List<FagsystemVedtak> =
        restClient
            .get()
            .uri(
                UriComponentsBuilder
                    .fromUri(familieEfSakUri)
                    .pathSegment("api/ekstern/vedtak/$fagsystemEksternFagsakId")
                    .build()
                    .toUri(),
            ).retrieve()
            .body<Ressurs<List<FagsystemVedtak>>>()!!
            .getDataOrThrow()

    fun kanOppretteRevurdering(fagsystemEksternFagsakId: String): KanOppretteRevurderingResponse =
        restClient
            .get()
            .uri(
                UriComponentsBuilder
                    .fromUri(familieEfSakUri)
                    .pathSegment("api/ekstern/behandling/kan-opprette-revurdering-klage/$fagsystemEksternFagsakId")
                    .build()
                    .toUri(),
            ).retrieve()
            .body<Ressurs<KanOppretteRevurderingResponse>>()!!
            .getDataOrThrow()

    fun opprettRevurdering(fagsystemEksternFagsakId: String): OpprettRevurderingResponse =
        restClient
            .post()
            .uri(
                UriComponentsBuilder
                    .fromUri(familieEfSakUri)
                    .pathSegment("api/ekstern/behandling/opprett-revurdering-klage/$fagsystemEksternFagsakId")
                    .build()
                    .toUri(),
            ).contentType(MediaType.APPLICATION_JSON)
            .body(emptyMap<String, String>())
            .retrieve()
            .body<Ressurs<OpprettRevurderingResponse>>()!!
            .getDataOrThrow()
}
