package no.nav.familie.klage.infrastruktur.config

import no.nav.familie.felles.tokenklient.entraid.EntraIDRestClientFactory
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.log.interceptor.ConsumerIdClientInterceptor
import no.nav.familie.log.interceptor.MdcValuesPropagatingClientInterceptor
import no.nav.familie.tilgangsmaskin.TilgangsmaskinKlientConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

@Configuration
class RestClientConfig(
    private val entraIDRestClientFactory: EntraIDRestClientFactory,
    private val consumerIdClientInterceptor: ConsumerIdClientInterceptor,
    private val mdcValuesPropagatingClientInterceptor: MdcValuesPropagatingClientInterceptor,
) {
    /** familie-integrasjoner (dual-mode: OBO for saksbehandler, CC for systembruker) */
    @Bean("integrasjonerRestClient")
    fun integrasjonerRestClient(
        @Value("\${FAMILIE_INTEGRASJONER_SCOPE}") scope: String,
    ): RestClient = entraIDRestClientFactory.lagHybridRestKlient(scope) { SikkerhetContext.hentJwt()?.tokenValue }

    /** repr-api / fullmakt (dual-mode: OBO for saksbehandler, CC for systembruker) */
    @Bean("reprApiRestClient")
    fun reprApiRestClient(
        @Value("\${REPR_API_SCOPE}") scope: String,
    ): RestClient = entraIDRestClientFactory.lagHybridRestKlient(scope) { SikkerhetContext.hentJwt()?.tokenValue }

    /** kabal-api (dual-mode: OBO for saksbehandler, CC for systembruker) */
    @Bean("kabalRestClient")
    fun kabalRestClient(
        @Value("\${KABAL_SCOPE}") scope: String,
    ): RestClient = entraIDRestClientFactory.lagHybridRestKlient(scope) { SikkerhetContext.hentJwt()?.tokenValue }

    /** familie-ef-sak (dual-mode: OBO for saksbehandler, CC for systembruker) */
    @Bean("efSakRestClient")
    fun efSakRestClient(
        @Value("\${FAMILIE_EF_SAK_SCOPE}") scope: String,
    ): RestClient = entraIDRestClientFactory.lagOboRestKlient(scope) { SikkerhetContext.hentJwt()?.tokenValue ?: error("OBO-kall uten innlogget bruker") }

    /** familie-ba-sak (OBO-only: alltid kalt i saksbehandler-kontekst) */
    @Bean("baSakRestClient")
    fun baSakRestClient(
        @Value("\${FAMILIE_BA_SAK_SCOPE}") scope: String,
    ): RestClient = entraIDRestClientFactory.lagOboRestKlient(scope) { SikkerhetContext.hentJwt()?.tokenValue ?: error("OBO-kall uten innlogget bruker") }

    /** familie-ks-sak (OBO-only: alltid kalt i saksbehandler-kontekst) */
    @Bean("ksSakRestClient")
    fun ksSakRestClient(
        @Value("\${FAMILIE_KS_SAK_SCOPE}") scope: String,
    ): RestClient = entraIDRestClientFactory.lagOboRestKlient(scope) { SikkerhetContext.hentJwt()?.tokenValue ?: error("OBO-kall uten innlogget bruker") }

    /** Tilgangsmaskinen (OBO-only: bulk-endepunktet avviser maskin-til-maskin-token) */
    @Bean(TilgangsmaskinKlientConfig.TILGANGSMASKIN_OBO_REST_CLIENT)
    fun tilgangsmaskinOboRestClient(
        @Value("\${TILGANGSMASKIN_SCOPE}") scope: String,
    ): RestClient {
        val requestFactory =
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(Duration.ofSeconds(2))
                setReadTimeout(Duration.ofSeconds(5))
            }
        return entraIDRestClientFactory
            .lagOboRestKlient(scope) {
                SikkerhetContext.hentJwt()?.tokenValue
                    ?: throw IllegalStateException("Kall mot Tilgangsmaskinen krever OBO-token, men fant ingen JWT i konteksten")
            }.mutate()
            .requestFactory(requestFactory)
            .build()
    }

    /** PDL (CC-only: systemkall) */
    @Bean("pdlRestClient")
    fun pdlRestClient(
        @Value("\${PDL_SCOPE}") scope: String,
    ): RestClient = entraIDRestClientFactory.lagMaskinTilMaskinRestKlient(scope)

    /** familie-ef-proxy (CC-only: systemkall) */
    @Bean("efProxyRestClient")
    fun efProxyRestClient(
        @Value("\${FAMILIE_EF_PROXY_SCOPE}") scope: String,
    ): RestClient = entraIDRestClientFactory.lagMaskinTilMaskinRestKlient(scope)

    /** Uten autentisering – for åpne endepunkter (familie-brev, familie-dokument) */
    @Bean("utenAuthRestClient")
    fun utenAuthRestClient(): RestClient =
        RestClient
            .builder()
            .requestInterceptor(consumerIdClientInterceptor)
            .requestInterceptor(mdcValuesPropagatingClientInterceptor)
            .build()
}
