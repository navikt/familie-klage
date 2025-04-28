package no.nav.familie.klage.oppgave

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.domain.erUnderArbeidAvSaksbehandler
import no.nav.familie.klage.behandling.enhet.Enhet
import no.nav.familie.klage.infrastruktur.config.getValue
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.oppgave.MappeDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.StatusEnum
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class OppgaveService(
    private val behandleSakOppgaveRepository: BehandleSakOppgaveRepository,
    private val oppgaveClient: OppgaveClient,
    private val behandlingService: BehandlingService,
    private val cacheManager: CacheManager,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun hentOppgave(gsakOppgaveId: Long): Oppgave = oppgaveClient.finnOppgaveMedId(gsakOppgaveId)

    fun oppdaterOppgave(oppgave: Oppgave) = oppgaveClient.oppdaterOppgave(oppgave)

    fun oppdaterOppgaveTilÅGjeldeTilbakekreving(behandlingId: UUID) {
        val behandling = behandlingService.hentBehandling(behandlingId)

        // Skal ikke oppdatere tema for oppgaver som alt er ferdigstilt
        if (!behandling.status.erUnderArbeidAvSaksbehandler()) return

        val eksisterendeOppgave = behandleSakOppgaveRepository.findByBehandlingId(behandlingId)
            ?: error("Fant ikke oppgave for behandling $behandlingId")
        val oppdatertOppgave =
            Oppgave(id = eksisterendeOppgave.oppgaveId, behandlingstema = Behandlingstema.Tilbakebetaling.value)

        oppgaveClient.oppdaterOppgave(oppdatertOppgave)
    }

    fun fordelOppgave(
        gsakOppgaveId: Long,
        saksbehandler: String,
        versjon: Int? = null,
    ): Long {
        val oppgave = hentOppgave(gsakOppgaveId)

        return if (oppgave.tilordnetRessurs == saksbehandler) {
            gsakOppgaveId
        } else {
            oppgaveClient.fordelOppgave(
                oppgaveId = gsakOppgaveId,
                saksbehandler = saksbehandler,
                versjon = versjon,
            )
        }
    }

    fun finnMapper(enheter: List<String>): List<MappeDto> {
        val mapper = enheter.flatMap { enhet -> finnMapperFraCache(enhet = enhet) }
        return mapper.sortedBy { mappe -> mappe.navn }
    }

    private fun finnMapperFraCache(enhet: String): List<MappeDto> =
        cacheManager.getValue(cache = MAPPE_CACHE_NAVN, key = enhet) {
            logger.info("Henter mapper på nytt")

            val mappeRespons = oppgaveClient.finnMapper(
                enhetnummer = enhet,
                limit = 1000,
            )

            if (mappeRespons.antallTreffTotalt > mappeRespons.mapper.size) {
                logger.error(
                    "Det finnes flere mapper (${mappeRespons.antallTreffTotalt}) " +
                        "enn vi har hentet ut (${mappeRespons.mapper.size}). Sjekk limit. ",
                )
            }

            mappeRespons.mapper
        }

    fun oppdaterEnhetPåBehandleSakOppgave(behandlingId: UUID, behandlendeEnhet: Enhet) {
        val behandleSakOppgave = behandleSakOppgaveRepository.findByBehandlingId(behandlingId)?.let { hentOppgave(it.oppgaveId) }

        if (behandleSakOppgave == null) {
            throw Feil("Finner ingen BehandleSak-Oppgave tilknyttet behandling $behandlingId")
        }

        if (behandleSakOppgave.status !in listOf(StatusEnum.FERDIGSTILT, StatusEnum.FEILREGISTRERT)) {
            oppdaterOppgave(
                Oppgave(
                    id = behandleSakOppgave.id,
                    tildeltEnhetsnr = behandlendeEnhet.enhetsnummer,
                    mappeId = null,
                    tilordnetRessurs = null,
                ),
            )
        }
    }

    companion object {
        const val MAPPE_CACHE_NAVN = "oppgave-mappe"
    }
}
