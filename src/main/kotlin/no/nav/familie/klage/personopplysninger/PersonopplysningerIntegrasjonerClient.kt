package no.nav.familie.klage.personopplysninger

import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.klage.felles.dto.EgenAnsattRequest
import no.nav.familie.klage.felles.dto.EgenAnsattResponse
import no.nav.familie.klage.felles.dto.Tilgang
import no.nav.familie.klage.infrastruktur.config.IntegrasjonerConfig
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI

@Component
class PersonopplysningerIntegrasjonerClient(
    @Qualifier("azure") restOperations: RestOperations,
    private val integrasjonerConfig: IntegrasjonerConfig,
) : AbstractPingableRestClient(restOperations, "familie.integrasjoner") {
    override val pingUri: URI = integrasjonerConfig.pingUri

    fun sjekkTilgangTilPersonMedRelasjoner(personIdent: String): Tilgang =
        postForEntity(
            integrasjonerConfig.tilgangRelasjonerUri,
            PersonIdent(personIdent),
            HttpHeaders().also {
                it.set(HEADER_NAV_TEMA, HEADER_NAV_TEMA_ENF)
            },
        )

    fun egenAnsatt(ident: String): Boolean =
        postForEntity<Ressurs<EgenAnsattResponse>>(
            integrasjonerConfig.egenAnsattUri,
            EgenAnsattRequest(ident),
        ).data!!.erEgenAnsatt

    companion object {
        const val HEADER_NAV_TEMA = "Nav-Tema"
        const val HEADER_NAV_TEMA_ENF = "ENF"
    }
}
