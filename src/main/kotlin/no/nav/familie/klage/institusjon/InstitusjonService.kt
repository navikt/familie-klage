package no.nav.familie.klage.institusjon

import no.nav.familie.klage.integrasjoner.FamilieIntegrasjonerClient
import org.springframework.stereotype.Service

@Service
class InstitusjonService(
    private val institusjonRepository: InstitusjonRepository,
    private val familieIntegrasjonerClient: FamilieIntegrasjonerClient,
) {
    fun hentEllerLagreInstitusjon(orgNummer: String): Institusjon {
        val institusjon = institusjonRepository.finnInstitusjon(orgNummer)
        if (institusjon != null) {
            return institusjon
        }
        val organisasjon = familieIntegrasjonerClient.hentOrganisasjon(orgNummer)
        return institusjonRepository.insert(
            Institusjon(
                orgNummer = orgNummer,
                navn = organisasjon.navn,
            ),
        )
    }
}
