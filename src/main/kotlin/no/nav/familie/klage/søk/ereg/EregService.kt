package no.nav.familie.klage.søk.ereg

import no.nav.familie.klage.infrastruktur.exception.ApiFeil
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class EregService(private val eregClient: EregClient) {

    @Cacheable("hentOrganisasjon", cacheManager = "shortCache")
    fun hentOrganisasjon(organisasjonsnummer: String): Organisasjon {
        val organisasjon = eregClient.hentOrganisasjoner(listOf(organisasjonsnummer)).firstOrNull()

        return organisasjon?.let { mapOrganisasjonDto(it) } ?: throw ApiFeil(
            "Finner ingen organisasjon for søket",
            HttpStatus.BAD_REQUEST,
        )
    }
}
