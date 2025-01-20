package no.nav.familie.klage.brev.felles

import no.nav.familie.klage.brev.baks.avvistbrev.BAAvvistBrevInnholdUtleder
import no.nav.familie.klage.brev.baks.avvistbrev.KSAvvistBrevInnholdUtleder
import no.nav.familie.klage.brev.ef.avvistbrev.EFAvvistBrevInnholdUtleder
import no.nav.familie.klage.formkrav.domain.FormVilkår
import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.testutil.DomainUtil.oppfyltForm
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class AvvistBrevUtlederLookupTest(
    @Autowired
    private val avvistBrevInnholdUtlederLookup: AvvistBrevInnholdUtleder.Lookup,
    @Autowired
    private val efAvvistBrevUtleder: EFAvvistBrevInnholdUtleder,
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
                oppfyltForm(behandlingId = UUID.randomUUID()).copy(
                    klagePart = FormVilkår.IKKE_OPPFYLT,
                    brevtekst = "Test",
                ),
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
                oppfyltForm(behandlingId = UUID.randomUUID()).copy(
                    klagePart = FormVilkår.IKKE_OPPFYLT,
                    brevtekst = "Test",
                ),
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
                oppfyltForm(behandlingId = UUID.randomUUID()).copy(
                    klagePart = FormVilkår.IKKE_OPPFYLT,
                    brevtekst = "Test",
                ),
            )
        }
    }
}
