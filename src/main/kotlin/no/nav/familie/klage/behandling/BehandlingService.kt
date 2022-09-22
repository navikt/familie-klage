package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.Klagebehandlingsesultat
import no.nav.familie.klage.behandling.domain.erUnderArbeidAvSaksbehandler
import no.nav.familie.klage.behandling.dto.BehandlingDto
import no.nav.familie.klage.behandling.dto.tilDto
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.klage.kabal.KlageresultatRepository
import no.nav.familie.klage.kabal.domain.tilDto
import no.nav.familie.klage.kabal.dto.KlageresultatDto
import no.nav.familie.klage.repository.findByIdOrThrow
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class BehandlingService(
    private val behandlingRepository: BehandlingRepository,
    private val fagsakService: FagsakService,
    private val klageresultatRepository: KlageresultatRepository
) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun hentBehandling(behandlingId: UUID): Behandling = behandlingRepository.findByIdOrThrow(behandlingId)

    fun hentBehandlingDto(behandlingId: UUID): BehandlingDto {
        val stønadstype = fagsakService.hentFagsakForBehandling(behandlingId).stønadstype
        return behandlingRepository.findByIdOrThrow(behandlingId)
            .tilDto(stønadstype, hentKlageresultatDto(behandlingId))
    }

    fun opprettBehandling(behandling: Behandling): Behandling {
        validerKanOppretteBehandling(behandling.fagsakId)
        return behandlingRepository.insert(behandling)
    }

    // TODO
    fun hentNavnFraBehandlingsId(behandlingId: UUID): String {
        return "Navn Navnesen"
    }

    private fun hentKlageresultatDto(behandlingId: UUID): List<KlageresultatDto> {
        val klageresultater = klageresultatRepository.findByBehandlingId(behandlingId)
        return klageresultater.tilDto()
    }

    fun finnKlagebehandlingsresultat(eksternFagsakId: String, fagsystem: Fagsystem): List<Klagebehandlingsesultat> {
        return behandlingRepository.finnKlagebehandlingsresultat(eksternFagsakId, fagsystem)
    }

    fun hentAktivIdent(behandlingId: UUID): String {
        val behandling = hentBehandling(behandlingId)
        return fagsakService.hentFagsak(behandling.fagsakId).hentAktivIdent()
    }

    fun oppdaterBehandlingsresultatOgVedtaksdato(behandlingId: UUID, behandlingsresultat: BehandlingResultat) {
        val behandling = hentBehandling(behandlingId)
        if (behandling.resultat != BehandlingResultat.IKKE_SATT) {
            error("Kan ikke endre på et resultat som allerede er satt")
        }
        val oppdatertBehandling = behandling.copy(resultat = behandlingsresultat, vedtakDato = LocalDateTime.now())
        behandlingRepository.update(oppdatertBehandling)
    }

    private fun validerKanOppretteBehandling(fagsakId: UUID) {
        val behandlinger = behandlingRepository.findByFagsakId(fagsakId)

        brukerfeilHvis(behandlinger.any { it.status.erUnderArbeidAvSaksbehandler() }) {
            "Det eksisterer allerede en klagebehandling som ikke er ferdigstilt på fagsak med id=$fagsakId"
        }
    }
}
