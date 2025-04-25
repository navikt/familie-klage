package no.nav.familie.klage.kabal.domain

import no.nav.familie.klage.brevmottaker.domain.BrevmottakerOrganisasjon
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonMedIdent
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonUtenIdent
import no.nav.familie.klage.brevmottaker.domain.Brevmottakere
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle
import no.nav.familie.klage.fagsak.domain.tilYtelse
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import no.nav.familie.klage.testutil.DomainUtil.vurdering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class OversendtKlageAnkeTest {
    private val fagsak = fagsak()
    private val behandling = behandling()
    private val vurdering = vurdering(behandling.id)

    private val bruker =
        BrevmottakerPersonMedIdent(
            personIdent = "12345678910",
            navn = "Bruker Brukersen",
            mottakerRolle = MottakerRolle.BRUKER,
        )

    private val fullmektigUtenIdent =
        BrevmottakerPersonUtenIdent(
            id = UUID.randomUUID(),
            navn = "Fullmektig Fullmektigsen",
            mottakerRolle = MottakerRolle.FULLMAKT,
            adresselinje1 = "Adresselinje 1",
            adresselinje2 = "Adresselinje 2",
            postnummer = "1234",
            poststed = "Poststed",
            landkode = "NO",
        )

    private val fullmektigMedIdent =
        BrevmottakerPersonMedIdent(
            personIdent = "12345678910",
            navn = "Fullmektig Fullmektigsen",
            mottakerRolle = MottakerRolle.FULLMAKT,
        )

    private val verge =
        BrevmottakerPersonMedIdent(
            personIdent = "12345678910",
            navn = "Verge Vergesen",
            mottakerRolle = MottakerRolle.VERGE,
        )

    private val organisasjon =
        BrevmottakerOrganisasjon(
            organisasjonsnummer = "123456789",
            organisasjonsnavn = "Organ Isasjon",
            navnHosOrganisasjon = "Bruker Brukersen",
        )

    @Nested
    inner class OversendtKlageAnkeFellesTest {
        @Test
        fun `utledFullmektigEllerVerge skal returnere null hvis det ikke er en fullmektig`() {
            // Arrange
            val brevmottakere = Brevmottakere(personer = listOf(bruker))

            // Act
            val fullmektig = utledFullmektigEllerVerge(brevmottakere)

            // Assert
            assertThat(fullmektig).isNull()
        }

        @Test
        fun `skal velge organisasjon som fullmektig`() {
            // Arrange
            val brevmottakere =
                Brevmottakere(
                    personer = listOf(bruker),
                    organisasjoner = listOf(organisasjon),
                )

            // Act
            val fullmektig = utledFullmektigEllerVerge(brevmottakere)

            // Assert
            assertThat(fullmektig).isEqualTo(organisasjon)
        }

        @Test
        fun `skal returnere verge før organisasjon`() {
            val brevmottakere =
                Brevmottakere(
                    personer = listOf(bruker, verge),
                    organisasjoner = listOf(organisasjon),
                )

            val fullmektig = utledFullmektigEllerVerge(brevmottakere)

            assertThat(fullmektig).isEqualTo(verge)
        }

        @Test
        fun `skal returnere fullmektig før verge og organisasjon`() {
            val brevmottakere =
                Brevmottakere(
                    personer = listOf(bruker, verge, fullmektigMedIdent),
                    organisasjoner = listOf(organisasjon),
                )

            val fullmektig = utledFullmektigEllerVerge(brevmottakere)

            assertThat(fullmektig).isEqualTo(fullmektig)
        }
    }

    @Nested
    inner class OversendtKlageAnkeV3Test {
        @Test
        fun `skal lage klageoversendelse`() {
            // Arrange
            val brevmottakere =
                Brevmottakere(
                    personer = listOf(bruker, fullmektigMedIdent),
                    organisasjoner = listOf(organisasjon),
                )

            // Act
            val oversendtKlageAnke = lagKlageOversendelse(brevmottakere = brevmottakere)

            // Assert
            assertThat(oversendtKlageAnke.type).isEqualTo(Type.KLAGE)
            assertThat(oversendtKlageAnke.klager.id.type).isEqualTo(OversendtPartIdType.PERSON)
            assertThat(oversendtKlageAnke.klager.id.verdi).isEqualTo(fagsak.hentAktivIdent())
            assertThat(
                oversendtKlageAnke.klager.klagersProsessfullmektig
                    ?.id
                    ?.type,
            ).isEqualTo(OversendtPartIdType.PERSON)
            assertThat(
                oversendtKlageAnke.klager.klagersProsessfullmektig
                    ?.id
                    ?.verdi,
            ).isEqualTo(fullmektigMedIdent.personIdent)
            assertThat(oversendtKlageAnke.fagsak?.fagsakId).isEqualTo(fagsak.eksternId)
            assertThat(oversendtKlageAnke.fagsak?.fagsystem).isEqualTo(fagsak.fagsystem.tilFellesFagsystem())
            assertThat(oversendtKlageAnke.kildeReferanse).isEqualTo(behandling.eksternBehandlingId.toString())
            assertThat(oversendtKlageAnke.hjemler).containsExactly(vurdering.hjemmel?.kabalHjemmel)
            assertThat(oversendtKlageAnke.forrigeBehandlendeEnhet).isEqualTo("1234")
            assertThat(oversendtKlageAnke.brukersHenvendelseMottattNavDato).isEqualTo(behandling.klageMottatt)
            assertThat(oversendtKlageAnke.innsendtTilNav).isEqualTo(behandling.klageMottatt)
            assertThat(oversendtKlageAnke.kilde).isEqualTo(fagsak.fagsystem.tilFellesFagsystem())
            assertThat(oversendtKlageAnke.ytelse).isEqualTo(fagsak.stønadstype.tilYtelse())
            assertThat(oversendtKlageAnke.hindreAutomatiskSvarbrev).isFalse()
        }

        @Test
        fun `skal kaste feil dersom en brevmottaker ikke har ident`() {
            // Arrange
            val brevmottakere =
                Brevmottakere(
                    personer = listOf(bruker, fullmektigUtenIdent),
                    organisasjoner = listOf(organisasjon),
                )
            // Act
            val exception =
                assertThrows<IllegalStateException> {
                    lagKlageOversendelse(brevmottakere = brevmottakere)
                }

            // Assert
            assertThat(exception.message).isEqualTo("Person uten ident er ikke støttet i V3")
        }

        private fun lagKlageOversendelse(brevmottakere: Brevmottakere): OversendtKlageAnkeV3 =
            OversendtKlageAnkeV3.lagKlageOversendelse(
                fagsak = fagsak,
                behandling = behandling,
                vurdering = vurdering,
                saksbehandlersEnhet = "1234",
                brevmottakere = brevmottakere,
                innsynUrl = "innsynUrl",
            )
    }

    @Nested
    inner class OversendtKlageAnkeV4Test {
        @Test
        fun `skal lage klageoversendelse`() {
            // Arrange
            val brevmottakere =
                Brevmottakere(
                    personer = listOf(bruker, fullmektigUtenIdent),
                    organisasjoner = listOf(organisasjon),
                )

            // Act
            val oversendtKlageAnke = lagKlageOversendelse(brevmottakere = brevmottakere)

            // Assert
            assertThat(oversendtKlageAnke.type).isEqualTo(OversendtType.KLAGE)
            assertThat(oversendtKlageAnke.sakenGjelder.id.type).isEqualTo(OversendtPartIdType.PERSON)
            assertThat(oversendtKlageAnke.sakenGjelder.id.verdi).isEqualTo(fagsak.hentAktivIdent())
            assertThat(oversendtKlageAnke.prosessfullmektig?.id).isNull()
            assertThat(oversendtKlageAnke.prosessfullmektig?.navn).isEqualTo(fullmektigUtenIdent.navn)
            assertThat(oversendtKlageAnke.prosessfullmektig?.adresse?.adresselinje1).isEqualTo(fullmektigUtenIdent.adresselinje1)
            assertThat(oversendtKlageAnke.prosessfullmektig?.adresse?.adresselinje2).isEqualTo(fullmektigUtenIdent.adresselinje2)
            assertThat(oversendtKlageAnke.prosessfullmektig?.adresse?.postnummer).isEqualTo(fullmektigUtenIdent.postnummer)
            assertThat(oversendtKlageAnke.prosessfullmektig?.adresse?.poststed).isEqualTo(fullmektigUtenIdent.poststed)
            assertThat(oversendtKlageAnke.prosessfullmektig?.adresse?.land).isEqualTo(fullmektigUtenIdent.landkode)
            assertThat(oversendtKlageAnke.fagsak.fagsakId).isEqualTo(fagsak.eksternId)
            assertThat(oversendtKlageAnke.fagsak.fagsystem).isEqualTo(fagsak.fagsystem.tilFellesFagsystem())
            assertThat(oversendtKlageAnke.kildeReferanse).isEqualTo(behandling.eksternBehandlingId.toString())
            assertThat(oversendtKlageAnke.hjemler).containsExactly(vurdering.hjemmel?.kabalHjemmel)
            assertThat(oversendtKlageAnke.forrigeBehandlendeEnhet).isEqualTo("1234")
            assertThat(oversendtKlageAnke.brukersKlageMottattVedtaksinstans).isEqualTo(behandling.klageMottatt)
            assertThat(oversendtKlageAnke.ytelse).isEqualTo(fagsak.stønadstype.tilYtelse())
            assertThat(oversendtKlageAnke.hindreAutomatiskSvarbrev).isFalse()
        }

        private fun lagKlageOversendelse(brevmottakere: Brevmottakere): OversendtKlageAnkeV4 =
            OversendtKlageAnkeV4.lagKlageOversendelse(
                fagsak = fagsak,
                behandling = behandling,
                vurdering = vurdering,
                saksbehandlersEnhet = "1234",
                brevmottakere = brevmottakere,
            )
    }
}
