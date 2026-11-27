package no.nav.familie.klage.personopplysninger

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.klage.felles.domain.AuditLoggerEvent
import no.nav.familie.klage.infrastruktur.exception.ManglerTilgang
import no.nav.familie.klage.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.klage.testutil.DtoTestUtil.lagPersonopplysningerDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

internal class PersonopplysningerControllerTest {
    private val personopplysningerService = mockk<PersonopplysningerService>()
    private val tilgangService = mockk<TilgangService>()

    private val personopplysningerController =
        PersonopplysningerController(
            personopplysningerService = personopplysningerService,
            tilgangService = tilgangService,
        )

    private val behandlingId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS) }
        justRun { tilgangService.validerTilgangTilSøker(behandlingId, AuditLoggerEvent.ACCESS) }
        justRun { tilgangService.validerHarVeilederrolleTilStønadForBehandling(behandlingId) }
        every { personopplysningerService.hentPersonopplysningerFagsakEier(behandlingId) } returns lagPersonopplysningerDto(personIdent = "1")
        every { personopplysningerService.hentPersonopplysningerSøker(behandlingId) } returns lagPersonopplysningerDto(personIdent = "2")
    }

    @Test
    fun `skal validere tilgang til søker før personopplysninger om fagsakeier og søker hentes`() {
        // Act
        val personopplysninger = personopplysningerController.hentPersonopplysningerForFagsakEierOgSøker(behandlingId).data

        // Assert
        assertThat(personopplysninger?.søker?.personIdent).isEqualTo("2")
        verify(exactly = 1) { tilgangService.validerTilgangTilSøker(behandlingId, AuditLoggerEvent.ACCESS) }
    }

    @Test
    fun `skal ikke hente personopplysninger om søker når saksbehandler mangler tilgang til søker`() {
        // Arrange
        every {
            tilgangService.validerTilgangTilSøker(behandlingId, AuditLoggerEvent.ACCESS)
        } throws ManglerTilgang(melding = "Mangler tilgang til søker", frontendFeilmelding = "Mangler tilgang til opplysningene.")

        // Act & Assert
        assertThrows<ManglerTilgang> {
            personopplysningerController.hentPersonopplysningerForFagsakEierOgSøker(behandlingId)
        }

        verify(exactly = 0) { personopplysningerService.hentPersonopplysningerSøker(any()) }
        verify(exactly = 0) { personopplysningerService.hentPersonopplysningerFagsakEier(any()) }
    }
}
