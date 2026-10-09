package no.nav.familie.klage.infrastruktur.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.tilgangsmaskin.Avvisningskode
import no.nav.familie.tilgangsmaskin.TilgangsmaskinKlient
import no.nav.familie.tilgangsmaskin.TilgangsmaskinResultat
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-tilgangsmaskin")
class TilgangsmaskinKlientMock {
    @Bean
    @Primary
    fun tilgangsmaskinKlient(): TilgangsmaskinKlient {
        val tilgangsmaskinKlient = mockk<TilgangsmaskinKlient>()
        every { tilgangsmaskinKlient.sjekkTilgangTilPersoner(any(), any()) } answers {
            firstArg<Set<String>>().map { personIdent ->
                if (personIdent.contains("ikkeTilgang")) {
                    TilgangsmaskinResultat(
                        personIdent = personIdent,
                        harTilgang = false,
                        httpStatus = 403,
                        avvisningskode = Avvisningskode.AVVIST_STRENGT_FORTROLIG_ADRESSE,
                        begrunnelse = "Du har ikke tilgang til brukere med strengt fortrolig adresse (kode 6)",
                    )
                } else {
                    TilgangsmaskinResultat(personIdent = personIdent, harTilgang = true, httpStatus = 204)
                }
            }
        }
        return tilgangsmaskinKlient
    }
}
