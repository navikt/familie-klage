package no.nav.familie.klage.infrastruktur.sikkerhet

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.klage.felles.dto.Tilgang
import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.klage.infrastruktur.featuretoggle.Toggle
import no.nav.familie.klage.testutil.BrukerContextUtil.clearBrukerContext
import no.nav.familie.klage.testutil.BrukerContextUtil.mockBrukerContext
import no.nav.familie.tilgangsmaskin.Avvisningskode
import no.nav.familie.tilgangsmaskin.Regeltype
import no.nav.familie.tilgangsmaskin.TilgangsmaskinException
import no.nav.familie.tilgangsmaskin.TilgangsmaskinKlient
import no.nav.familie.tilgangsmaskin.TilgangsmaskinResultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.slf4j.LoggerFactory

class TilgangsmaskinSkyggeServiceTest {
    private val tilgangsmaskinKlient = mockk<TilgangsmaskinKlient>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val tilgangsmaskinSkyggeService = TilgangsmaskinSkyggeService(tilgangsmaskinKlient, featureToggleService)

    private val åpenLogg = LoggerFactory.getLogger(TilgangsmaskinSkyggeService::class.java) as Logger
    private val listAppender = ListAppender<ILoggingEvent>()
    private var opprinneligLoggnivå: Level? = null

    private val meterRegistry = SimpleMeterRegistry()

    @BeforeEach
    fun setUp() {
        mockBrukerContext()
        every { featureToggleService.isEnabled(Toggle.SKAL_SKYGGEKJØRE_TILGANGSMASKINEN) } returns true
        opprinneligLoggnivå = åpenLogg.level
        åpenLogg.level = Level.INFO
        listAppender.start()
        åpenLogg.addAppender(listAppender)
        Metrics.addRegistry(meterRegistry)
    }

    @AfterEach
    fun tearDown() {
        Metrics.removeRegistry(meterRegistry)
        meterRegistry.close()
        åpenLogg.detachAppender(listAppender)
        åpenLogg.level = opprinneligLoggnivå
        listAppender.stop()
        clearBrukerContext()
    }

    @Test
    fun `skal ikke kalle Tilgangsmaskinen når toggelen er av`() {
        // Arrange
        every { featureToggleService.isEnabled(Toggle.SKAL_SKYGGEKJØRE_TILGANGSMASKINEN) } returns false

        // Act
        tilgangsmaskinSkyggeService.skyggeSjekkTilgangTilPersonMedRelasjoner(PERSONIDENT, Tilgang(harTilgang = true))

        // Assert
        verify(exactly = 0) { tilgangsmaskinKlient.sjekkTilgangTilPersoner(any(), any()) }
    }

    @Test
    fun `skal ikke kalle Tilgangsmaskinen i systemkontekst`() {
        // Arrange
        clearBrukerContext()

        // Act
        tilgangsmaskinSkyggeService.skyggeSjekkTilgangTilPersonMedRelasjoner(PERSONIDENT, Tilgang(harTilgang = true))

        // Assert
        verify(exactly = 0) { tilgangsmaskinKlient.sjekkTilgangTilPersoner(any(), any()) }
    }

    @Test
    fun `skal spørre Tilgangsmaskinen med kjernereglene for søkerens egen ident`() {
        // Arrange
        every { tilgangsmaskinKlient.sjekkTilgangTilPersoner(any(), any()) } returns listOf(resultat(harTilgang = true))

        // Act
        tilgangsmaskinSkyggeService.skyggeSjekkTilgangTilPersonMedRelasjoner(PERSONIDENT, Tilgang(harTilgang = true))

        // Assert
        verify(exactly = 1) { tilgangsmaskinKlient.sjekkTilgangTilPersoner(setOf(PERSONIDENT), Regeltype.KJERNE_REGELTYPE) }
    }

    @Test
    fun `skal ikke logge avvik når begge gir tilgang`() {
        // Arrange
        every { tilgangsmaskinKlient.sjekkTilgangTilPersoner(any(), any()) } returns listOf(resultat(harTilgang = true))

        // Act
        tilgangsmaskinSkyggeService.skyggeSjekkTilgangTilPersonMedRelasjoner(PERSONIDENT, Tilgang(harTilgang = true))

        // Assert
        assertThat(tellerVerdi("tilgangsmaskin.skygge.sammenlignet")).isEqualTo(1.0)
        assertThat(tellerVerdi("tilgangsmaskin.skygge.avvik")).isEqualTo(0.0)
        assertThat(loggmeldinger(Level.WARN)).isEmpty()
    }

    @Test
    fun `skal ikke logge avvik når begge nekter tilgang`() {
        // Arrange
        every { tilgangsmaskinKlient.sjekkTilgangTilPersoner(any(), any()) } returns
            listOf(resultat(harTilgang = false, avvisningskode = Avvisningskode.AVVIST_SKJERMING, httpStatus = 403))

        // Act
        tilgangsmaskinSkyggeService.skyggeSjekkTilgangTilPersonMedRelasjoner(PERSONIDENT, Tilgang(harTilgang = false))

        // Assert
        assertThat(tellerVerdi("tilgangsmaskin.skygge.sammenlignet")).isEqualTo(1.0)
        assertThat(tellerVerdi("tilgangsmaskin.skygge.avvik")).isEqualTo(0.0)
        assertThat(loggmeldinger(Level.WARN)).isEmpty()
    }

    @Test
    fun `skal logge avvik når integrasjoner ga tilgang men Tilgangsmaskinen avviser søkeren`() {
        // Arrange
        every { tilgangsmaskinKlient.sjekkTilgangTilPersoner(any(), any()) } returns
            listOf(resultat(harTilgang = false, avvisningskode = Avvisningskode.AVVIST_HABILITET, httpStatus = 403))

        // Act
        tilgangsmaskinSkyggeService.skyggeSjekkTilgangTilPersonMedRelasjoner(PERSONIDENT, Tilgang(harTilgang = true))

        // Assert
        assertThat(tellerVerdi("tilgangsmaskin.skygge.avvik")).isEqualTo(1.0)
        assertThat(loggmeldinger(Level.WARN)).anyMatch { it.contains("ny-strengere") }
    }

    @Test
    fun `skal ikke telle som avvik når integrasjoner avviste og Tilgangsmaskinen ga tilgang til søkeren`() {
        // Arrange
        // Avslaget fra integrasjoner kan skyldes en relasjon vi ikke spør Tilgangsmaskinen om.
        every { tilgangsmaskinKlient.sjekkTilgangTilPersoner(any(), any()) } returns listOf(resultat(harTilgang = true))

        // Act
        tilgangsmaskinSkyggeService.skyggeSjekkTilgangTilPersonMedRelasjoner(PERSONIDENT, Tilgang(harTilgang = false))

        // Assert
        assertThat(tellerVerdi("tilgangsmaskin.skygge.avvik")).isEqualTo(0.0)
        assertThat(tellerVerdi("tilgangsmaskin.skygge.uavklart")).isEqualTo(1.0)
        assertThat(loggmeldinger(Level.WARN)).isEmpty()
    }

    @Test
    fun `skal ikke sammenligne når Tilgangsmaskinen ikke svarte for identen`() {
        // Arrange
        every { tilgangsmaskinKlient.sjekkTilgangTilPersoner(any(), any()) } returns
            listOf(resultat(harTilgang = false, avvisningskode = Avvisningskode.UKJENT, httpStatus = 500))

        // Act
        tilgangsmaskinSkyggeService.skyggeSjekkTilgangTilPersonMedRelasjoner(PERSONIDENT, Tilgang(harTilgang = true))

        // Assert
        assertThat(tellerVerdi("tilgangsmaskin.skygge.manglende.svar")).isEqualTo(1.0)
        assertThat(tellerVerdi("tilgangsmaskin.skygge.sammenlignet")).isEqualTo(0.0)
        assertThat(tellerVerdi("tilgangsmaskin.skygge.avvik")).isEqualTo(0.0)
    }

    @Test
    fun `skal ikke sammenligne når Tilgangsmaskinen svarte for en annen ident`() {
        // Arrange
        every { tilgangsmaskinKlient.sjekkTilgangTilPersoner(any(), any()) } returns
            listOf(resultat(personIdent = "99999999999", harTilgang = false, httpStatus = 403))

        // Act
        tilgangsmaskinSkyggeService.skyggeSjekkTilgangTilPersonMedRelasjoner(PERSONIDENT, Tilgang(harTilgang = true))

        // Assert
        assertThat(tellerVerdi("tilgangsmaskin.skygge.manglende.svar")).isEqualTo(1.0)
        assertThat(tellerVerdi("tilgangsmaskin.skygge.avvik")).isEqualTo(0.0)
    }

    @Test
    fun `skal svelge feil fra Tilgangsmaskinen slik at tilgangskontrollen ikke påvirkes`() {
        // Arrange
        every { tilgangsmaskinKlient.sjekkTilgangTilPersoner(any(), any()) } throws
            TilgangsmaskinException("Kallet feilet", httpStatus = 503)

        // Act & assert
        assertDoesNotThrow {
            tilgangsmaskinSkyggeService.skyggeSjekkTilgangTilPersonMedRelasjoner(PERSONIDENT, Tilgang(harTilgang = true))
        }
        assertThat(tellerVerdi("tilgangsmaskin.skygge.feilet")).isEqualTo(1.0)
    }

    private fun resultat(
        personIdent: String = PERSONIDENT,
        harTilgang: Boolean,
        avvisningskode: Avvisningskode? = null,
        httpStatus: Int = 204,
    ) = TilgangsmaskinResultat(
        personIdent = personIdent,
        harTilgang = harTilgang,
        httpStatus = httpStatus,
        avvisningskode = avvisningskode,
        traceId = "trace-id",
    )

    private fun tellerVerdi(navn: String): Double = meterRegistry.find(navn).counters().sumOf { it.count() }

    private fun loggmeldinger(nivå: Level): List<String> = listAppender.list.filter { it.level == nivå }.map { it.formattedMessage }

    companion object {
        private const val PERSONIDENT = "12345678910"
    }
}
