package no.nav.familie.klage.institusjon

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.klage.integrasjoner.FamilieIntegrasjonerClient
import no.nav.familie.klage.testutil.DomainUtil.lagInstitusjon
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.UUID

class InstitusjonServiceTest {
    private val institusjonRepository = mockk<InstitusjonRepository>()
    private val familieIntegrasjonerClient = mockk<FamilieIntegrasjonerClient>()

    private val institusjonService =
        InstitusjonService(
            institusjonRepository,
            familieIntegrasjonerClient,
        )

    @Nested
    inner class HentEllerLagreInstitusjon {
        @Test
        fun `skal hente allerde eksistrende institusjon`() {
            // Arrange
            val lagretInstitusjon = lagInstitusjon()

            every { institusjonRepository.finnInstitusjon(lagretInstitusjon.orgNummer) } returns lagretInstitusjon

            // Act
            val hentetInstitusjon = institusjonService.hentEllerLagreInstitusjon(lagretInstitusjon.orgNummer)

            // Assert
            assertThat(hentetInstitusjon).isEqualTo(lagretInstitusjon)
        }

        @Test
        fun `skal opprette institusjon om det ikke allerede finnes`() {
            // Arrange
            val organisasjon =
                Organisasjon(
                    organisasjonsnummer = "123456789",
                    navn = "navn",
                )

            every { institusjonRepository.finnInstitusjon(organisasjon.organisasjonsnummer) } returns null
            every { familieIntegrasjonerClient.hentOrganisasjon(organisasjon.organisasjonsnummer) } returns organisasjon
            every { institusjonRepository.insert(any()) } returnsArgument 0

            // Act
            val hentetInstitusjon = institusjonService.hentEllerLagreInstitusjon(organisasjon.organisasjonsnummer)

            // Assert
            assertThat(hentetInstitusjon.id).isNotNull()
            assertThat(hentetInstitusjon.orgNummer).isEqualTo(organisasjon.organisasjonsnummer)
            assertThat(hentetInstitusjon.navn).isEqualTo(organisasjon.navn)
        }
    }

    @Nested
    inner class FinnInstitusjon {
        @Test
        fun `skal finne institusjon med id`() {
            // Arrange
            val lagretInstitusjon = lagInstitusjon()

            every { institusjonRepository.findById(lagretInstitusjon.id) } returns Optional.of(lagretInstitusjon)

            // Act
            val hentetInstitusjon = institusjonService.finnInstitusjon(lagretInstitusjon.id)

            // Assert
            assertThat(hentetInstitusjon).isEqualTo(lagretInstitusjon)
        }

        @Test
        fun `skal returnere null hvis institusjon med id ikke eksisterer`() {
            // Arrange
            every { institusjonRepository.findById(any()) } returns Optional.empty()

            // Act
            val hentetInstitusjon = institusjonService.finnInstitusjon(UUID.randomUUID())

            // Assert
            assertThat(hentetInstitusjon).isNull()
        }
    }
}
