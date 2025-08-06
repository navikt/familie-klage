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
import no.nav.familie.klage.infrastruktur.exception.ManglerTilgang
import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.klage.infrastruktur.featuretoggle.Toggle
import no.nav.familie.klage.integrasjoner.FamilieBASakClient
import no.nav.familie.klage.integrasjoner.FamilieKSSakClient
import no.nav.familie.klage.personopplysninger.PersonopplysningerIntegrasjonerClient
import no.nav.familie.klage.testutil.BrukerContextUtil.testWithBrukerContext
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.Stønadstype
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
    private val personopplysningerIntegrasjonerClient = mockk<PersonopplysningerIntegrasjonerClient>()
    private val rolleConfig = RolleConfigTestUtil.rolleConfig
    private val cacheManager = ConcurrentMapCacheManager()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>()
    private val fagsakService = mockk<FagsakService>()
    private val familieBASakClient = mockk<FamilieBASakClient>()
    private val familieKSSakClient = mockk<FamilieKSSakClient>()
    private val featureToggleService = mockk<FeatureToggleService>()

    private val tilgangService =
        TilgangService(
            personopplysningerIntegrasjonerClient,
            rolleConfig,
            cacheManager,
            auditLogger,
            behandlingService,
            fagsakService,
            familieBASakClient,
            familieKSSakClient,
            featureToggleService,
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
                every { featureToggleService.isEnabled(Toggle.BRUK_NY_TILGANG_KONTROLL_BAKS) } returns true
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
                    else -> return
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
                every { featureToggleService.isEnabled(Toggle.BRUK_NY_TILGANG_KONTROLL_BAKS) } returns true
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
                    else -> return
                }
            }

            @Test
            fun `skal kaste feil dersom saksbehandler ikke har tilgang til fagsak når fagsystem er EF`() {
                // Arrange
                val fagsak = fagsak()
                every { fagsakService.hentFagsak(fagsak.id) } returns fagsak

                every { personopplysningerIntegrasjonerClient.sjekkTilgangTilPersonMedRelasjoner(fagsak.hentAktivIdent()) } returns Tilgang(harTilgang = false, begrunnelse = "Ingen tilgang")

                // Act & Assert
                testWithBrukerContext {
                    val manglerTilgangException = assertThrows<ManglerTilgang> { tilgangService.validerTilgangTilFagsak(fagsak.id, AuditLoggerEvent.ACCESS) }

                    verify(exactly = 1) { personopplysningerIntegrasjonerClient.sjekkTilgangTilPersonMedRelasjoner(fagsak.hentAktivIdent()) }
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

                every { personopplysningerIntegrasjonerClient.sjekkTilgangTilPersonMedRelasjoner(fagsak.hentAktivIdent()) } returns Tilgang(harTilgang = true)

                // Act & Assert
                testWithBrukerContext {
                    assertDoesNotThrow { tilgangService.validerTilgangTilFagsak(fagsak.id, AuditLoggerEvent.ACCESS) }
                    verify(exactly = 1) { personopplysningerIntegrasjonerClient.sjekkTilgangTilPersonMedRelasjoner(fagsak.hentAktivIdent()) }
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
                every { featureToggleService.isEnabled(Toggle.BRUK_NY_TILGANG_KONTROLL_BAKS) } returns true
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
                    else -> return
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
                every { featureToggleService.isEnabled(Toggle.BRUK_NY_TILGANG_KONTROLL_BAKS) } returns true
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
                    else -> return
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
                every { featureToggleService.isEnabled(Toggle.BRUK_NY_TILGANG_KONTROLL_BAKS) } returns true
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
                    else -> return
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
                every { featureToggleService.isEnabled(Toggle.BRUK_NY_TILGANG_KONTROLL_BAKS) } returns true
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
                    else -> return
                }
            }

            @Test
            fun `skal kaste feil dersom saksbehandler ikke har tilgang til behandling når fagsystem er EF`() {
                // Arrange
                val fagsak = fagsak()
                val behandling = behandling(fagsak)

                every { fagsakService.hentFagsakForBehandling(behandling.id) } returns fagsak
                every { personopplysningerIntegrasjonerClient.sjekkTilgangTilPersonMedRelasjoner(fagsak.hentAktivIdent()) } returns Tilgang(harTilgang = false, begrunnelse = "Ingen tilgang")

                // Act & Assert
                testWithBrukerContext {
                    val manglerTilgangException = assertThrows<ManglerTilgang> { tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS) }

                    verify(exactly = 1) { personopplysningerIntegrasjonerClient.sjekkTilgangTilPersonMedRelasjoner(fagsak.hentAktivIdent()) }
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
                every { personopplysningerIntegrasjonerClient.sjekkTilgangTilPersonMedRelasjoner(fagsak.hentAktivIdent()) } returns Tilgang(harTilgang = true)

                // Act & Assert
                testWithBrukerContext {
                    assertDoesNotThrow { tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS) }
                    verify(exactly = 1) { personopplysningerIntegrasjonerClient.sjekkTilgangTilPersonMedRelasjoner(fagsak.hentAktivIdent()) }
                }
            }
        }
    }
}
