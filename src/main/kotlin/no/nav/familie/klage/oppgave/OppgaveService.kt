package no.nav.familie.klage.oppgave

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.domain.erUnderArbeidAvSaksbehandler
import no.nav.familie.klage.infrastruktur.config.getValue
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.oppgave.MappeDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
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

    fun finnMapper(enheter: List<String>): List<MappeDto> =
        enheter.flatMap { enhet -> finnMapperFraCache(enhet = enhet) }

    fun finnMapperFraCache(enhet: String): List<MappeDto> =
        cacheManager.getValue(cache = "oppgave-mappe", key = enhet) {
            // TODO: Legg til logger greier med håndtering for sukksess og feil.

            val mappeRespons = oppgaveClient.finnMapper(
                enhetnummer = enhet,
                limit = 1000
            )

            if (mappeRespons.antallTreffTotalt > mappeRespons.mapper.size) {
                // TODO: Legg til logger greier med håndtering for sukksess og feil.
            }
            mappeRespons.mapper
        }
}
