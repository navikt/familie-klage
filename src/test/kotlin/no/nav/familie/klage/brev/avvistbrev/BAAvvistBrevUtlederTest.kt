package no.nav.familie.klage.brev.avvistbrev

import no.nav.familie.klage.formkrav.domain.FormVilkår
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import no.nav.familie.klage.testutil.DomainUtil.lagInstitusjon
import no.nav.familie.klage.testutil.DomainUtil.oppfyltForm
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class BAAvvistBrevUtlederTest {
    private val baAvvistBrevUtleder = BAAvvistBrevInnholdUtleder()
    private val fagsak = fagsak()
    private val barnetrygdlovenPrefix = "Vedtaket er gjort etter barnetrygdloven"
    private val forvaltningslovPrefix = "Vedtaket er gjort etter forvaltningsloven"

    @Nested
    inner class UtledBrevInnhold {
        @Test
        internal fun `skal kaste feil dersom alle formkrav er oppfylt`() {
            // Arrange
            val oppfyltForm = oppfyltForm(UUID.randomUUID())

            // Act & Assert
            val feil =
                org.junit.jupiter.api.assertThrows<Feil> {
                    baAvvistBrevUtleder.utledBrevInnhold(
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
            val brevInnhold = baAvvistBrevUtleder.utledBrevInnhold(fagsak, klagePartIkkeOppfyltForm)

            // Assert
            assertThat(brevInnhold.årsakTilAvvisning).contains(Formkrav.KLAGE_PART.hentTekst(false))
            assertThat(brevInnhold.lovtekst).contains("28")
            assertThat(brevInnhold.lovtekst).contains("33")
            assertThat(brevInnhold.lovtekst).doesNotContain("15")
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
            val brevInnhold = baAvvistBrevUtleder.utledBrevInnhold(fagsak, klagePartIkkeOppfyltForm)

            // Assert
            assertThat(brevInnhold.årsakTilAvvisning).contains(Formkrav.KLAGE_KONKRET.hentTekst(false))
            assertThat(brevInnhold.lovtekst).contains(forvaltningslovPrefix)
            assertThat(brevInnhold.lovtekst).contains("32")
            assertThat(brevInnhold.lovtekst).contains("33")
            assertThat(brevInnhold.lovtekst).doesNotContain("15")
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
            val brevInnhold = baAvvistBrevUtleder.utledBrevInnhold(fagsak, klagePartIkkeOppfyltForm)

            // Assert
            assertThat(brevInnhold.årsakTilAvvisning).contains(Formkrav.KLAGE_SIGNERT.hentTekst(false))
            assertThat(brevInnhold.lovtekst).contains(forvaltningslovPrefix)
            assertThat(brevInnhold.lovtekst).contains("32")
            assertThat(brevInnhold.lovtekst).contains("33")
            assertThat(brevInnhold.lovtekst).doesNotContain("15")
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
            val brevInnhold = baAvvistBrevUtleder.utledBrevInnhold(fagsak, klagePartIkkeOppfyltForm)

            // Assert
            assertThat(brevInnhold.årsakTilAvvisning).contains(Formkrav.KLAGEFRIST_OVERHOLDT.hentTekst(false))
            assertThat(brevInnhold.lovtekst).contains(barnetrygdlovenPrefix)
            assertThat(brevInnhold.lovtekst).contains("og forvaltningsloven")
            assertThat(brevInnhold.lovtekst).contains("15")
            assertThat(brevInnhold.lovtekst).contains("31")
            assertThat(brevInnhold.lovtekst).contains("33")
            assertThat(brevInnhold.brevtekstFraSaksbehandler).isEqualTo(saksbehandlerBrevtekst)
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
            val brevInnhold = baAvvistBrevUtleder.utledBrevInnhold(fagsak, klagePartIkkeOppfyltForm)

            // Assert
            assertThat(brevInnhold.årsakTilAvvisning).contains("Vi har avvist klagen din fordi")
            listOf(
                Formkrav.KLAGE_PART,
                Formkrav.KLAGE_KONKRET,
                Formkrav.KLAGE_SIGNERT,
                Formkrav.KLAGEFRIST_OVERHOLDT,
            ).forEach {
                assertThat(brevInnhold.årsakTilAvvisning).contains(it.hentTekst(false))
            }
            assertThat(brevInnhold.lovtekst).contains(barnetrygdlovenPrefix)
            assertThat(brevInnhold.lovtekst).contains("og forvaltningsloven")
            assertThat(brevInnhold.lovtekst).contains("31")
            assertThat(brevInnhold.lovtekst).contains("33")
            assertThat(brevInnhold.lovtekst).contains("15")
            assertThat(brevInnhold.brevtekstFraSaksbehandler).isEqualTo(saksbehandlerBrevtekst)
        }

        @Test
        internal fun `skal utlede riktig lovtekst for institusjon`() {
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

            val fagsak = fagsak.copy(institusjon = lagInstitusjon())

            // Act
            val brevInnhold = baAvvistBrevUtleder.utledBrevInnhold(fagsak, klagePartIkkeOppfyltForm)

            // Assert
            assertThat(brevInnhold.årsakTilAvvisning).contains("Vi har avvist klagen deres fordi")
            listOf(
                Formkrav.KLAGE_PART,
                Formkrav.KLAGE_KONKRET,
                Formkrav.KLAGE_SIGNERT,
                Formkrav.KLAGEFRIST_OVERHOLDT,
            ).forEach {
                assertThat(brevInnhold.årsakTilAvvisning).contains(it.hentTekst(true))
            }
            assertThat(brevInnhold.lovtekst).contains(barnetrygdlovenPrefix)
            assertThat(brevInnhold.lovtekst).contains("og forvaltningsloven")
            assertThat(brevInnhold.lovtekst).contains("31")
            assertThat(brevInnhold.lovtekst).contains("33")
            assertThat(brevInnhold.lovtekst).contains("15")
            assertThat(brevInnhold.brevtekstFraSaksbehandler).isEqualTo(saksbehandlerBrevtekst)
        }
    }
}
