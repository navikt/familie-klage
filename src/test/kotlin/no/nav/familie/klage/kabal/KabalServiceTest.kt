package no.nav.familie.klage.kabal

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.klage.behandling.domain.PåklagetVedtak
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype
import no.nav.familie.klage.brev.ef.domain.BrevmottakerOrganisasjon
import no.nav.familie.klage.brev.ef.domain.BrevmottakerPerson
import no.nav.familie.klage.brev.ef.domain.Brevmottakere
import no.nav.familie.klage.brev.ef.domain.MottakerRolle
import no.nav.familie.klage.fagsak.domain.PersonIdent
import no.nav.familie.klage.infrastruktur.config.LenkeConfig
import no.nav.familie.klage.integrasjoner.FamilieIntegrasjonerClient
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsakDomain
import no.nav.familie.klage.testutil.DomainUtil.påklagetVedtakDetaljer
import no.nav.familie.klage.testutil.DomainUtil.vurdering
import no.nav.familie.klage.vurdering.domain.Hjemmel
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.klage.FagsystemType
import no.nav.familie.kontrakter.felles.klage.Klagebehandlingsårsak
import no.nav.familie.kontrakter.felles.saksbehandler.Saksbehandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

internal class KabalServiceTest {
    val kabalClient = mockk<KabalClient>()
    val integrasjonerClient = mockk<FamilieIntegrasjonerClient>()
    val lenkeConfig = LenkeConfig(efSakLenke = "BASEURL_EF", baSakLenke = "BASEURL_BA", ksSakLenke = "BASEURL_KS")
    val kabalService = KabalService(kabalClient, integrasjonerClient, lenkeConfig)
    val fagsak = fagsakDomain().tilFagsakMedPerson(setOf(PersonIdent("1")))
    val ingenBrevmottaker = Brevmottakere()

    val hjemmel = Hjemmel.FT_FEMTEN_FIRE

    val oversendelseSlot = slot<OversendtKlageAnkeV3>()
    val saksbehandlerA = Saksbehandler(UUID.randomUUID(), "A123456", "Alfa", "Surname", "4415")
    val saksbehandlerB = Saksbehandler(UUID.randomUUID(), "B987654", "Beta", "Etternavn", "4408")

    @BeforeEach
    internal fun setUp() {
        every { kabalClient.sendTilKabal(capture(oversendelseSlot)) } just Runs
        every { integrasjonerClient.hentSaksbehandlerInfo(any()) } answers {
            when (firstArg<String>()) {
                saksbehandlerA.navIdent -> saksbehandlerA
                saksbehandlerB.navIdent -> saksbehandlerB
                else -> error("Fant ikke info om saksbehanlder ${firstArg<String>()}")
            }
        }
    }

    @Test
    fun sendTilKabal() {
        val påklagetVedtakDetaljer = påklagetVedtakDetaljer()
        val behandling =
            behandling(fagsak, påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.VEDTAK, påklagetVedtakDetaljer))
        val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)

        kabalService.sendTilKabal(fagsak, behandling, vurdering, saksbehandlerA.navIdent, ingenBrevmottaker)

        val oversendelse = oversendelseSlot.captured
        assertThat(oversendelse.fagsak?.fagsakId).isEqualTo(fagsak.eksternId)
        assertThat(oversendelse.fagsak?.fagsystem).isEqualTo(Fagsystem.EF)
        assertThat(oversendelse.hjemler).containsAll(listOf(hjemmel.kabalHjemmel))
        assertThat(oversendelse.kildeReferanse).isEqualTo(behandling.eksternBehandlingId.toString())
        assertThat(oversendelse.innsynUrl)
            .isEqualTo("${lenkeConfig.efSakLenke}/fagsak/${fagsak.eksternId}/${påklagetVedtakDetaljer.eksternFagsystemBehandlingId}")
        assertThat(oversendelse.tilknyttedeJournalposter).isEmpty()
        assertThat(oversendelse.brukersHenvendelseMottattNavDato).isEqualTo(behandling.klageMottatt)
        assertThat(oversendelse.innsendtTilNav).isEqualTo(behandling.klageMottatt)
        assertThat(oversendelse.klager.id.verdi).isEqualTo(fagsak.hentAktivIdent())
        assertThat(oversendelse.sakenGjelder).isNull()
        assertThat(oversendelse.kilde).isEqualTo(Fagsystem.EF)
        assertThat(oversendelse.ytelse).isEqualTo(Ytelse.ENF_ENF)
        assertThat(oversendelse.kommentar).isNull()
        assertThat(oversendelse.dvhReferanse).isNull()
        assertThat(oversendelse.forrigeBehandlendeEnhet).isEqualTo(saksbehandlerA.enhet)
        assertThat(oversendelse.hindreAutomatiskSvarbrev).isFalse()
    }

    @Test
    internal fun `skal sette innsynUrl til saksoversikten hvis påklaget vedtakstype gjelder tilbakekreving`() {
        val påklagetVedtakDetaljer = påklagetVedtakDetaljer(fagsystemType = FagsystemType.TILBAKEKREVING)
        val behandling =
            behandling(fagsak, påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.VEDTAK, påklagetVedtakDetaljer))
        val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)

        kabalService.sendTilKabal(fagsak, behandling, vurdering, saksbehandlerB.navIdent, ingenBrevmottaker)

        assertThat(oversendelseSlot.captured.innsynUrl)
            .isEqualTo("${lenkeConfig.efSakLenke}/fagsak/${fagsak.eksternId}/saksoversikt")
        assertThat(oversendelseSlot.captured.forrigeBehandlendeEnhet).isEqualTo(saksbehandlerB.enhet)
    }

    @Test
    internal fun `skal sette hindreAutomatiskSvarbrev til true dersom årsaken til behandlingen er henvendelse fra kabal`() {
        val påklagetVedtakDetaljer = påklagetVedtakDetaljer()
        val behandling =
            behandling(
                fagsak,
                påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.VEDTAK, påklagetVedtakDetaljer),
                årsak = Klagebehandlingsårsak.HENVENDELSE_FRA_KABAL,
            )
        val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)

        kabalService.sendTilKabal(fagsak, behandling, vurdering, saksbehandlerB.navIdent, ingenBrevmottaker)

        assertThat(oversendelseSlot.captured.hindreAutomatiskSvarbrev).isTrue()
    }

    @Test
    internal fun `skal sette innsynUrl til saksoversikten hvis påklaget vedtak ikke er satt`() {
        val behandling = behandling(fagsak, påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.UTEN_VEDTAK))
        val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)

        kabalService.sendTilKabal(fagsak, behandling, vurdering, saksbehandlerB.navIdent, ingenBrevmottaker)

        assertThat(oversendelseSlot.captured.innsynUrl)
            .isEqualTo("${lenkeConfig.efSakLenke}/fagsak/${fagsak.eksternId}/saksoversikt")
        assertThat(oversendelseSlot.captured.forrigeBehandlendeEnhet).isEqualTo(saksbehandlerB.enhet)
    }

    @Test
    internal fun `skal feile hvis saksbehandlerinfo ikke finnes`() {
        val behandling = behandling(fagsak, påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.UTEN_VEDTAK))
        val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)

        assertThrows<IllegalStateException> {
            kabalService.sendTilKabal(fagsak, behandling, vurdering, "UKJENT1234", ingenBrevmottaker)
        }
    }

    @Nested
    inner class VergeOgFullmektig {
        @Test
        fun `dersom det både er verge og bruker skal motta kopi av brevet skal verge sendes over til kabal og motta svartidsbrevet`() {
            val påklagetVedtakDetaljer = påklagetVedtakDetaljer()
            val behandling =
                behandling(fagsak, påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.VEDTAK, påklagetVedtakDetaljer))
            val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)
            val verge = BrevmottakerPerson("01234567890", "Navn", MottakerRolle.VERGE)
            val bruker = BrevmottakerPerson(fagsak.hentAktivIdent(), "Navn", MottakerRolle.BRUKER)
            kabalService.sendTilKabal(
                fagsak,
                behandling,
                vurdering,
                saksbehandlerA.navIdent,
                Brevmottakere(personer = listOf(verge, bruker)),
            )

            val oversendelse = oversendelseSlot.captured
            assertThat(
                oversendelse.klager.klagersProsessfullmektig
                    ?.id
                    ?.verdi,
            ).isEqualTo(verge.personIdent)
            assertThat(
                oversendelse.klager.klagersProsessfullmektig
                    ?.id
                    ?.type,
            ).isEqualTo(OversendtPartIdType.PERSON)
            assertThat(oversendelse.klager.klagersProsessfullmektig?.skalKlagerMottaKopi).isFalse()
        }

        @Test
        fun `skal sende med verge til kabal uten kopi til bruker`() {
            val påklagetVedtakDetaljer = påklagetVedtakDetaljer()
            val behandling =
                behandling(fagsak, påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.VEDTAK, påklagetVedtakDetaljer))
            val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)
            val verge = BrevmottakerPerson("01234567890", "Navn", MottakerRolle.VERGE)
            kabalService.sendTilKabal(
                fagsak,
                behandling,
                vurdering,
                saksbehandlerA.navIdent,
                Brevmottakere(personer = listOf(verge)),
            )

            val oversendelse = oversendelseSlot.captured
            assertThat(
                oversendelse.klager.klagersProsessfullmektig
                    ?.id
                    ?.verdi,
            ).isEqualTo(verge.personIdent)
            assertThat(
                oversendelse.klager.klagersProsessfullmektig
                    ?.id
                    ?.type,
            ).isEqualTo(OversendtPartIdType.PERSON)
            assertThat(oversendelse.klager.klagersProsessfullmektig?.skalKlagerMottaKopi).isFalse()
        }

        @Test
        fun `skal sende med fullmektig til kabal `() {
            val påklagetVedtakDetaljer = påklagetVedtakDetaljer()
            val behandling =
                behandling(fagsak, påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.VEDTAK, påklagetVedtakDetaljer))
            val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)
            val fullmektig = BrevmottakerPerson("01234567890", "Navn", MottakerRolle.FULLMAKT)
            kabalService.sendTilKabal(
                fagsak,
                behandling,
                vurdering,
                saksbehandlerA.navIdent,
                Brevmottakere(personer = listOf(fullmektig)),
            )

            val oversendelse = oversendelseSlot.captured
            assertThat(
                oversendelse.klager.klagersProsessfullmektig
                    ?.id
                    ?.verdi,
            ).isEqualTo(fullmektig.personIdent)
            assertThat(
                oversendelse.klager.klagersProsessfullmektig
                    ?.id
                    ?.type,
            ).isEqualTo(OversendtPartIdType.PERSON)
            assertThat(oversendelse.klager.klagersProsessfullmektig?.skalKlagerMottaKopi).isFalse()
        }

        @Test
        fun `skal sende med organisasjonsfullemktig til kabal `() {
            val påklagetVedtakDetaljer = påklagetVedtakDetaljer()
            val behandling =
                behandling(fagsak, påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.VEDTAK, påklagetVedtakDetaljer))
            val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)
            val fullmektig = BrevmottakerOrganisasjon("012345678", "Navn på org", "Navn på person")
            kabalService.sendTilKabal(
                fagsak,
                behandling,
                vurdering,
                saksbehandlerA.navIdent,
                Brevmottakere(organisasjoner = listOf(fullmektig)),
            )

            val oversendelse = oversendelseSlot.captured
            assertThat(
                oversendelse.klager.klagersProsessfullmektig
                    ?.id
                    ?.verdi,
            ).isEqualTo(fullmektig.organisasjonsnummer)
            assertThat(
                oversendelse.klager.klagersProsessfullmektig
                    ?.id
                    ?.type,
            ).isEqualTo(OversendtPartIdType.VIRKSOMHET)
            assertThat(oversendelse.klager.klagersProsessfullmektig?.skalKlagerMottaKopi).isFalse()
        }

        @Test
        fun `skal sende med fullmektig til kabal dersom det finnes både verge, fullmektig og organsiasjon`() {
            val påklagetVedtakDetaljer = påklagetVedtakDetaljer()
            val behandling =
                behandling(fagsak, påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.VEDTAK, påklagetVedtakDetaljer))
            val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)
            val fullmektigOrganisasjon = BrevmottakerOrganisasjon("012345678", "Navn på org", "Navn på person")
            val fullmektig = BrevmottakerPerson("01234567890", "Fullmektig", MottakerRolle.FULLMAKT)
            val verge = BrevmottakerPerson("98765432100", "Verge", MottakerRolle.VERGE)
            kabalService.sendTilKabal(
                fagsak,
                behandling,
                vurdering,
                saksbehandlerA.navIdent,
                Brevmottakere(personer = listOf(verge, fullmektig), organisasjoner = listOf(fullmektigOrganisasjon)),
            )

            val oversendelse = oversendelseSlot.captured
            assertThat(
                oversendelse.klager.klagersProsessfullmektig
                    ?.id
                    ?.verdi,
            ).isEqualTo(fullmektig.personIdent)
            assertThat(
                oversendelse.klager.klagersProsessfullmektig
                    ?.id
                    ?.type,
            ).isEqualTo(OversendtPartIdType.PERSON)
            assertThat(oversendelse.klager.klagersProsessfullmektig?.skalKlagerMottaKopi).isFalse()
        }
    }
}
