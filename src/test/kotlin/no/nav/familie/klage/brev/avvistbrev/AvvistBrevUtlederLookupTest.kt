package no.nav.familie.klage.brev.avvistbrev

import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import no.nav.familie.klage.testutil.DomainUtil.ikkeOppfyltForm
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired

class AvvistBrevUtlederLookupTest(
    @Autowired
    private val avvistBrevInnholdUtlederLookup: AvvistBrevInnholdUtleder.Lookup,
) : OppslagSpringRunnerTest() {
    @Test
    fun `skal hente EFAvvistBrevUtleder dersom fagsystem er EF`() {
        // Act
        val avvistBrevUtleder = avvistBrevInnholdUtlederLookup.hentAvvistBrevUtlederForFagsystem(Fagsystem.EF)

        // Assert
        assertNotNull(avvistBrevUtleder)
        assertInstanceOf(EFAvvistBrevInnholdUtleder::class.java, avvistBrevUtleder)
        assertDoesNotThrow {
            avvistBrevUtleder.utledBrevInnhold(
                fagsak = fagsak(),
                form = ikkeOppfyltForm(),
            )
        }
    }

    @Test
    fun `skal hente BAAvvistBrevUtleder dersom fagsystem er BA`() {
        // Act
        val avvistBrevUtleder = avvistBrevInnholdUtlederLookup.hentAvvistBrevUtlederForFagsystem(Fagsystem.BA)

        // Assert
        assertNotNull(avvistBrevUtleder)
        assertInstanceOf(BAAvvistBrevInnholdUtleder::class.java, avvistBrevUtleder)
        assertDoesNotThrow {
            avvistBrevUtleder.utledBrevInnhold(
                fagsak = fagsak(),
                form = ikkeOppfyltForm(),
            )
        }
    }

    @Test
    fun `skal hente KSAvvistBrevUtleder dersom fagsystem er KS`() {
        // Act
        val avvistBrevUtleder = avvistBrevInnholdUtlederLookup.hentAvvistBrevUtlederForFagsystem(Fagsystem.KS)

        // Assert
        assertNotNull(avvistBrevUtleder)
        assertInstanceOf(KSAvvistBrevInnholdUtleder::class.java, avvistBrevUtleder)
        assertDoesNotThrow {
            avvistBrevUtleder.utledBrevInnhold(
                fagsak = fagsak(),
                form = ikkeOppfyltForm(),
            )
        }
    }
}
