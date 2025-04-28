package no.nav.familie.klage.behandling.enhet

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.dto.OppdaterBehandlendeEnhetRequest
import no.nav.familie.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.klage.behandlingshistorikk.domain.HistorikkHendelse
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.oppgave.OppgaveService
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BehandlendeEnhetServiceTest {
    private val behandlingService: BehandlingService = mockk()
    private val fagsakService: FagsakService = mockk()
    private val behandlingshistorikkService: BehandlingshistorikkService = mockk()
    private val oppgaveService: OppgaveService = mockk()
    private val behandlendeEnhetService =
        BehandlendeEnhetService(
            behandlingService = behandlingService,
            fagsakService = fagsakService,
            behandlingshistorikkService = behandlingshistorikkService,
            oppgaveService = oppgaveService,
        )

    @Nested
    inner class OppdaterBehandlendeEnhet {
        @ParameterizedTest
        @EnumSource(BarnetrygdEnhet::class, names = ["MIDLERTIDIG_ENHET"], mode = EnumSource.Mode.EXCLUDE)
        fun `skal oppdatere behandlende enhet på behandling og oppgave samt opprette historikkinnslag for BA`(
            nyBehandlendeEnhet: BarnetrygdEnhet,
        ) {
            // Arrange
            val fagsak = fagsak(stønadstype = Stønadstype.BARNETRYGD)
            val behandling = behandling(fagsak = fagsak, behandlendeEnhet = BarnetrygdEnhet.DRAMMEN.enhetsnummer)
            val begrunnelse = "Behandlende enhet var registrert på feil enhet"
            val oppdaterBehandlendeEnhetRequest =
                OppdaterBehandlendeEnhetRequest(
                    enhetsnummer = nyBehandlendeEnhet.enhetsnummer,
                    begrunnelse = begrunnelse,
                )
            val behandlendeEnhetSlot = slot<Enhet>()
            val historikkHendelseSlot = slot<HistorikkHendelse>()
            val beskrivelseSlot = slot<String>()
            val behandlendeEnhetOppgaveSlot = slot<Enhet>()

            every { behandlingService.hentBehandling(behandling.id) } returns behandling
            every { fagsakService.hentFagsak(fagsak.id) } returns fagsak
            every { behandlingService.oppdaterBehandlendeEnhet(behandling.id, capture(behandlendeEnhetSlot), Fagsystem.BA) } just Runs
            every {
                behandlingshistorikkService.opprettBehandlingshistorikk(
                    behandlingId = behandling.id,
                    steg = behandling.steg,
                    historikkHendelse = capture(historikkHendelseSlot),
                    beskrivelse = capture(beskrivelseSlot),
                )
            } returns mockk()

            every { oppgaveService.oppdaterEnhetPåBehandleSakOppgave(behandling.id, capture(behandlendeEnhetOppgaveSlot)) } just Runs

            // Act
            behandlendeEnhetService.oppdaterBehandlendeEnhet(behandling.id, oppdaterBehandlendeEnhetRequest)

            // Assert
            verify(exactly = 1) { behandlingService.oppdaterBehandlendeEnhet(any(), any(), any()) }
            verify(exactly = 1) { behandlingshistorikkService.opprettBehandlingshistorikk(any(), any(), any(), any()) }
            verify(exactly = 1) { oppgaveService.oppdaterEnhetPåBehandleSakOppgave(any(), any()) }

            assertThat(behandlendeEnhetSlot.captured).isEqualTo(nyBehandlendeEnhet)
            assertThat(historikkHendelseSlot.captured).isEqualTo(HistorikkHendelse.BEHANDLENDE_ENHET_ENDRET)
            assertThat(beskrivelseSlot.captured).contains(begrunnelse)
            assertThat(behandlendeEnhetOppgaveSlot.captured).isEqualTo(nyBehandlendeEnhet)
        }

        @ParameterizedTest
        @EnumSource(KontantstøtteEnhet::class, names = ["MIDLERTIDIG_ENHET"], mode = EnumSource.Mode.EXCLUDE)
        fun `skal oppdatere behandlende enhet på behandling og oppgave samt opprette historikkinnslag for KS`(
            nyBehandlendeEnhet: KontantstøtteEnhet,
        ) {
            // Arrange
            val fagsak = fagsak(stønadstype = Stønadstype.KONTANTSTØTTE)
            val behandling = behandling(fagsak = fagsak, behandlendeEnhet = KontantstøtteEnhet.DRAMMEN.enhetsnummer)
            val begrunnelse = "Behandlende enhet var registrert på feil enhet"
            val oppdaterBehandlendeEnhetRequest =
                OppdaterBehandlendeEnhetRequest(
                    enhetsnummer = nyBehandlendeEnhet.enhetsnummer,
                    begrunnelse = begrunnelse,
                )
            val behandlendeEnhetSlot = slot<Enhet>()
            val historikkHendelseSlot = slot<HistorikkHendelse>()
            val beskrivelseSlot = slot<String>()
            val behandlendeEnhetOppgaveSlot = slot<Enhet>()

            every { behandlingService.hentBehandling(behandling.id) } returns behandling
            every { fagsakService.hentFagsak(fagsak.id) } returns fagsak
            every { behandlingService.oppdaterBehandlendeEnhet(behandling.id, capture(behandlendeEnhetSlot), Fagsystem.KS) } just Runs
            every {
                behandlingshistorikkService.opprettBehandlingshistorikk(
                    behandlingId = behandling.id,
                    steg = behandling.steg,
                    historikkHendelse = capture(historikkHendelseSlot),
                    beskrivelse = capture(beskrivelseSlot),
                )
            } returns mockk()

            every { oppgaveService.oppdaterEnhetPåBehandleSakOppgave(behandling.id, capture(behandlendeEnhetOppgaveSlot)) } just Runs

            // Act
            behandlendeEnhetService.oppdaterBehandlendeEnhet(behandling.id, oppdaterBehandlendeEnhetRequest)

            // Assert
            verify(exactly = 1) { behandlingService.oppdaterBehandlendeEnhet(any(), any(), any()) }
            verify(exactly = 1) { behandlingshistorikkService.opprettBehandlingshistorikk(any(), any(), any(), any()) }
            verify(exactly = 1) { oppgaveService.oppdaterEnhetPåBehandleSakOppgave(any(), any()) }

            assertThat(behandlendeEnhetSlot.captured).isEqualTo(nyBehandlendeEnhet)
            assertThat(historikkHendelseSlot.captured).isEqualTo(HistorikkHendelse.BEHANDLENDE_ENHET_ENDRET)
            assertThat(beskrivelseSlot.captured).contains(begrunnelse)
            assertThat(behandlendeEnhetOppgaveSlot.captured).isEqualTo(nyBehandlendeEnhet)
        }

        @ParameterizedTest
        @EnumSource(Stønadstype::class, names = ["OVERGANGSSTØNAD", "BARNETILSYN", "SKOLEPENGER"], mode = EnumSource.Mode.INCLUDE)
        fun `skal kaste feil dersom fagsystem er EF`(stønadstype: Stønadstype) {
            // Arrange
            val fagsak = fagsak(stønadstype = stønadstype)
            val behandling = behandling(fagsak = fagsak)
            val oppdaterBehandlendeEnhetRequest =
                OppdaterBehandlendeEnhetRequest(
                    enhetsnummer = "1234",
                    begrunnelse = "Behandlende enhet var registrert på feil enhet",
                )

            every { behandlingService.hentBehandling(behandling.id) } returns behandling
            every { fagsakService.hentFagsak(fagsak.id) } returns fagsak

            // Act & Assert
            val feil =
                assertThrows<Feil> { behandlendeEnhetService.oppdaterBehandlendeEnhet(behandling.id, oppdaterBehandlendeEnhetRequest) }

            assertThat(feil.message).isEqualTo("Støtter ikke endring av enhet for fagsystem EF")
        }
    }
}
