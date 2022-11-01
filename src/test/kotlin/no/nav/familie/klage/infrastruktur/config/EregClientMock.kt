package no.nav.familie.klage.infrastruktur.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.klage.søk.ereg.EregClient
import no.nav.familie.klage.søk.ereg.Navn
import no.nav.familie.klage.søk.ereg.OrganisasjonDetaljer
import no.nav.familie.klage.søk.ereg.OrganisasjonDto
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
class EregClientMock {

    @Profile("mock-ereg")
    @Bean
    @Primary
    fun eregClient(): EregClient {
        val mockk = mockk<EregClient>()
        every { mockk.hentOrganisasjoner(any()) } returns lagResponse()
        return mockk
    }

    private fun lagResponse() = listOf(lagOrganisasjonDto(lagNavn(), lagOrganisasjonDetaljer()))

    private fun lagOrganisasjonDetaljer(): OrganisasjonDetaljer =
        OrganisasjonDetaljer(
            registreringsdato = null,
            enhetstyper = null,
            navn = null,
            forretningsAdresser = null,
            postAdresser = null,
            sistEndret = null
        )

    private fun lagNavn() =
        Navn(
            bruksperiode = null,
            gyldighetsperiode = null,
            navnelinje1 = "Julenissens Gavefabrikk AS",
            navnelinje2 = null,
            navnelinje3 = null,
            navnelinje4 = null,
            navnelinje5 = null,
            redigertnavn = null
        )

    private fun lagOrganisasjonDto(
        navn: Navn,
        organisasjonDetaljer: OrganisasjonDetaljer
    ) =
        OrganisasjonDto(
            organisasjonsnummer = "123456789",
            type = "type",
            navn = navn,
            organisasjonDetaljer = organisasjonDetaljer
        )
}
