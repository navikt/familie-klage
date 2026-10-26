package no.nav.familie.klage.personopplysninger.fullmakt

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.klage.personopplysninger.pdl.PdlClient
import no.nav.familie.klage.testutil.PdlTestdataHelper.lagNavn
import no.nav.familie.klage.testutil.PdlTestdataHelper.pdlNavn
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FullmaktServiceTest {
    private val fullmaktClient = mockk<FullmaktClient>()
    private val pdlClient = mockk<PdlClient>()
    private val fullmaktService = FullmaktService(fullmaktClient, pdlClient)

    @Test
    fun `skal sette navn fra PDL på riktig fullmektig`() {
        // Arrange
        every { fullmaktClient.hentFullmakt(FULLMAKTSGIVER) } returns
            listOf(fullmaktResponse(fullmektig = FULLMEKTIG_1), fullmaktResponse(fullmektig = FULLMEKTIG_2))
        every { pdlClient.hentNavnBolk(listOf(FULLMEKTIG_1, FULLMEKTIG_2), STØNADSTYPE) } returns
            mapOf(
                FULLMEKTIG_2 to pdlNavn(listOf(lagNavn(fornavn = "Andre", mellomnavn = null, etternavn = "Fullmektig"))),
                FULLMEKTIG_1 to pdlNavn(listOf(lagNavn(fornavn = "Første", mellomnavn = null, etternavn = "Fullmektig"))),
            )

        // Act
        val resultat = fullmaktService.hentFullmakt(FULLMAKTSGIVER, STØNADSTYPE)

        // Assert
        assertThat(resultat.harTilgang).isTrue
        assertThat(resultat.fullmakt.associate { it.motpartsPersonident to it.fullmektigsNavn })
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    FULLMEKTIG_1 to "Første Fullmektig",
                    FULLMEKTIG_2 to "Andre Fullmektig",
                ),
            )
    }

    @Test
    fun `skal slå sammen lese- og skriverettigheter uten duplikater`() {
        // Arrange
        every { fullmaktClient.hentFullmakt(FULLMAKTSGIVER) } returns
            listOf(
                fullmaktResponse(
                    fullmektig = FULLMEKTIG_1,
                    leserettigheter = listOf("ENF", "BAR"),
                    skriverettigheter = listOf("ENF", "KON"),
                ),
            )
        every { pdlClient.hentNavnBolk(any(), any()) } returns emptyMap()

        // Act
        val resultat = fullmaktService.hentFullmakt(FULLMAKTSGIVER, STØNADSTYPE)

        // Assert
        assertThat(resultat.fullmakt.single().omraader).containsExactly("ENF", "BAR", "KON")
    }

    @Test
    fun `skal ikke kalle PDL når det ikke finnes fullmakter`() {
        // Arrange
        every { fullmaktClient.hentFullmakt(FULLMAKTSGIVER) } returns emptyList()

        // Act
        val resultat = fullmaktService.hentFullmakt(FULLMAKTSGIVER, STØNADSTYPE)

        // Assert
        assertThat(resultat.fullmakt).isEmpty()
        assertThat(resultat.harTilgang).isTrue
        verify(exactly = 0) { pdlClient.hentNavnBolk(any(), any()) }
    }

    @Test
    fun `skal returnere fullmakter uten navn når PDL feiler`() {
        // Arrange
        every { fullmaktClient.hentFullmakt(FULLMAKTSGIVER) } returns
            listOf(fullmaktResponse(fullmektig = FULLMEKTIG_1), fullmaktResponse(fullmektig = FULLMEKTIG_2))
        every { pdlClient.hentNavnBolk(any(), any()) } throws RuntimeException("PDL er nede")

        // Act
        val resultat = fullmaktService.hentFullmakt(FULLMAKTSGIVER, STØNADSTYPE)

        // Assert
        assertThat(resultat.harTilgang).isTrue
        assertThat(resultat.fullmakt.map { it.motpartsPersonident }).containsExactly(FULLMEKTIG_1, FULLMEKTIG_2)
        assertThat(resultat.fullmakt).allSatisfy { assertThat(it.fullmektigsNavn).isNull() }
    }

    private fun fullmaktResponse(
        fullmektig: String,
        leserettigheter: List<String> = listOf("ENF"),
        skriverettigheter: List<String> = emptyList(),
    ) = FullmaktResponse(
        gyldigFraOgMed = LocalDate.of(2026, 1, 1),
        gyldigTilOgMed = null,
        fullmektig = fullmektig,
        leserettigheter = leserettigheter,
        skriverettigheter = skriverettigheter,
    )

    companion object {
        private const val FULLMAKTSGIVER = "01010112345"
        private const val FULLMEKTIG_1 = "02020212345"
        private const val FULLMEKTIG_2 = "03030312345"
        private val STØNADSTYPE = Stønadstype.OVERGANGSSTØNAD
    }
}
