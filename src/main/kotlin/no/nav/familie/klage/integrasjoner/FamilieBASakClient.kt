package no.nav.familie.klage.integrasjoner

import no.nav.familie.klage.felles.dto.Tilgang
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import no.nav.familie.kontrakter.felles.klage.KanOppretteRevurderingResponse
import no.nav.familie.kontrakter.felles.klage.OpprettRevurderingResponse
import no.nav.familie.kontrakter.felles.tilgangskontroll.FagsakTilgang
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.util.UUID

@Component
class FamilieBASakClient(
    @Value("\${FAMILIE_BA_SAK_URL}") private val familieBaSakUri: URI,
    @Qualifier("baSakRestClient") private val restClient: RestClient,
) {
    fun hentVedtak(eksternFagsakId: String): List<FagsystemVedtak> =
        restClient
            .get()
            .uri(
                UriComponentsBuilder
                    .fromUri(familieBaSakUri)
                    .pathSegment("api/klage/fagsaker/$eksternFagsakId/vedtak")
                    .build()
                    .toUri(),
            ).retrieve()
            .body<Ressurs<List<FagsystemVedtak>>>()!!
            .getDataOrThrow()

    fun kanOppretteRevurdering(eksternFagsakId: String): KanOppretteRevurderingResponse =
        restClient
            .get()
            .uri(
                UriComponentsBuilder
                    .fromUri(familieBaSakUri)
                    .pathSegment("api/klage/fagsaker/$eksternFagsakId/kan-opprette-revurdering-klage")
                    .build()
                    .toUri(),
            ).retrieve()
            .body<Ressurs<KanOppretteRevurderingResponse>>()!!
            .getDataOrThrow()

    fun opprettRevurdering(
        eksternFagsakId: String,
        eksternBehandlingId: UUID,
    ): OpprettRevurderingResponse =
        restClient
            .post()
            .uri(
                UriComponentsBuilder
                    .fromUri(familieBaSakUri)
                    .pathSegment("api/klage/fagsak/$eksternFagsakId/klagebehandling/$eksternBehandlingId/opprett-revurdering-klage")
                    .build()
                    .toUri(),
            ).contentType(MediaType.APPLICATION_JSON)
            .body(emptyMap<String, String>())
            .retrieve()
            .body<Ressurs<OpprettRevurderingResponse>>()!!
            .getDataOrThrow()

    fun hentTilgangTilFagsak(eksternFagsakId: String): Tilgang {
        val tilgangUri =
            UriComponentsBuilder
                .fromUri(familieBaSakUri)
                .pathSegment("api/klage/fagsak/$eksternFagsakId/tilgang")
                .build()
                .toUri()
        val fagsakTilgang =
            restClient
                .get()
                .uri(tilgangUri)
                .retrieve()
                .body<Ressurs<FagsakTilgang>>()!!
                .getDataOrThrow()
        return Tilgang(harTilgang = fagsakTilgang.harTilgang, begrunnelse = fagsakTilgang.begrunnelse)
    }
}
