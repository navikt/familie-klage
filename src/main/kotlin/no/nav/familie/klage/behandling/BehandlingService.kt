package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.Klagebehandlingsesultat
import no.nav.familie.klage.behandling.dto.BehandlingDto
import no.nav.familie.klage.behandling.dto.tilDto
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.repository.findByIdOrThrow
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BehandlingService(
    private val behandlingRepository: BehandlingRepository,
    private val fagsakService: FagsakService,

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
            opprettKlagebehandlingRequest.ident,
            opprettKlagebehandlingRequest.eksternFagsakId,
            opprettKlagebehandlingRequest.fagsystem,
            opprettKlagebehandlingRequest.stønadstype
        )

        return behandlingRepository.insert(
            Behandling(
                fagsakId = fagsak.id,
                eksternBehandlingId = opprettKlagebehandlingRequest.eksternBehandlingId,
                klageMottatt = opprettKlagebehandlingRequest.klageMottatt,
                behandlendeEnhet = "4489" // TODO: Må inn i request
            )
        ).id
    }

    fun hentBehandlinger(eksternFagsakId: String, fagsystem: Fagsystem): List<Klagebehandlingsesultat> {
        return behandlingRepository.finnKlagebehandlingsresultat(eksternFagsakId, fagsystem)
    }

    fun hentAktivIdent(behandlingId: UUID): String {
        val behandling = hentBehandling(behandlingId)
        return fagsakService.hentFagsak(behandling.fagsakId).hentAktivIdent()
    }
}
