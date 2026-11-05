package no.nav.familie.klage.integrasjoner

import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.klage.infrastruktur.featuretoggle.Toggle
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.URI

@Component
class IntegrasjonerClientProvider(
    @Qualifier("integrasjonerRestClient") private val restClientFss: RestClient,
    @Value("\${FAMILIE_INTEGRASJONER_URL}")
    private val integrasjonFssUri: URI,
    @Qualifier("integrasjonerGcpRestClient") private val restClientGcp: RestClient,
    @Value("\${FAMILIE_INTEGRASJONER_GCP_URL}")
    private val integrasjonGcpUri: URI,
    private val featureToggleService: FeatureToggleService,
) {
    fun hentKlient(): Pair<URI, RestClient> =
        if (featureToggleService.isEnabled(Toggle.BRUK_FAMILIE_INTEGRASJONER_I_GCP)) {
            integrasjonGcpUri to restClientGcp
        } else {
            integrasjonFssUri to restClientFss
        }
}
