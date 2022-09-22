package no.nav.familie.klage

import no.nav.familie.klage.formkrav.FormController
import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FlytTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var formController: FormController

    private val fagsak = fagsak()
    private val behandling = behandling(fagsak)

    @BeforeEach
    internal fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagreBehandling(behandling)
    }

    @Test
    internal fun `skal sende melding til kabal ved`() {
        // formController.hentVilkår()
    }
}
