package no.nav.familie.klage.integrasjoner

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.klage.felles.dto.Tilgang
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
import java.util.UUID

@Component
class FamilieKSSakClient(
    @Qualifier("azure") restOperations: RestOperations,
    @Value("\${FAMILIE_KS_SAK_URL}") private val familieKsSakUri: URI,
) : AbstractRestClient(restOperations, "familie.ks.sak") {
    fun hentVedtak(eksternFagsakId: String): List<FagsystemVedtak> {
        val hentVedtakUri =
            UriComponentsBuilder
                .fromUri(familieKsSakUri)
                .pathSegment("api/ekstern/fagsaker/$eksternFagsakId/vedtak")
                .build()
                .toUri()
        return getForEntity<Ressurs<List<FagsystemVedtak>>>(hentVedtakUri).getDataOrThrow()
    }

    fun kanOppretteRevurdering(eksternFagsakId: String): KanOppretteRevurderingResponse {
        val hentVedtakUri =
            UriComponentsBuilder
                .fromUri(familieKsSakUri)
                .pathSegment("api/ekstern/fagsaker/$eksternFagsakId/kan-opprette-revurdering-klage")
                .build()
                .toUri()
        return getForEntity<Ressurs<KanOppretteRevurderingResponse>>(hentVedtakUri).getDataOrThrow()
    }

    fun opprettRevurdering(
        eksternFagsakId: String,
        eksternBehandlingId: UUID,
    ): OpprettRevurderingResponse {
        val hentVedtakUri =
            UriComponentsBuilder
                .fromUri(familieKsSakUri)
                .pathSegment("api/ekstern/fagsak/$eksternFagsakId/klagebehandling/$eksternBehandlingId/opprett-revurdering-klage")
                .build()
                .toUri()
        return postForEntity<Ressurs<OpprettRevurderingResponse>>(hentVedtakUri, emptyMap<String, String>()).getDataOrThrow()
    }

    fun hentTilgangTilFagsak(eksternFagsakId: String): Tilgang {
        val tilgangUri =
            UriComponentsBuilder
                .fromUri(familieKsSakUri)
                .pathSegment("api/klage/fagsak/$eksternFagsakId/tilgang")
                .build()
                .toUri()
        return getForEntity<Ressurs<Tilgang>>(tilgangUri).getDataOrThrow()
    }
}
