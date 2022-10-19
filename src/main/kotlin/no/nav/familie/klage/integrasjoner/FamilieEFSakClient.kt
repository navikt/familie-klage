package no.nav.familie.klage.integrasjoner

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class FamilieEFSakClient(
    @Qualifier("azure") restOperations: RestOperations,
    @Value("\${FAMILIE_EF_SAK_URL}") private val familieEfSakUri: URI
) : AbstractRestClient(restOperations, "familie.ef.sak") {

    fun hentVedtak(fagsystemEksternFagsakId: String): List<FagsystemVedtak> {
        val hentVedtakUri = UriComponentsBuilder.fromUri(familieEfSakUri).pathSegment("api/ekstern/vedtak").build().toUri()
        return getForEntity<Ressurs<List<FagsystemVedtak>>>(hentVedtakUri).getDataOrThrow()
    }
}
