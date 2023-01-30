package no.nav.familie.klage.integrasjoner

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import no.nav.familie.kontrakter.felles.klage.KanOppretteRevurderingResponse
import no.nav.familie.kontrakter.felles.klage.OpprettRevurderingResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class FamilieKSSakClient(
    @Qualifier("azure") restOperations: RestOperations,
    @Value("\${FAMILIE_KS_SAK_URL}") private val familieKsSakUri: URI,
) : AbstractRestClient(restOperations, "familie.ks.sak") {

    fun hentVedtak(fagsystemEksternFagsakId: String): List<FagsystemVedtak> {
        val hentVedtakUri = UriComponentsBuilder.fromUri(familieKsSakUri)
            .pathSegment("api/ekstern/fagsaker/$fagsystemEksternFagsakId/vedtak")
            .build().toUri()
        return getForEntity<Ressurs<List<FagsystemVedtak>>>(hentVedtakUri).getDataOrThrow()
    }

    fun kanOppretteRevurdering(fagsystemEksternFagsakId: String): KanOppretteRevurderingResponse {
        val hentVedtakUri = UriComponentsBuilder.fromUri(familieKsSakUri)
            .pathSegment("api/ekstern/fagsaker/$fagsystemEksternFagsakId/kan-opprette-revurdering-klage")
            .build().toUri()
        return getForEntity<Ressurs<KanOppretteRevurderingResponse>>(hentVedtakUri).getDataOrThrow()
    }

    fun opprettRevurdering(fagsystemEksternFagsakId: String): OpprettRevurderingResponse {
        val hentVedtakUri = UriComponentsBuilder.fromUri(familieKsSakUri)
            .pathSegment("api/ekstern/fagsaker/$fagsystemEksternFagsakId/opprett-revurdering-klage/")
            .build().toUri()
        return postForEntity<Ressurs<OpprettRevurderingResponse>>(hentVedtakUri, emptyMap<String, String>()).getDataOrThrow()
    }
}
