package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.Klagebehandlingsesultat
import no.nav.familie.klage.behandling.dto.BehandlingDto
import no.nav.familie.klage.behandling.dto.tilDto
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.formkrav.FormService
import no.nav.familie.klage.kabal.KlageresultatRepository
import no.nav.familie.klage.kabal.domain.tilDto
import no.nav.familie.klage.kabal.dto.KlageresultatDto
import no.nav.familie.klage.repository.findByIdOrThrow
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class BehandlingService(
    private val behandlingRepository: BehandlingRepository,
    private val klageresultatRepository: KlageresultatRepository,
    private val formService: FormService,
    private val fagsakService: FagsakService
) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun hentBehandling(behandlingId: UUID): Behandling = behandlingRepository.findByIdOrThrow(behandlingId)

    fun hentBehandlingDto(behandlingId: UUID): BehandlingDto {
        val stønadstype = fagsakService.hentFagsakForBehandling(behandlingId).stønadstype
        return behandlingRepository.findByIdOrThrow(behandlingId).tilDto(stønadstype)
    }

    fun hentNavnFraBehandlingsId(behandlingId: UUID): String {
        return "Navn Navnesen"
    }

    @Transactional
    fun opprettBehandling(
        opprettKlagebehandlingRequest: OpprettKlagebehandlingRequest
    ): UUID {
        val fagsak = fagsakService.hentEllerOpprettFagsak(
            ident = opprettKlagebehandlingRequest.ident,
            eksternId = opprettKlagebehandlingRequest.eksternFagsakId,
            fagsystem = opprettKlagebehandlingRequest.fagsystem,
            stønadstype = opprettKlagebehandlingRequest.stønadstype
        )

        val behandlingId = behandlingRepository.insert(
            Behandling(
                fagsakId = fagsak.id,
                eksternFagsystemBehandlingId = opprettKlagebehandlingRequest.eksternBehandlingId,
                klageMottatt = opprettKlagebehandlingRequest.klageMottatt,
                behandlendeEnhet = "4489" // TODO: Må inn i request
            )
        ).id

        return formService.opprettInitielleFormkrav(behandlingId, fagsak.id).behandlingId
    }

    fun hentKlageresultatDto(behandlingId: UUID): List<KlageresultatDto> {
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
}
