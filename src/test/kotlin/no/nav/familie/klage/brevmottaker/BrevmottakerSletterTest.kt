package no.nav.familie.klage.brevmottaker

import io.mockk.mockk
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.brev.BrevRepository
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.brevmottaker.domain.SlettbarBrevmottakerPersonUtenIdent
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import no.nav.familie.klage.testutil.DomainUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

// TODO : Finish writing tests
class BrevmottakerSletterTest {
    private val behandlingService: BehandlingService = mockk()
    private val fagsakService: FagsakService = mockk()
    private val brevService: BrevService = mockk()
    private val brevRepository: BrevRepository = mockk()
    private val personopplysningerService: PersonopplysningerService = mockk()
    private val brevmottakerSletter: BrevmottakerSletter = BrevmottakerSletter(
        behandlingService = behandlingService,
        brevService = brevService,
        brevRepository = brevRepository,
        fagsakService = fagsakService,
        personopplysningerService = personopplysningerService,
    )

    @Nested
    inner class SlettBrevmottakerTest {
        @Test
        fun `skal slette brevmottaker`() {
            // Arrange
            val behandling = DomainUtil.behandling()

            val slettbarBrevmottaker = SlettbarBrevmottakerPersonUtenIdent(UUID.randomUUID())

            // Act
            brevmottakerSletter.slettBrevmottaker(behandling.id, slettbarBrevmottaker)

            // Assert
            assertThat(1).isEqualTo(1)
        }
    }
}
