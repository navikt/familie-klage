package no.nav.familie.klage.oppgave

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.familie.klage.behandling.BehandlingRepository
import no.nav.familie.klage.behandling.domain.PåklagetVedtak
import no.nav.familie.klage.behandling.domain.PåklagetVedtakDetaljer
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.fagsak.FagsakPersonRepository
import no.nav.familie.klage.fagsak.FagsakRepository
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import no.nav.familie.klage.testutil.DomainUtil.fagsakDomain
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Regelverk
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.FagsystemType
import no.nav.familie.kontrakter.felles.klage.Klagebehandlingsårsak
import no.nav.familie.kontrakter.felles.klage.KlageinstansUtfall
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Behandlingstype
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class OpprettKabalEventOppgaveTaskTest {
    @Nested
    inner class DoTaskTest {
        private val fagsakRepository = mockk<FagsakRepository>()
        private val behandlingRepository = mockk<BehandlingRepository>()
        private val fagsakPersonRepository = mockk<FagsakPersonRepository>()
        private val oppgaveClient = mockk<OppgaveClient>()
        private val opprettKabalEventOppgaveTask = OpprettKabalEventOppgaveTask(
            fagsakRepository = fagsakRepository,
            behandlingRepository = behandlingRepository,
            personRepository = fagsakPersonRepository,
            oppgaveClient = oppgaveClient
        )

        @Test
        fun `skal kaste feil om fagsak ikke finnes for behandling`() {
            // Arrange
            val opprettOppgavePayload = OpprettOppgavePayload(
                klagebehandlingEksternId = UUID.fromString("7be8afe5-f3ff-4635-9676-7930fbd4af86"),
                oppgaveTekst = "Oppgavetekst",
                fagsystem = Fagsystem.BA,
                klageinstansUtfall = KlageinstansUtfall.STADFESTELSE,
                behandlingstema = Behandlingstema.Barnetrygd,
                behandlingstype = null,
            )

            val task = Task(
                OpprettKabalEventOppgaveTask.TYPE,
                objectMapper.writeValueAsString(opprettOppgavePayload)
            )

            val fagsak = fagsak(
                id = UUID.fromString("5d4a47f1-9d72-4bab-a86d-8f3ed269a8b7"),
                stønadstype = Stønadstype.BARNETRYGD,
                fagsakPersonId = UUID.fromString("6706f41f-5e55-4849-b07b-b79c06764e69")
            )


            val behandling = behandling(
                id = UUID.fromString("d4122035-d9c5-41d1-a424-98ccd3e5c723"),
                fagsak = fagsak,
                status = BehandlingStatus.FERDIGSTILT,
                steg = StegType.BEHANDLING_FERDIGSTILT,
                resultat = BehandlingResultat.IKKE_MEDHOLD,
                vedtakDato = LocalDateTime.of(2025, 2, 18, 13, 40, 26),
                klageMottatt = LocalDate.of(2025, 1, 1),
                behandlendeEnhet = "4812",
                eksternBehandlingId = UUID.fromString("7be8afe5-f3ff-4635-9676-7930fbd4af86"),
                henlagtÅrsak = null,
                påklagetVedtak = PåklagetVedtak(
                    påklagetVedtakstype = PåklagetVedtakstype.VEDTAK,
                    påklagetVedtakDetaljer = PåklagetVedtakDetaljer(
                        fagsystemType = FagsystemType.ORDNIÆR,
                        eksternFagsystemBehandlingId = "100224307",
                        behandlingstype = "Førstegangsbehandling",
                        resultat = "Innvilget",
                        vedtakstidspunkt = LocalDateTime.of(2025, 2, 18, 8, 40, 23),
                        regelverk = Regelverk.NASJONAL
                    )
                ),
                fagsystemRevurdering = null,
                årsak = Klagebehandlingsårsak.ORDINÆR
            )

            every { behandlingRepository.findByEksternBehandlingId(opprettOppgavePayload.klagebehandlingEksternId) } returns behandling
            every { fagsakRepository.finnFagsakForBehandlingId(behandling.id) } returns null

            // Act & assert
            val exception = assertThrows<Feil> {
                opprettKabalEventOppgaveTask.doTask(task)
            }
            assertThat(exception.message).isEqualTo("Finner ikke fagsak for behandling med ekstern id ${behandling.eksternBehandlingId}.")
        }

        @ParameterizedTest
        @EnumSource(
            value = KlageinstansUtfall::class,
            names = ["OPPHEVET"],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `skal opprette oppgave med normal prioritert`(
            utfall: KlageinstansUtfall
        ) {
            // Arrange
            val opprettOppgavePayload = OpprettOppgavePayload(
                klagebehandlingEksternId = UUID.fromString("7be8afe5-f3ff-4635-9676-7930fbd4af86"),
                oppgaveTekst = "Oppgavetekst",
                fagsystem = Fagsystem.BA,
                klageinstansUtfall = utfall,
                behandlingstema = Behandlingstema.Barnetrygd,
                behandlingstype = null,
            )

            val task = Task(
                OpprettKabalEventOppgaveTask.TYPE,
                objectMapper.writeValueAsString(opprettOppgavePayload)
            )

            val fagsak = fagsak(
                id = UUID.fromString("5d4a47f1-9d72-4bab-a86d-8f3ed269a8b7"),
                stønadstype = Stønadstype.BARNETRYGD,
                fagsakPersonId = UUID.fromString("6706f41f-5e55-4849-b07b-b79c06764e69")
            )

            val fagsakDomain = fagsakDomain(
                id = fagsak.id,
                stønadstype = fagsak.stønadstype,
                personId = fagsak.fagsakPersonId,
                fagsystem = fagsak.fagsystem,
                eksternId = fagsak.eksternId
            )

            val behandling = behandling(
                id = UUID.fromString("d4122035-d9c5-41d1-a424-98ccd3e5c723"),
                fagsak = fagsak,
                status = BehandlingStatus.FERDIGSTILT,
                steg = StegType.BEHANDLING_FERDIGSTILT,
                resultat = BehandlingResultat.IKKE_MEDHOLD,
                vedtakDato = LocalDateTime.of(2025, 2, 18, 13, 40, 26),
                klageMottatt = LocalDate.of(2025, 1, 1),
                behandlendeEnhet = "4812",
                eksternBehandlingId = UUID.fromString("7be8afe5-f3ff-4635-9676-7930fbd4af86"),
                henlagtÅrsak = null,
                påklagetVedtak = PåklagetVedtak(
                    påklagetVedtakstype = PåklagetVedtakstype.VEDTAK,
                    påklagetVedtakDetaljer = PåklagetVedtakDetaljer(
                        fagsystemType = FagsystemType.ORDNIÆR,
                        eksternFagsystemBehandlingId = "100224307",
                        behandlingstype = "Førstegangsbehandling",
                        resultat = "Innvilget",
                        vedtakstidspunkt = LocalDateTime.of(2025, 2, 18, 8, 40, 23),
                        regelverk = Regelverk.NASJONAL
                    )
                ),
                fagsystemRevurdering = null,
                årsak = Klagebehandlingsårsak.ORDINÆR
            )

            val opprettOppgaveRequestSlot = slot<OpprettOppgaveRequest>()

            every { behandlingRepository.findByEksternBehandlingId(opprettOppgavePayload.klagebehandlingEksternId) } returns behandling
            every { fagsakRepository.finnFagsakForBehandlingId(behandling.id) } returns fagsakDomain
            every { fagsakPersonRepository.hentAktivIdent(fagsakDomain.fagsakPersonId) } returns fagsakDomain.fagsakPersonId.toString()
            every { oppgaveClient.opprettOppgave(capture(opprettOppgaveRequestSlot)) } returns 1L

            // Act
            opprettKabalEventOppgaveTask.doTask(task)

            // Assert
            val captured = opprettOppgaveRequestSlot.captured
            assertThat(captured.ident?.ident).isEqualTo("6706f41f-5e55-4849-b07b-b79c06764e69")
            assertThat(captured.ident?.gruppe).isEqualTo(IdentGruppe.FOLKEREGISTERIDENT)
            assertThat(captured.enhetsnummer).isEqualTo("4812")
            assertThat(captured.saksId).isEqualTo("1")
            assertThat(captured.journalpostId).isNull()
            assertThat(captured.tema).isEqualTo(Tema.BAR)
            assertThat(captured.oppgavetype).isEqualTo(Oppgavetype.VurderKonsekvensForYtelse)
            assertThat(captured.behandlingstema).isEqualTo(Behandlingstema.Barnetrygd.value)
            assertThat(captured.tilordnetRessurs).isNull()
            assertThat(captured.fristFerdigstillelse).isNotNull()
            assertThat(captured.aktivFra).isNotNull()
            assertThat(captured.beskrivelse).isEqualTo("Oppgavetekst")
            assertThat(captured.prioritet).isEqualTo(OppgavePrioritet.NORM)
            assertThat(captured.behandlingstype).isNull()
            assertThat(captured.behandlesAvApplikasjon).isNull()
            assertThat(captured.mappeId).isNull()
        }

        @Test
        fun `skal opprette oppgave med høy prioritert`() {
            // Arrange
            val opprettOppgavePayload = OpprettOppgavePayload(
                klagebehandlingEksternId = UUID.fromString("7be8afe5-f3ff-4635-9676-7930fbd4af86"),
                oppgaveTekst = "Oppgavetekst",
                fagsystem = Fagsystem.BA,
                klageinstansUtfall = KlageinstansUtfall.OPPHEVET,
                behandlingstema = Behandlingstema.Barnetrygd,
                behandlingstype = null,
            )

            val task = Task(
                OpprettKabalEventOppgaveTask.TYPE,
                objectMapper.writeValueAsString(opprettOppgavePayload)
            )

            val fagsak = fagsak(
                id = UUID.fromString("5d4a47f1-9d72-4bab-a86d-8f3ed269a8b7"),
                stønadstype = Stønadstype.BARNETRYGD,
                fagsakPersonId = UUID.fromString("6706f41f-5e55-4849-b07b-b79c06764e69")
            )

            val fagsakDomain = fagsakDomain(
                id = fagsak.id,
                stønadstype = fagsak.stønadstype,
                personId = fagsak.fagsakPersonId,
                fagsystem = fagsak.fagsystem,
                eksternId = fagsak.eksternId
            )

            val behandling = behandling(
                id = UUID.fromString("d4122035-d9c5-41d1-a424-98ccd3e5c723"),
                fagsak = fagsak,
                status = BehandlingStatus.FERDIGSTILT,
                steg = StegType.BEHANDLING_FERDIGSTILT,
                resultat = BehandlingResultat.IKKE_MEDHOLD,
                vedtakDato = LocalDateTime.of(2025, 2, 18, 13, 40, 26),
                klageMottatt = LocalDate.of(2025, 1, 1),
                behandlendeEnhet = "4812",
                eksternBehandlingId = UUID.fromString("7be8afe5-f3ff-4635-9676-7930fbd4af86"),
                henlagtÅrsak = null,
                påklagetVedtak = PåklagetVedtak(
                    påklagetVedtakstype = PåklagetVedtakstype.VEDTAK,
                    påklagetVedtakDetaljer = PåklagetVedtakDetaljer(
                        fagsystemType = FagsystemType.ORDNIÆR,
                        eksternFagsystemBehandlingId = "100224307",
                        behandlingstype = "Førstegangsbehandling",
                        resultat = "Innvilget",
                        vedtakstidspunkt = LocalDateTime.of(2025, 2, 18, 8, 40, 23),
                        regelverk = Regelverk.NASJONAL
                    )
                ),
                fagsystemRevurdering = null,
                årsak = Klagebehandlingsårsak.ORDINÆR
            )

            val opprettOppgaveRequestSlot = slot<OpprettOppgaveRequest>()

            every { behandlingRepository.findByEksternBehandlingId(opprettOppgavePayload.klagebehandlingEksternId) } returns behandling
            every { fagsakRepository.finnFagsakForBehandlingId(behandling.id) } returns fagsakDomain
            every { fagsakPersonRepository.hentAktivIdent(fagsakDomain.fagsakPersonId) } returns fagsakDomain.fagsakPersonId.toString()
            every { oppgaveClient.opprettOppgave(capture(opprettOppgaveRequestSlot)) } returns 1L

            // Act
            opprettKabalEventOppgaveTask.doTask(task)

            // Assert
            val captured = opprettOppgaveRequestSlot.captured
            assertThat(captured.ident?.ident).isEqualTo("6706f41f-5e55-4849-b07b-b79c06764e69")
            assertThat(captured.ident?.gruppe).isEqualTo(IdentGruppe.FOLKEREGISTERIDENT)
            assertThat(captured.enhetsnummer).isEqualTo("4812")
            assertThat(captured.saksId).isEqualTo("1")
            assertThat(captured.journalpostId).isNull()
            assertThat(captured.tema).isEqualTo(Tema.BAR)
            assertThat(captured.oppgavetype).isEqualTo(Oppgavetype.VurderKonsekvensForYtelse)
            assertThat(captured.behandlingstema).isEqualTo(Behandlingstema.Barnetrygd.value)
            assertThat(captured.tilordnetRessurs).isNull()
            assertThat(captured.fristFerdigstillelse).isNotNull()
            assertThat(captured.aktivFra).isNotNull()
            assertThat(captured.beskrivelse).isEqualTo("Oppgavetekst")
            assertThat(captured.prioritet).isEqualTo(OppgavePrioritet.HOY)
            assertThat(captured.behandlingstype).isNull()
            assertThat(captured.behandlesAvApplikasjon).isNull()
            assertThat(captured.mappeId).isNull()
        }

        @Test
        fun `skal opprette oppgave uten behandlingstema`() {
            // Arrange
            val opprettOppgavePayload = OpprettOppgavePayload(
                klagebehandlingEksternId = UUID.fromString("7be8afe5-f3ff-4635-9676-7930fbd4af86"),
                oppgaveTekst = "Oppgavetekst",
                fagsystem = Fagsystem.BA,
                klageinstansUtfall = KlageinstansUtfall.OPPHEVET,
                behandlingstema = null,
                behandlingstype = Behandlingstype.NASJONAL.value,
            )

            val task = Task(
                OpprettKabalEventOppgaveTask.TYPE,
                objectMapper.writeValueAsString(opprettOppgavePayload)
            )

            val fagsak = fagsak(
                id = UUID.fromString("5d4a47f1-9d72-4bab-a86d-8f3ed269a8b7"),
                stønadstype = Stønadstype.BARNETRYGD,
                fagsakPersonId = UUID.fromString("6706f41f-5e55-4849-b07b-b79c06764e69")
            )

            val fagsakDomain = fagsakDomain(
                id = fagsak.id,
                stønadstype = fagsak.stønadstype,
                personId = fagsak.fagsakPersonId,
                fagsystem = fagsak.fagsystem,
                eksternId = fagsak.eksternId
            )

            val behandling = behandling(
                id = UUID.fromString("d4122035-d9c5-41d1-a424-98ccd3e5c723"),
                fagsak = fagsak,
                status = BehandlingStatus.FERDIGSTILT,
                steg = StegType.BEHANDLING_FERDIGSTILT,
                resultat = BehandlingResultat.IKKE_MEDHOLD,
                vedtakDato = LocalDateTime.of(2025, 2, 18, 13, 40, 26),
                klageMottatt = LocalDate.of(2025, 1, 1),
                behandlendeEnhet = "4812",
                eksternBehandlingId = UUID.fromString("7be8afe5-f3ff-4635-9676-7930fbd4af86"),
                henlagtÅrsak = null,
                påklagetVedtak = PåklagetVedtak(
                    påklagetVedtakstype = PåklagetVedtakstype.VEDTAK,
                    påklagetVedtakDetaljer = PåklagetVedtakDetaljer(
                        fagsystemType = FagsystemType.ORDNIÆR,
                        eksternFagsystemBehandlingId = "100224307",
                        behandlingstype = "Førstegangsbehandling",
                        resultat = "Innvilget",
                        vedtakstidspunkt = LocalDateTime.of(2025, 2, 18, 8, 40, 23),
                        regelverk = Regelverk.NASJONAL
                    )
                ),
                fagsystemRevurdering = null,
                årsak = Klagebehandlingsårsak.ORDINÆR
            )

            val opprettOppgaveRequestSlot = slot<OpprettOppgaveRequest>()

            every { behandlingRepository.findByEksternBehandlingId(opprettOppgavePayload.klagebehandlingEksternId) } returns behandling
            every { fagsakRepository.finnFagsakForBehandlingId(behandling.id) } returns fagsakDomain
            every { fagsakPersonRepository.hentAktivIdent(fagsakDomain.fagsakPersonId) } returns fagsakDomain.fagsakPersonId.toString()
            every { oppgaveClient.opprettOppgave(capture(opprettOppgaveRequestSlot)) } returns 1L

            // Act
            opprettKabalEventOppgaveTask.doTask(task)

            // Assert
            val captured = opprettOppgaveRequestSlot.captured
            assertThat(captured.ident?.ident).isEqualTo("6706f41f-5e55-4849-b07b-b79c06764e69")
            assertThat(captured.ident?.gruppe).isEqualTo(IdentGruppe.FOLKEREGISTERIDENT)
            assertThat(captured.enhetsnummer).isEqualTo("4812")
            assertThat(captured.saksId).isEqualTo("1")
            assertThat(captured.journalpostId).isNull()
            assertThat(captured.tema).isEqualTo(Tema.BAR)
            assertThat(captured.oppgavetype).isEqualTo(Oppgavetype.VurderKonsekvensForYtelse)
            assertThat(captured.behandlingstema).isNull()
            assertThat(captured.tilordnetRessurs).isNull()
            assertThat(captured.fristFerdigstillelse).isNotNull()
            assertThat(captured.aktivFra).isNotNull()
            assertThat(captured.beskrivelse).isEqualTo("Oppgavetekst")
            assertThat(captured.prioritet).isEqualTo(OppgavePrioritet.HOY)
            assertThat(captured.behandlingstype).isEqualTo(Behandlingstype.NASJONAL.value)
            assertThat(captured.behandlesAvApplikasjon).isNull()
            assertThat(captured.mappeId).isNull()
        }
    }

    @Nested
    inner class OpprettFraTest {
        @Test
        fun `skal opprette task`() {
            // Arrange
            val opprettOppgavePayload = OpprettOppgavePayload(
                klagebehandlingEksternId = UUID.fromString("7be8afe5-f3ff-4635-9676-7930fbd4af86"),
                oppgaveTekst = "Oppgavetekst",
                fagsystem = Fagsystem.BA,
                klageinstansUtfall = KlageinstansUtfall.OPPHEVET,
                behandlingstema = Behandlingstema.Barnetrygd,
                behandlingstype = null,
            )

            // Act
            val task = OpprettKabalEventOppgaveTask.opprettTask(opprettOppgavePayload)

            // Assert
            assertThat(task.type).isEqualTo(OpprettKabalEventOppgaveTask.TYPE)
            assertThat(task.payload).isEqualTo(objectMapper.writeValueAsString(opprettOppgavePayload))
        }
    }
}