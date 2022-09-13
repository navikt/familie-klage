package no.nav.familie.klage.kabal.event

import no.nav.familie.klage.behandling.BehandlingsRepository
import no.nav.familie.klage.fagsak.FagsakPersonRepository
import no.nav.familie.klage.fagsak.FagsakRepository
import no.nav.familie.klage.infrastruktur.config.getValue
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.integrasjoner.FamilieIntegrasjonerClient
import no.nav.familie.klage.integrasjoner.OppgaveClient
import no.nav.familie.klage.kabal.BehandlingEvent
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import no.nav.familie.kontrakter.felles.oppgave.FinnMappeRequest
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.MappeDto
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
class BehandlingEventService(
    private val oppgaveClient: OppgaveClient,
    private val familieIntegrasjonerClient: FamilieIntegrasjonerClient,
    private val behandlingsRepository: BehandlingsRepository,
    private val fagsakRepository: FagsakRepository,
    private val personRepository: FagsakPersonRepository,
    private val cacheManager: CacheManager
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun handleEvent(behandlingEvent: BehandlingEvent) {
        if (behandlingEvent.utfallErMedhold()) {
            // Opprette revurderingsoppgave med url til klagesaken
        } else {
            opprettOppgave(behandlingEvent)
            ferdigstillKlagebehandling()
        }
    }

    private fun opprettOppgave(behandlingEvent: BehandlingEvent) {

        val behandling = behandlingsRepository.findByEksternBehandlingId(UUID.fromString(behandlingEvent.kildeReferanse))
        val fagsak = fagsakRepository.finnFagsakForBehandling(behandling.id)
        val aktivIdent = personRepository.hentAktivIdent(fagsak?.fagsakPersonId ?: throw Feil("Finner ikke fagsak for behandling med eksternId ${behandlingEvent.kildeReferanse}"))
        val enhetsnummer = familieIntegrasjonerClient.hentNavEnhet(aktivIdent).enhetId
        val opprettOppgaveRequest =
            OpprettOppgaveRequest(
                ident = OppgaveIdentV2(ident = aktivIdent, gruppe = IdentGruppe.FOLKEREGISTERIDENT),
                saksId = fagsak.eksternId,
                tema = Tema.ENF,
                oppgavetype = Oppgavetype.BehandleSak,
                fristFerdigstillelse = lagFristForOppgave(LocalDateTime.now()),
                beskrivelse = behandlingEvent.lagOppgaveTekst(),
                enhetsnummer = enhetsnummer,
                behandlingstema = finnBehandlingstema(fagsak.stønadstype).value,
                tilordnetRessurs = null,
                behandlesAvApplikasjon = "familie-ef-sak",
                mappeId = finnHendelseMappeId(enhetsnummer)
            )

        oppgaveClient.opprettOppgave(opprettOppgaveRequest)
    }

    fun ferdigstillKlagebehandling() {
    }

    private fun finnHendelseMappeId(enhetsnummer: String?): Long? {
        if ((enhetsnummer == "4489")) {
            val mapper = finnMapper(enhetsnummer)
            val mappeIdForGodkjenneVedtak = mapper.find {
                it.navn.contains("62 Hendelser")
            }?.id?.toLong()
            return mappeIdForGodkjenneVedtak
        }
        return null
    }

    fun finnMapper(enhet: String): List<MappeDto> {
        return cacheManager.getValue("oppgave-mappe", enhet) {
            logger.info("Henter mapper på nytt")
            val mappeRespons = oppgaveClient.finnMapper(
                FinnMappeRequest(
                    tema = listOf(),
                    enhetsnr = enhet,
                    opprettetFom = null,
                    limit = 1000
                )
            )
            if (mappeRespons.antallTreffTotalt > mappeRespons.mapper.size) {
                logger.error(
                    "Det finnes flere mapper (${mappeRespons.antallTreffTotalt}) " +
                        "enn vi har hentet ut (${mappeRespons.mapper.size}). Sjekk limit. "
                )
            }
            mappeRespons.mapper
        }
    }

    private fun lagFristForOppgave(gjeldendeTid: LocalDateTime): LocalDate {
        val frist = when (gjeldendeTid.dayOfWeek) {
            DayOfWeek.FRIDAY -> fristBasertPåKlokkeslett(gjeldendeTid.plusDays(2))
            DayOfWeek.SATURDAY -> fristBasertPåKlokkeslett(gjeldendeTid.plusDays(2).withHour(8))
            DayOfWeek.SUNDAY -> fristBasertPåKlokkeslett(gjeldendeTid.plusDays(1).withHour(8))
            else -> fristBasertPåKlokkeslett(gjeldendeTid)
        }

        return when (frist.dayOfWeek) {
            DayOfWeek.SATURDAY -> frist.plusDays(2)
            DayOfWeek.SUNDAY -> frist.plusDays(1)
            else -> frist
        }
    }

    private fun finnBehandlingstema(stønadstype: Stønadstype): Behandlingstema {
        return when (stønadstype) {
            Stønadstype.BARNETRYGD -> Behandlingstema.Barnetrygd
            Stønadstype.OVERGANGSSTØNAD -> Behandlingstema.Overgangsstønad
            Stønadstype.BARNETILSYN -> Behandlingstema.Barnetilsyn
            Stønadstype.SKOLEPENGER -> Behandlingstema.Skolepenger
            Stønadstype.KONTANTSTØTTE -> Behandlingstema.Kontantstøtte
        }
    }

    private fun fristBasertPåKlokkeslett(gjeldendeTid: LocalDateTime): LocalDate {
        return if (gjeldendeTid.hour >= 12) {
            return gjeldendeTid.plusDays(2).toLocalDate()
        } else {
            gjeldendeTid.plusDays(1).toLocalDate()
        }
    }
}
