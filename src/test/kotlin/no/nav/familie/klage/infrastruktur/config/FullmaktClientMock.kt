package no.nav.familie.klage.infrastruktur.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.klage.personopplysninger.fullmakt.FullmaktClient
import no.nav.familie.klage.personopplysninger.fullmakt.FullmaktResponse
import no.nav.familie.klage.personopplysninger.fullmakt.Handling
import no.nav.familie.klage.personopplysninger.fullmakt.Område
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.LocalDate

@Configuration
@Profile("mock-fullmakt")
class FullmaktClientMock {
    @Bean
    @Primary
    fun fullmaktClient(): FullmaktClient {
        val fullmaktClient: FullmaktClient = mockk()
        every { fullmaktClient.hentFullmakt(any()) } returns
            listOf(
                FullmaktResponse(
                    LocalDate.now().minusYears(1),
                    LocalDate.now().plusYears(1),
                    "01010199999",
                    "Navn",
                    listOf(Område("ENF", listOf(Handling.LES))),
                ),
            )

        return fullmaktClient
    }
}
