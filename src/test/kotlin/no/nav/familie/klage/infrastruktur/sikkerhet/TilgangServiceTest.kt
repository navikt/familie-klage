package no.nav.familie.klage.infrastruktur.sikkerhet

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.felles.domain.AuditLogger
import no.nav.familie.klage.felles.domain.AuditLoggerEvent
import no.nav.familie.klage.felles.domain.BehandlerRolle
import no.nav.familie.klage.felles.dto.Tilgang
import no.nav.familie.klage.infrastruktur.config.RolleConfigTestUtil
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.infrastruktur.exception.ManglerTilgang
import no.nav.familie.klage.integrasjoner.FamilieBASakClient
import no.nav.familie.klage.integrasjoner.FamilieKSSakClient
import no.nav.familie.klage.personopplysninger.PersonMedRelasjonerService
import no.nav.familie.klage.testutil.BrukerContextUtil.testWithBrukerContext
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import no.nav.familie.tilgangsmaskin.Avvisningskode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.cache.concurrent.ConcurrentMapCacheManager

internal class TilgangServiceTest {
    private val personMedRelasjonerService = mockk<PersonMedRelasjonerService>()
    private val tilgangsmaskinTilgangskontrollKlient = mockk<TilgangsmaskinTilgangskontrollKlient>()
    private val rolleConfig = RolleConfigTestUtil.rolleConfig
    private val cacheManager = ConcurrentMapCacheManager()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>()
    private val fagsakService = mockk<FagsakService>()
    private val familieBASakClient = mockk<FamilieBASakClient>()
    private val familieKSSakClient = mockk<FamilieKSSakClient>()

    private val tilgangService =
        TilgangService(
            personMedRelasjonerService,
            tilgangsmaskinTilgangskontrollKlient,
            rolleConfig,
            cacheManager,
            auditLogger,
            behandlingService,
            fagsakService,
            familieBASakClient,
            familieKSSakClient,
        )

    private val fagsakEf = fagsak()
    private val behandlingEf = behandling(fagsakEf)
    private val fagsakBa = fagsak(stønadstype = Stønadstype.BARNETRYGD)
    private val behandlingBa = behandling(fagsakBa)

    @BeforeEach
    internal fun setUp() {
        mockFagsakOgBehandling(fagsakEf, behandlingEf)
        mockFagsakOgBehandling(fagsakBa, behandlingBa)
    }

    private fun mockTilgangTilPersonMedRelasjoner(
        tema: Tema,
        vararg tilganger: PersonTilgang,
    ) {
        val søker = tilganger.first().personIdent
        val identer = tilganger.map { it.personIdent }.toSet()
        every { personMedRelasjonerService.hentIdenterForPersonMedRelasjoner(søker, tema) } returns identer
        every { tilgangsmaskinTilgangskontrollKlient.sjekkTilgangTilPersoner(identer) } returns tilganger.toList()
    }

    private fun mockFagsakOgBehandling(
        fagsak: Fagsak,
        behandling: Behandling,
    ) {
        every { fagsakService.hentFagsak(fagsak.id) } returns fagsak
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
    }

    @Nested
    inner class TilgangGittRolle {
        @Test
        internal fun `saksbehandler har tilgang til behandling av fagsystem barnetrygd`() {
            testWithBrukerContext(groups = listOf(rolleConfig.ba.saksbehandler)) {
                assertThat(tilgangService.harTilgangTilBehandlingGittRolle(behandlingBa.id, BehandlerRolle.SAKSBEHANDLER)).isTrue
            }
        }

        @Test
        internal fun `ef-saksbehandler har ikke tilgang til behandling av fagsystem barnetrygd`() {
            testWithBrukerContext(groups = listOf(rolleConfig.ef.saksbehandler)) {
                assertThat(tilgangService.harTilgangTilBehandlingGittRolle(behandlingBa.id, BehandlerRolle.SAKSBEHANDLER)).isFalse
            }
        }

        @Test
        internal fun `veileder har ikke tilgang som saksbehandler eller beslutter`() {
            testWithBrukerContext(groups = listOf(rolleConfig.ba.veileder)) {
                assertThat(tilgangService.harTilgangTilBehandlingGittRolle(behandlingBa.id, BehandlerRolle.VEILEDER)).isTrue
                assertThat(tilgangService.harTilgangTilBehandlingGittRolle(behandlingBa.id, BehandlerRolle.SAKSBEHANDLER)).isFalse
                assertThat(tilgangService.harTilgangTilBehandlingGittRolle(behandlingBa.id, BehandlerRolle.BESLUTTER)).isFalse
            }
        }

        @Test
        internal fun `saksbehandler har tilgang som veileder og saksbehandler, men ikke beslutter`() {
            testWithBrukerContext(groups = listOf(rolleConfig.ef.saksbehandler)) {
                assertThat(tilgangService.harTilgangTilBehandlingGittRolle(behandlingEf.id, BehandlerRolle.VEILEDER)).isTrue
                assertThat(tilgangService.harTilgangTilBehandlingGittRolle(behandlingEf.id, BehandlerRolle.SAKSBEHANDLER)).isTrue
                assertThat(tilgangService.harTilgangTilBehandlingGittRolle(behandlingEf.id, BehandlerRolle.BESLUTTER)).isFalse
            }
        }

        @Test
        internal fun `beslutter har tilgang som saksbehandler, beslutter og veileder`() {
            testWithBrukerContext(groups = listOf(rolleConfig.ef.beslutter)) {
                assertThat(tilgangService.harTilgangTilBehandlingGittRolle(behandlingEf.id, BehandlerRolle.VEILEDER)).isTrue
                assertThat(tilgangService.harTilgangTilBehandlingGittRolle(behandlingEf.id, BehandlerRolle.SAKSBEHANDLER)).isTrue
                assertThat(tilgangService.harTilgangTilBehandlingGittRolle(behandlingEf.id, BehandlerRolle.BESLUTTER)).isTrue
            }
        }

        @Nested
        inner class ValiderTilgangTilPersonMedRelasjonerForFagsak {
            @ParameterizedTest
            @EnumSource(Stønadstype::class, names = ["BARNETRYGD", "KONTANTSTØTTE"])
            fun `skal kaste feil dersom saksbehandler ikke har tilgang til fagsak når fagsystem er BA eller KS`(stønadstype: Stønadstype) {
                // Arrange
                val fagsak = fagsak(stønadstype = stønadstype)
                every { fagsakService.hentFagsak(fagsak.id) } returns fagsak
                every { familieBASakClient.hentTilgangTilFagsak(fagsak.eksternId) } returns Tilgang(harTilgang = false, begrunnelse = "Ingen tilgang")
                every { familieKSSakClient.hentTilgangTilFagsak(fagsak.eksternId) } returns Tilgang(harTilgang = false, begrunnelse = "Ingen tilgang")

                // Act & Assert
                val manglerTilgangException = assertThrows<ManglerTilgang> { tilgangService.validerTilgangTilFagsak(fagsak.id, AuditLoggerEvent.ACCESS) }

                when (stønadstype) {
                    Stønadstype.BARNETRYGD -> {
                        verify(exactly = 1) { familieBASakClient.hentTilgangTilFagsak(fagsak.eksternId) }
                        verify(exactly = 0) { familieKSSakClient.hentTilgangTilFagsak(fagsak.eksternId) }
                    }

                    Stønadstype.KONTANTSTØTTE -> {
                        verify(exactly = 1) { familieKSSakClient.hentTilgangTilFagsak(fagsak.eksternId) }
                        verify(exactly = 0) { familieBASakClient.hentTilgangTilFagsak(fagsak.eksternId) }
                    }

                    else -> {
                        return
                    }
                }

                assertThat(manglerTilgangException.message)
                    .isEqualTo(
                        "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                            "har ikke tilgang til fagsak=${fagsak.id}",
                    )
                assertThat(manglerTilgangException.frontendFeilmelding).isEqualTo("Mangler tilgang til opplysningene. Årsak: Ingen tilgang")
            }

            @ParameterizedTest
            @EnumSource(Stønadstype::class, names = ["BARNETRYGD", "KONTANTSTØTTE"])
            fun `skal ikke kaste feil dersom saksbehandler har tilgang til fagsak når fagsystem er BA eller KS`(stønadstype: Stønadstype) {
                // Arrange
                val fagsak = fagsak(stønadstype = stønadstype)
                every { fagsakService.hentFagsak(fagsak.id) } returns fagsak
                every { familieBASakClient.hentTilgangTilFagsak(fagsak.eksternId) } returns Tilgang(harTilgang = true)
                every { familieKSSakClient.hentTilgangTilFagsak(fagsak.eksternId) } returns Tilgang(harTilgang = true)

                // Act & Assert
                assertDoesNotThrow { tilgangService.validerTilgangTilFagsak(fagsak.id, AuditLoggerEvent.ACCESS) }

                when (stønadstype) {
                    Stønadstype.BARNETRYGD -> {
                        verify(exactly = 1) { familieBASakClient.hentTilgangTilFagsak(fagsak.eksternId) }
                        verify(exactly = 0) { familieKSSakClient.hentTilgangTilFagsak(fagsak.eksternId) }
                    }

                    Stønadstype.KONTANTSTØTTE -> {
                        verify(exactly = 1) { familieKSSakClient.hentTilgangTilFagsak(fagsak.eksternId) }
                        verify(exactly = 0) { familieBASakClient.hentTilgangTilFagsak(fagsak.eksternId) }
                    }

                    else -> {
                        return
                    }
                }
            }

            @Test
            fun `skal kaste feil dersom saksbehandler ikke har tilgang til fagsak når fagsystem er EF`() {
                // Arrange
                val fagsak = fagsak()
                every { fagsakService.hentFagsak(fagsak.id) } returns fagsak

                mockTilgangTilPersonMedRelasjoner(Tema.ENF, PersonTilgang.avvist(fagsak.hentFagsakEierIdent(), Avvisningskode.AVVIST_SKJERMING, "Ingen tilgang"))

                // Act & Assert
                testWithBrukerContext {
                    val manglerTilgangException = assertThrows<ManglerTilgang> { tilgangService.validerTilgangTilFagsak(fagsak.id, AuditLoggerEvent.ACCESS) }

                    verify(exactly = 1) { tilgangsmaskinTilgangskontrollKlient.sjekkTilgangTilPersoner(setOf(fagsak.hentFagsakEierIdent())) }
                    assertThat(manglerTilgangException.message)
                        .isEqualTo(
                            "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                                "har ikke tilgang til fagsak=${fagsak.id}",
                        )
                    assertThat(manglerTilgangException.frontendFeilmelding).isEqualTo("Mangler tilgang til opplysningene. Årsak: Ingen tilgang")
                }
            }

            @Test
            fun `skal ikke kaste feil dersom saksbehandler har tilgang til fagsak når fagsystem er EF`() {
                // Arrange
                val fagsak = fagsak()
                every { fagsakService.hentFagsak(fagsak.id) } returns fagsak

                mockTilgangTilPersonMedRelasjoner(Tema.ENF, PersonTilgang.medTilgang(fagsak.hentFagsakEierIdent()))

                // Act & Assert
                testWithBrukerContext {
                    assertDoesNotThrow { tilgangService.validerTilgangTilFagsak(fagsak.id, AuditLoggerEvent.ACCESS) }
                    verify(exactly = 1) { tilgangsmaskinTilgangskontrollKlient.sjekkTilgangTilPersoner(setOf(fagsak.hentFagsakEierIdent())) }
                }
            }
        }

        @Nested
        inner class ValiderTilgangTilEksternFagsak {
            @ParameterizedTest
            @EnumSource(Stønadstype::class, names = ["BARNETRYGD", "KONTANTSTØTTE"])
            fun `skal kaste feil dersom saksbehandler ikke har tilgang til ekstern fagsak når fagsystem er BA eller KS`(stønadstype: Stønadstype) {
                // Arrange
                val fagsak = fagsak(stønadstype = stønadstype)
                val fagsystem = if (stønadstype == Stønadstype.BARNETRYGD) Fagsystem.BA else Fagsystem.KS

                every { fagsakService.hentFagsakForEksternIdOgFagsystem(eksternId = fagsak.eksternId, fagsystem = fagsystem) } returns fagsak
                every { familieBASakClient.hentTilgangTilFagsak(fagsak.eksternId) } returns Tilgang(harTilgang = false, begrunnelse = "Ingen tilgang")
                every { familieKSSakClient.hentTilgangTilFagsak(fagsak.eksternId) } returns Tilgang(harTilgang = false, begrunnelse = "Ingen tilgang")

                // Act & Assert
                val manglerTilgangException =
                    assertThrows<ManglerTilgang> {
                        tilgangService.validerTilgangTilEksternFagsak(
                            eksternFagsakId = fagsak.eksternId,
                            fagsystem = fagsystem,
                            event = AuditLoggerEvent.ACCESS,
                        )
                    }

                when (stønadstype) {
                    Stønadstype.BARNETRYGD -> {
                        verify(exactly = 1) { familieBASakClient.hentTilgangTilFagsak(fagsak.eksternId) }
                        verify(exactly = 0) { familieKSSakClient.hentTilgangTilFagsak(fagsak.eksternId) }
                    }

                    Stønadstype.KONTANTSTØTTE -> {
                        verify(exactly = 1) { familieKSSakClient.hentTilgangTilFagsak(fagsak.eksternId) }
                        verify(exactly = 0) { familieBASakClient.hentTilgangTilFagsak(fagsak.eksternId) }
                    }

                    else -> {
                        return
                    }
                }

                assertThat(manglerTilgangException.message)
                    .isEqualTo(
                        "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                            "har ikke tilgang til fagsak=${fagsak.id}",
                    )
                assertThat(manglerTilgangException.frontendFeilmelding).isEqualTo("Mangler tilgang til opplysningene. Årsak: Ingen tilgang")
            }

            @ParameterizedTest
            @EnumSource(Stønadstype::class, names = ["BARNETRYGD", "KONTANTSTØTTE"])
            fun `skal ikke kaste feil dersom saksbehandler har tilgang til ekstern fagsak når fagsystem er BA eller KS`(stønadstype: Stønadstype) {
                // Arrange
                val fagsak = fagsak(stønadstype = stønadstype)
                val fagsystem = if (stønadstype == Stønadstype.BARNETRYGD) Fagsystem.BA else Fagsystem.KS

                every { fagsakService.hentFagsakForEksternIdOgFagsystem(fagsak.eksternId, fagsystem) } returns fagsak
                every { familieBASakClient.hentTilgangTilFagsak(fagsak.eksternId) } returns Tilgang(harTilgang = true)
                every { familieKSSakClient.hentTilgangTilFagsak(fagsak.eksternId) } returns Tilgang(harTilgang = true)

                // Act & Assert
                assertDoesNotThrow {
                    tilgangService.validerTilgangTilEksternFagsak(
                        eksternFagsakId = fagsak.eksternId,
                        fagsystem = fagsystem,
                        event = AuditLoggerEvent.ACCESS,
                    )
                }

                when (stønadstype) {
                    Stønadstype.BARNETRYGD -> {
                        verify(exactly = 1) { familieBASakClient.hentTilgangTilFagsak(fagsak.eksternId) }
                        verify(exactly = 0) { familieKSSakClient.hentTilgangTilFagsak(fagsak.eksternId) }
                    }

                    Stønadstype.KONTANTSTØTTE -> {
                        verify(exactly = 1) { familieKSSakClient.hentTilgangTilFagsak(fagsak.eksternId) }
                        verify(exactly = 0) { familieBASakClient.hentTilgangTilFagsak(fagsak.eksternId) }
                    }

                    else -> {
                        return
                    }
                }
            }
        }

        @Nested
        inner class ValiderTilgangTilBehandling {
            @ParameterizedTest
            @EnumSource(Stønadstype::class, names = ["BARNETRYGD", "KONTANTSTØTTE"])
            fun `skal kaste feil dersom saksbehandler ikke har tilgang til behandling når fagsystem er BA eller KS`(stønadstype: Stønadstype) {
                // Arrange
                val fagsak = fagsak(stønadstype = stønadstype)
                val behandling = behandling(fagsak)

                every { fagsakService.hentFagsakForBehandling(behandling.id) } returns fagsak
                every { familieBASakClient.hentTilgangTilFagsak(fagsak.eksternId) } returns Tilgang(harTilgang = false, begrunnelse = "Ingen tilgang")
                every { familieKSSakClient.hentTilgangTilFagsak(fagsak.eksternId) } returns Tilgang(harTilgang = false, begrunnelse = "Ingen tilgang")

                // Act & Assert
                val manglerTilgangException = assertThrows<ManglerTilgang> { tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS) }

                when (stønadstype) {
                    Stønadstype.BARNETRYGD -> {
                        verify(exactly = 1) { familieBASakClient.hentTilgangTilFagsak(fagsak.eksternId) }
                        verify(exactly = 0) { familieKSSakClient.hentTilgangTilFagsak(fagsak.eksternId) }
                    }

                    Stønadstype.KONTANTSTØTTE -> {
                        verify(exactly = 1) { familieKSSakClient.hentTilgangTilFagsak(fagsak.eksternId) }
                        verify(exactly = 0) { familieBASakClient.hentTilgangTilFagsak(fagsak.eksternId) }
                    }

                    else -> {
                        return
                    }
                }

                assertThat(manglerTilgangException.message)
                    .isEqualTo(
                        "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                            "har ikke tilgang til behandling=${behandling.id}",
                    )
                assertThat(manglerTilgangException.frontendFeilmelding).isEqualTo("Mangler tilgang til opplysningene. Årsak: Ingen tilgang")
            }

            @ParameterizedTest
            @EnumSource(Stønadstype::class, names = ["BARNETRYGD", "KONTANTSTØTTE"])
            fun `skal ikke kaste feil dersom saksbehandler har tilgang til behandling når fagsystem er BA eller KS`(stønadstype: Stønadstype) {
                // Arrange
                val fagsak = fagsak(stønadstype = stønadstype)
                val behandling = behandling(fagsak)

                every { fagsakService.hentFagsakForBehandling(behandling.id) } returns fagsak
                every { familieBASakClient.hentTilgangTilFagsak(fagsak.eksternId) } returns Tilgang(harTilgang = true)
                every { familieKSSakClient.hentTilgangTilFagsak(fagsak.eksternId) } returns Tilgang(harTilgang = true)

                // Act & Assert
                assertDoesNotThrow { tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS) }

                when (stønadstype) {
                    Stønadstype.BARNETRYGD -> {
                        verify(exactly = 1) { familieBASakClient.hentTilgangTilFagsak(fagsak.eksternId) }
                        verify(exactly = 0) { familieKSSakClient.hentTilgangTilFagsak(fagsak.eksternId) }
                    }

                    Stønadstype.KONTANTSTØTTE -> {
                        verify(exactly = 1) { familieKSSakClient.hentTilgangTilFagsak(fagsak.eksternId) }
                        verify(exactly = 0) { familieBASakClient.hentTilgangTilFagsak(fagsak.eksternId) }
                    }

                    else -> {
                        return
                    }
                }
            }

            @Test
            fun `skal kaste feil dersom saksbehandler ikke har tilgang til behandling når fagsystem er EF`() {
                // Arrange
                val fagsak = fagsak()
                val behandling = behandling(fagsak)

                every { fagsakService.hentFagsakForBehandling(behandling.id) } returns fagsak
                mockTilgangTilPersonMedRelasjoner(Tema.ENF, PersonTilgang.avvist(fagsak.hentFagsakEierIdent(), Avvisningskode.AVVIST_SKJERMING, "Ingen tilgang"))

                // Act & Assert
                testWithBrukerContext {
                    val manglerTilgangException = assertThrows<ManglerTilgang> { tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS) }

                    verify(exactly = 1) { tilgangsmaskinTilgangskontrollKlient.sjekkTilgangTilPersoner(setOf(fagsak.hentFagsakEierIdent())) }
                    assertThat(manglerTilgangException.message)
                        .isEqualTo(
                            "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                                "har ikke tilgang til behandling=${behandling.id}",
                        )
                    assertThat(manglerTilgangException.frontendFeilmelding).isEqualTo("Mangler tilgang til opplysningene. Årsak: Ingen tilgang")
                }
            }

            @Test
            fun `skal ikke kaste feil dersom saksbehandler har tilgang til behandling når fagsystem er EF`() {
                // Arrange
                val fagsak = fagsak()
                val behandling = behandling(fagsak)

                every { fagsakService.hentFagsakForBehandling(behandling.id) } returns fagsak
                mockTilgangTilPersonMedRelasjoner(Tema.ENF, PersonTilgang.medTilgang(fagsak.hentFagsakEierIdent()))

                // Act & Assert
                testWithBrukerContext {
                    assertDoesNotThrow { tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS) }
                    verify(exactly = 1) { tilgangsmaskinTilgangskontrollKlient.sjekkTilgangTilPersoner(setOf(fagsak.hentFagsakEierIdent())) }
                }
            }
        }
    }

    @Nested
    inner class ValiderTilgangTilPersonMedRelasjoner {
        private val tema = Tema.BAR
        private val søker = "01010199999"
        private val barn = "01011099999"
        private val annenForelder = "02020299999"

        @Test
        fun `skal ikke kaste feil når saksbehandler har tilgang til personen og alle relasjonene`() {
            // Arrange
            mockTilgangTilPersonMedRelasjoner(
                tema,
                PersonTilgang.medTilgang(søker),
                PersonTilgang.medTilgang(barn),
                PersonTilgang.medTilgang(annenForelder),
            )

            // Act & Assert
            testWithBrukerContext {
                assertDoesNotThrow { tilgangService.validerTilgangTilPersonMedRelasjoner(søker, tema, AuditLoggerEvent.ACCESS) }
            }
        }

        @Test
        fun `skal kaste feil når saksbehandler mangler tilgang til en av relasjonene`() {
            // Arrange
            mockTilgangTilPersonMedRelasjoner(
                tema,
                PersonTilgang.medTilgang(søker),
                PersonTilgang.avvist(barn, Avvisningskode.AVVIST_STRENGT_FORTROLIG_ADRESSE, BEGRUNNELSE_STRENGT_FORTROLIG),
                PersonTilgang.medTilgang(annenForelder),
            )

            // Act
            val manglerTilgang =
                testWithBrukerContext {
                    assertThrows<ManglerTilgang> { tilgangService.validerTilgangTilPersonMedRelasjoner(søker, tema, AuditLoggerEvent.ACCESS) }
                }

            // Assert
            assertThat(manglerTilgang.frontendFeilmelding).isEqualTo("Mangler tilgang til opplysningene. Årsak: $BEGRUNNELSE_STRENGT_FORTROLIG")
            assertThat(manglerTilgang.message).doesNotContain(barn)
        }

        @Test
        fun `skal slå sammen ulike begrunnelser og fjerne duplikater`() {
            // Arrange
            mockTilgangTilPersonMedRelasjoner(
                tema,
                PersonTilgang.avvist(søker, Avvisningskode.AVVIST_SKJERMING, BEGRUNNELSE_SKJERMING),
                PersonTilgang.avvist(barn, Avvisningskode.AVVIST_STRENGT_FORTROLIG_ADRESSE, BEGRUNNELSE_STRENGT_FORTROLIG),
                PersonTilgang.avvist(annenForelder, Avvisningskode.AVVIST_STRENGT_FORTROLIG_ADRESSE, BEGRUNNELSE_STRENGT_FORTROLIG),
            )

            // Act
            val manglerTilgang =
                testWithBrukerContext {
                    assertThrows<ManglerTilgang> { tilgangService.validerTilgangTilPersonMedRelasjoner(søker, tema, AuditLoggerEvent.ACCESS) }
                }

            // Assert
            assertThat(manglerTilgang.frontendFeilmelding)
                .isEqualTo("Mangler tilgang til opplysningene. Årsak: $BEGRUNNELSE_SKJERMING. $BEGRUNNELSE_STRENGT_FORTROLIG")
        }

        @Test
        fun `skal kaste den opprinnelige feilen når tilgangssjekken feiler`() {
            // Arrange
            val feil = Feil(message = "Fikk ikke gyldig svar fra Tilgangsmaskinen for 1 av 1 identer.", frontendFeilmelding = "Prøv igjen senere.")
            every { personMedRelasjonerService.hentIdenterForPersonMedRelasjoner(søker, tema) } returns setOf(søker)
            every { tilgangsmaskinTilgangskontrollKlient.sjekkTilgangTilPersoner(setOf(søker)) } throws feil

            // Act
            val kastetFeil =
                testWithBrukerContext {
                    assertThrows<Feil> { tilgangService.validerTilgangTilPersonMedRelasjoner(søker, tema, AuditLoggerEvent.ACCESS) }
                }

            // Assert
            assertThat(kastetFeil).isSameAs(feil)
        }

        @Test
        fun `skal cache tilgangen per saksbehandler`() {
            // Arrange
            mockTilgangTilPersonMedRelasjoner(tema, PersonTilgang.medTilgang(søker), PersonTilgang.medTilgang(barn))
            testWithBrukerContext { tilgangService.validerTilgangTilPersonMedRelasjoner(søker, tema, AuditLoggerEvent.ACCESS) }

            // Act
            testWithBrukerContext { tilgangService.validerTilgangTilPersonMedRelasjoner(søker, tema, AuditLoggerEvent.ACCESS) }
            testWithBrukerContext(preferredUsername = "Annen saksbehandler") {
                tilgangService.validerTilgangTilPersonMedRelasjoner(søker, tema, AuditLoggerEvent.ACCESS)
            }

            // Assert
            verify(exactly = 2) { personMedRelasjonerService.hentIdenterForPersonMedRelasjoner(søker, tema) }
            verify(exactly = 2) { tilgangsmaskinTilgangskontrollKlient.sjekkTilgangTilPersoner(setOf(søker, barn)) }
        }
    }

    companion object {
        private const val BEGRUNNELSE_STRENGT_FORTROLIG = "Du har ikke tilgang til brukere med strengt fortrolig adresse (kode 6)"
        private const val BEGRUNNELSE_SKJERMING = "Du har ikke tilgang til Nav-ansatte og deres nærmeste familie"
    }
}
