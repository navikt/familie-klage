package no.nav.familie.klage.brev.avvistbrev

import no.nav.familie.klage.formkrav.domain.FormVilkår
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import no.nav.familie.klage.testutil.DomainUtil.oppfyltForm
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class EFAvvistBrevUtlederTest {
    private val efAvvistBrevUtleder = EFAvvistBrevInnholdUtleder()
    private val fagsak = fagsak()
    private val folketrygdLovPrefix = "Vedtaket er gjort etter folketrygdloven"
    private val forvaltningslovPrefix = "Vedtaket er gjort etter forvaltningsloven"

    @Nested
    inner class UtledBrevInnhold {
        @Test
        internal fun `skal kaste feil dersom alle formkrav er oppfylt`() {
            // Arrange
            val oppfyltForm = oppfyltForm(UUID.randomUUID())

            // Act & Assert
            val feil =
                assertThrows<Feil> {
                    efAvvistBrevUtleder.utledBrevInnhold(
                        fagsak = fagsak,
                        form = oppfyltForm,
                    )
                }
            assertThat(feil.message).isEqualTo("Kan ikke utlede brevinnhold for avvist brev dersom alle formkrav er oppfylt")
        }

        @Test
        internal fun `skal utlede riktig brevinnhold dersom kun KLAGE_PART ikke er oppfylt`() {
            // Arrange
            val saksbehandlerBrevtekst = "Saksbehandler brevtekst"
            val klagePartIkkeOppfyltForm =
                oppfyltForm(
                    UUID.randomUUID(),
                ).copy(klagePart = FormVilkår.IKKE_OPPFYLT, brevtekst = saksbehandlerBrevtekst)

            // Act
            val brevInnhold = efAvvistBrevUtleder.utledBrevInnhold(fagsak, klagePartIkkeOppfyltForm)

            // Assert
            assertThat(brevInnhold.årsakTilAvvisning).contains(Formkrav.KLAGE_PART.hentTekst(false))
            assertThat(brevInnhold.lovtekst).contains("28")
            assertThat(brevInnhold.lovtekst).contains("33")
            assertThat(brevInnhold.lovtekst).doesNotContain("31")
            assertThat(brevInnhold.lovtekst).doesNotContain("32")
            assertThat(brevInnhold.lovtekst).doesNotContain("21-12")
            assertThat(brevInnhold.brevtekstFraSaksbehandler).isEqualTo(saksbehandlerBrevtekst)
        }

        @Test
        internal fun `skal utlede riktig brevinnhold dersom kun KLAGE_KONKRET ikke er oppfylt`() {
            // Arrange
            val saksbehandlerBrevtekst = "Saksbehandler brevtekst"
            val klagePartIkkeOppfyltForm =
                oppfyltForm(
                    UUID.randomUUID(),
                ).copy(klageKonkret = FormVilkår.IKKE_OPPFYLT, brevtekst = saksbehandlerBrevtekst)

            // Act
            val brevInnhold = efAvvistBrevUtleder.utledBrevInnhold(fagsak, klagePartIkkeOppfyltForm)

            // Assert
            assertThat(brevInnhold.årsakTilAvvisning).contains(Formkrav.KLAGE_KONKRET.hentTekst(false))
            assertThat(brevInnhold.lovtekst).contains(forvaltningslovPrefix)
            assertThat(brevInnhold.lovtekst).contains("32")
            assertThat(brevInnhold.lovtekst).contains("33")
            assertThat(brevInnhold.lovtekst).doesNotContain("28")
            assertThat(brevInnhold.lovtekst).doesNotContain("31")
            assertThat(brevInnhold.lovtekst).doesNotContain("21-12")
            assertThat(brevInnhold.brevtekstFraSaksbehandler).isEqualTo(saksbehandlerBrevtekst)
        }

        @Test
        internal fun `skal utlede riktig brevinnhold dersom kun KLAGE_SIGNERT ikke er oppfylt`() {
            // Arrange
            val saksbehandlerBrevtekst = "Saksbehandler brevtekst"
            val klagePartIkkeOppfyltForm =
                oppfyltForm(
                    UUID.randomUUID(),
                ).copy(klageSignert = FormVilkår.IKKE_OPPFYLT, brevtekst = saksbehandlerBrevtekst)

            // Act
            val brevInnhold = efAvvistBrevUtleder.utledBrevInnhold(fagsak, klagePartIkkeOppfyltForm)

            // Assert
            assertThat(brevInnhold.årsakTilAvvisning).contains(Formkrav.KLAGE_SIGNERT.hentTekst(false))
            assertThat(brevInnhold.lovtekst).contains(forvaltningslovPrefix)
            assertThat(brevInnhold.lovtekst).contains("31")
            assertThat(brevInnhold.lovtekst).contains("33")
            assertThat(brevInnhold.lovtekst).doesNotContain("28")
            assertThat(brevInnhold.lovtekst).doesNotContain("32")
            assertThat(brevInnhold.lovtekst).doesNotContain("21-12")
            assertThat(brevInnhold.brevtekstFraSaksbehandler).isEqualTo(saksbehandlerBrevtekst)
        }

        @Test
        internal fun `skal utlede riktig brevinnhold dersom kun KLAGEFRIST_OVERHOLDT ikke er oppfylt`() {
            // Arrange
            val saksbehandlerBrevtekst = "Saksbehandler brevtekst"
            val klagePartIkkeOppfyltForm =
                oppfyltForm(
                    UUID.randomUUID(),
                ).copy(klagefristOverholdt = FormVilkår.IKKE_OPPFYLT, brevtekst = saksbehandlerBrevtekst)

            // Act
            val brevInnhold = efAvvistBrevUtleder.utledBrevInnhold(fagsak, klagePartIkkeOppfyltForm)

            // Assert
            assertThat(brevInnhold.årsakTilAvvisning).contains(Formkrav.KLAGEFRIST_OVERHOLDT.hentTekst(false))
            assertThat(brevInnhold.lovtekst).contains(folketrygdLovPrefix)
            assertThat(brevInnhold.lovtekst).contains("og forvaltningsloven")
            assertThat(brevInnhold.lovtekst).contains("21-12")
            assertThat(brevInnhold.lovtekst).contains("31")
            assertThat(brevInnhold.lovtekst).contains("33")
            assertThat(brevInnhold.lovtekst).doesNotContain("28")
            assertThat(brevInnhold.lovtekst).doesNotContain("32")
            assertThat(brevInnhold.brevtekstFraSaksbehandler).isEqualTo(saksbehandlerBrevtekst)
        }
    }

    @Test
    internal fun `skal utlede riktig lovtekst dersom flere formkrav ikke er oppfylt`() {
        // Arrange
        val saksbehandlerBrevtekst = "Saksbehandler brevtekst"
        val klagePartIkkeOppfyltForm =
            oppfyltForm(
                UUID.randomUUID(),
            ).copy(
                klagePart = FormVilkår.IKKE_OPPFYLT,
                klageKonkret = FormVilkår.IKKE_OPPFYLT,
                klageSignert = FormVilkår.IKKE_OPPFYLT,
                klagefristOverholdt = FormVilkår.IKKE_OPPFYLT,
                brevtekst = saksbehandlerBrevtekst,
            )

        // Act
        val brevInnhold = efAvvistBrevUtleder.utledBrevInnhold(fagsak, klagePartIkkeOppfyltForm)

        // Assert
        listOf(Formkrav.KLAGE_PART, Formkrav.KLAGE_KONKRET, Formkrav.KLAGE_SIGNERT, Formkrav.KLAGEFRIST_OVERHOLDT).forEach {
            assertThat(brevInnhold.årsakTilAvvisning).contains(it.hentTekst(false))
        }
        assertThat(brevInnhold.lovtekst).contains(folketrygdLovPrefix)
        assertThat(brevInnhold.lovtekst).contains("og forvaltningsloven")
        assertThat(brevInnhold.lovtekst).contains("28")
        assertThat(brevInnhold.lovtekst).contains("31")
        assertThat(brevInnhold.lovtekst).contains("32")
        assertThat(brevInnhold.lovtekst).contains("33")
        assertThat(brevInnhold.lovtekst).contains("21-12")
        assertThat(brevInnhold.brevtekstFraSaksbehandler).isEqualTo(saksbehandlerBrevtekst)
    }
}
