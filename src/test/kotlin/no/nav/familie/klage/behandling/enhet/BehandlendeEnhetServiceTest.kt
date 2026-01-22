package no.nav.familie.klage.behandling.enhet

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.klage.behandlingshistorikk.domain.HistorikkHendelse
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.klage.oppgave.OppgaveService
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BehandlendeEnhetServiceTest {
    private val behandlingService: BehandlingService = mockk()
    private val fagsakService: FagsakService = mockk()
    private val behandlingshistorikkService: BehandlingshistorikkService = mockk()
    private val oppgaveService: OppgaveService = mockk()
    private val taskService: TaskService = mockk()

    private val behandlendeEnhetService =
        BehandlendeEnhetService(
            behandlingService = behandlingService,
            fagsakService = fagsakService,
            behandlingshistorikkService = behandlingshistorikkService,
            oppgaveService = oppgaveService,
            taskService = taskService,
        )

    @BeforeEach
    fun setUp() {
        mockkObject(SikkerhetContext)
        every { taskService.save(any()) } returnsArgument 0
        every { SikkerhetContext.hentSaksbehandler(true) } returns "saksbehandler1"
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SikkerhetContext)
    }

    @Nested
    inner class OppdaterBehandlendeEnhetPåBehandling {
        @ParameterizedTest
        @EnumSource(BarnetrygdEnhet::class, names = ["MIDLERTIDIG_ENHET"], mode = EnumSource.Mode.EXCLUDE)
        fun `skal oppdatere behandlende enhet på behandling og oppgave samt opprette historikkinnslag for BA`(
            nyBehandlendeEnhet: BarnetrygdEnhet,
        ) {
            // Arrange
            val fagsak = fagsak(stønadstype = Stønadstype.BARNETRYGD)
            val behandlendeEnhetPåBehandling = if (nyBehandlendeEnhet == BarnetrygdEnhet.DRAMMEN) BarnetrygdEnhet.OSLO else BarnetrygdEnhet.DRAMMEN
            val behandling = behandling(fagsak = fagsak, behandlendeEnhet = behandlendeEnhetPåBehandling.enhetsnummer)
            val begrunnelse = "Behandlende enhet var registrert på feil enhet"
            val behandlendeEnhetSlot = slot<Enhet>()
            val historikkHendelseSlot = slot<HistorikkHendelse>()
            val beskrivelseSlot = slot<String>()
            val behandlendeEnhetOppgaveSlot = slot<Enhet>()
            val taskSlot = slot<Task>()

            every { behandlingService.hentBehandling(behandling.id) } returns behandling
            every { fagsakService.hentFagsak(fagsak.id) } returns fagsak

            every {
                behandlingService.oppdaterBehandlendeEnhet(
                    behandlingId = behandling.id,
                    behandlendeEnhet = capture(behandlendeEnhetSlot),
                    fagsystem = Fagsystem.BA,
                )
            } just Runs

            every {
                behandlingshistorikkService.opprettBehandlingshistorikk(
                    behandlingId = behandling.id,
                    steg = behandling.steg,
                    historikkHendelse = capture(historikkHendelseSlot),
                    beskrivelse = capture(beskrivelseSlot),
                )
            } returns mockk()

            every {
                oppgaveService.oppdaterEnhetPåBehandleSakOppgave(
                    fagsystem = Fagsystem.BA,
                    behandlingId = behandling.id,
                    enhet = capture(behandlendeEnhetOppgaveSlot),
                )
            } just Runs

            every { taskService.save(capture(taskSlot)) } returnsArgument 0

            // Act
            behandlendeEnhetService.oppdaterBehandlendeEnhetPåBehandling(
                behandlingId = behandling.id,
                enhetsnummer = nyBehandlendeEnhet.enhetsnummer,
                begrunnelse = begrunnelse,
            )

            // Assert
            verify(exactly = 1) { behandlingService.oppdaterBehandlendeEnhet(any(), any(), any()) }
            verify(exactly = 1) { behandlingshistorikkService.opprettBehandlingshistorikk(any(), any(), any(), any()) }
            verify(exactly = 1) { oppgaveService.oppdaterEnhetPåBehandleSakOppgave(any(), any(), any()) }
            verify(exactly = 1) { taskService.save(any()) }

            assertThat(behandlendeEnhetSlot.captured).isEqualTo(nyBehandlendeEnhet)
            assertThat(historikkHendelseSlot.captured).isEqualTo(HistorikkHendelse.BEHANDLENDE_ENHET_ENDRET)
            assertThat(beskrivelseSlot.captured).contains(begrunnelse)
            assertThat(behandlendeEnhetOppgaveSlot.captured).isEqualTo(nyBehandlendeEnhet)
            assertThat(taskSlot.captured.metadata["behandlingId"]).isEqualTo(behandling.id.toString())
            assertThat(taskSlot.captured.metadata["eksternFagsakId"]).isEqualTo(fagsak.eksternId)
        }

        @ParameterizedTest
        @EnumSource(KontantstøtteEnhet::class, names = ["MIDLERTIDIG_ENHET"], mode = EnumSource.Mode.EXCLUDE)
        fun `skal oppdatere behandlende enhet på behandling og oppgave samt opprette historikkinnslag for KS`(
            nyBehandlendeEnhet: KontantstøtteEnhet,
        ) {
            // Arrange
            val fagsak = fagsak(stønadstype = Stønadstype.KONTANTSTØTTE)
            val behandlendeEnhetPåBehandling = if (nyBehandlendeEnhet == KontantstøtteEnhet.DRAMMEN) KontantstøtteEnhet.OSLO else KontantstøtteEnhet.DRAMMEN
            val behandling = behandling(fagsak = fagsak, behandlendeEnhet = behandlendeEnhetPåBehandling.enhetsnummer)
            val begrunnelse = "Behandlende enhet var registrert på feil enhet"
            val behandlendeEnhetSlot = slot<Enhet>()
            val historikkHendelseSlot = slot<HistorikkHendelse>()
            val beskrivelseSlot = slot<String>()
            val behandlendeEnhetOppgaveSlot = slot<Enhet>()
            val taskSlot = slot<Task>()

            every { behandlingService.hentBehandling(behandling.id) } returns behandling
            every { fagsakService.hentFagsak(fagsak.id) } returns fagsak

            every {
                behandlingService.oppdaterBehandlendeEnhet(
                    behandlingId = behandling.id,
                    behandlendeEnhet = capture(behandlendeEnhetSlot),
                    fagsystem = Fagsystem.KS,
                )
            } just Runs

            every {
                behandlingshistorikkService.opprettBehandlingshistorikk(
                    behandlingId = behandling.id,
                    steg = behandling.steg,
                    historikkHendelse = capture(historikkHendelseSlot),
                    beskrivelse = capture(beskrivelseSlot),
                )
            } returns mockk()

            every {
                oppgaveService.oppdaterEnhetPåBehandleSakOppgave(
                    fagsystem = Fagsystem.KS,
                    behandlingId = behandling.id,
                    enhet = capture(behandlendeEnhetOppgaveSlot),
                )
            } just Runs

            every { taskService.save(capture(taskSlot)) } returnsArgument 0

            // Act
            behandlendeEnhetService.oppdaterBehandlendeEnhetPåBehandling(
                behandlingId = behandling.id,
                enhetsnummer = nyBehandlendeEnhet.enhetsnummer,
                begrunnelse = begrunnelse,
            )

            // Assert
            verify(exactly = 1) { behandlingService.oppdaterBehandlendeEnhet(any(), any(), any()) }
            verify(exactly = 1) { behandlingshistorikkService.opprettBehandlingshistorikk(any(), any(), any(), any()) }
            verify(exactly = 1) { oppgaveService.oppdaterEnhetPåBehandleSakOppgave(any(), any(), any()) }
            verify(exactly = 1) { taskService.save(any()) }

            assertThat(behandlendeEnhetSlot.captured).isEqualTo(nyBehandlendeEnhet)
            assertThat(historikkHendelseSlot.captured).isEqualTo(HistorikkHendelse.BEHANDLENDE_ENHET_ENDRET)
            assertThat(beskrivelseSlot.captured).contains(begrunnelse)
            assertThat(behandlendeEnhetOppgaveSlot.captured).isEqualTo(nyBehandlendeEnhet)
            assertThat(taskSlot.captured.metadata["behandlingId"]).isEqualTo(behandling.id.toString())
            assertThat(taskSlot.captured.metadata["eksternFagsakId"]).isEqualTo(fagsak.eksternId)
        }

        @ParameterizedTest
        @EnumSource(
            Stønadstype::class,
            names = ["OVERGANGSSTØNAD", "BARNETILSYN", "SKOLEPENGER"],
            mode = EnumSource.Mode.INCLUDE,
        )
        fun `skal kaste feil dersom fagsystem er EF`(stønadstype: Stønadstype) {
            // Arrange
            val fagsak = fagsak(stønadstype = stønadstype)
            val behandling = behandling(fagsak = fagsak)

            every { behandlingService.hentBehandling(behandling.id) } returns behandling
            every { fagsakService.hentFagsak(fagsak.id) } returns fagsak

            // Act & Assert
            val feil =
                assertThrows<Feil> {
                    behandlendeEnhetService.oppdaterBehandlendeEnhetPåBehandling(
                        behandlingId = behandling.id,
                        enhetsnummer = "1234",
                        begrunnelse = "Behandlende enhet var registrert på feil enhet",
                    )
                }

            assertThat(feil.message).isEqualTo("Fagsystem ${Fagsystem.EF.name} er foreløpig ikke støttet.")
        }

        @Test
        fun `skal ikke oppdatere enhet på behandlingen hvis den allerede er satt`() {
            // Arrange
            val fagsak = fagsak(stønadstype = Stønadstype.BARNETRYGD)
            val behandling = behandling(fagsak = fagsak, behandlendeEnhet = BarnetrygdEnhet.OSLO.enhetsnummer)
            val begrunnelse = "Behandlende enhet var registrert på feil enhet"

            every { behandlingService.hentBehandling(behandling.id) } returns behandling
            every { fagsakService.hentFagsak(fagsak.id) } returns fagsak

            // Act
            behandlendeEnhetService.oppdaterBehandlendeEnhetPåBehandling(
                behandlingId = behandling.id,
                enhetsnummer = BarnetrygdEnhet.OSLO.enhetsnummer,
                begrunnelse = begrunnelse,
            )

            // Assert
            verify(exactly = 0) { behandlingService.oppdaterBehandlendeEnhet(any(), any(), any()) }
            verify(exactly = 0) { behandlingshistorikkService.opprettBehandlingshistorikk(any(), any(), any(), any()) }
            verify(exactly = 0) { oppgaveService.oppdaterEnhetPåBehandleSakOppgave(any(), any(), any()) }
            verify(exactly = 0) { taskService.save(any()) }
        }
    }
}
