package no.nav.familie.klage.personopplysninger

import no.nav.familie.klage.felles.dto.EgenAnsattRequest
import no.nav.familie.klage.felles.dto.EgenAnsattResponse
import no.nav.familie.klage.infrastruktur.config.IntegrasjonerConfig
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Component
class PersonopplysningerIntegrasjonerClient(
    @Qualifier("integrasjonerRestClient") private val restClient: RestClient,
    private val integrasjonerConfig: IntegrasjonerConfig,
) {
    fun egenAnsatt(ident: String): Boolean =
        restClient
            .post()
            .uri(integrasjonerConfig.egenAnsattUri)
            .contentType(MediaType.APPLICATION_JSON)
            .body(EgenAnsattRequest(ident))
            .retrieve()
            .body<Ressurs<EgenAnsattResponse>>()!!
            .data!!
            .erEgenAnsatt
}
