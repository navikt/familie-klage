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
import java.util.UUID
import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.klage.infrastruktur.featuretoggle.Toggle

@Component
class FamilieBASakClient(
    @Qualifier("azure") restOperations: RestOperations,
    @Value("\${FAMILIE_BA_SAK_URL}") private val familieBaSakUri: URI,
    private val featureToggleService: FeatureToggleService
) : AbstractRestClient(restOperations, "familie.ba.sak") {

    fun hentVedtak(eksternFagsakId: String): List<FagsystemVedtak> {
        val hentVedtakUri = UriComponentsBuilder.fromUri(familieBaSakUri)
            .pathSegment("api/klage/fagsaker/$eksternFagsakId/vedtak")
            .build()
            .toUri()
        return getForEntity<Ressurs<List<FagsystemVedtak>>>(hentVedtakUri).getDataOrThrow()
    }

    fun kanOppretteRevurdering(eksternFagsakId: String): KanOppretteRevurderingResponse {
        val hentVedtakUri = UriComponentsBuilder.fromUri(familieBaSakUri)
            .pathSegment("api/klage/fagsaker/$eksternFagsakId/kan-opprette-revurdering-klage")
            .build()
            .toUri()
        return getForEntity<Ressurs<KanOppretteRevurderingResponse>>(hentVedtakUri).getDataOrThrow()
    }

    fun opprettRevurdering(eksternFagsakId: String, klagebehandlingId: UUID): OpprettRevurderingResponse {
        val url = if (featureToggleService.isEnabled(Toggle.SEND_BEHANDLING_ID_VED_OPPRETTING_AV_REVURDERING_KLAGE)) {
            "api/ekstern/fagsak/$eksternFagsakId/klagebehandling/$klagebehandlingId/opprett-revurdering-klage"
        } else {
            "api/ekstern/fagsaker/$eksternFagsakId/opprett-revurdering-klage"
        }
        val hentVedtakUri = UriComponentsBuilder.fromUri(familieBaSakUri)
            .pathSegment(url)
            .build()
            .toUri()
        return postForEntity<Ressurs<OpprettRevurderingResponse>>(hentVedtakUri, emptyMap<String, String>()).getDataOrThrow()
    }
}
