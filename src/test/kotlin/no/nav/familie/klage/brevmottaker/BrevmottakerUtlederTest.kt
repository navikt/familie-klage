package no.nav.familie.klage.brevmottaker

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonMedIdent
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.fagsak.domain.PersonIdent
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import no.nav.familie.klage.testutil.DomainUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class BrevmottakerUtlederTest {
    private val fagsakService = mockk<FagsakService>()
    private val personopplysningerService = mockk<PersonopplysningerService>()
    private val brevmottakerUtleder =
        BrevmottakerUtleder(
            fagsakService = fagsakService,
            personopplysningerService = personopplysningerService,
        )

    @Nested
    inner class UtledInitielleBrevmottakere {
        @Test
        fun `skal utlede initielle brevmottakere`() {
            // Arrange
            val personIdent = "123"
            val behandlingId = UUID.randomUUID()
            val fagsak = DomainUtil.fagsak(identer = setOf(PersonIdent(personIdent)))
            val personopplysninger = DomainUtil.personopplysningerDto(navn = "Navn Navnesen")

            every { fagsakService.hentFagsakForBehandling(behandlingId) } returns fagsak
            every { personopplysningerService.hentPersonopplysninger(behandlingId) } returns personopplysninger

            // Act
            val brevmottakere = brevmottakerUtleder.utledInitielleBrevmottakere(behandlingId)

            // Assert
            assertThat(brevmottakere.personer).hasSize(1)
            assertThat(brevmottakere.personer[0]).isInstanceOfSatisfying(BrevmottakerPersonMedIdent::class.java) {
                assertThat(it.personIdent).isEqualTo(personIdent)
                assertThat(it.navn).isEqualTo("Navn Navnesen")
                assertThat(it.mottakerRolle).isEqualTo(MottakerRolle.BRUKER)
            }
            assertThat(brevmottakere.organisasjoner).isEmpty()
        }
    }
}
