package no.nav.familie.klage.integrasjoner

import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FagsystemVedtakService(
    private val familieEFSakClient: FamilieEFSakClient,
    private val fagsakService: FagsakService
) {

    fun hentFagsystemVedtak(behandlingId: UUID): List<FagsystemVedtak> {
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        return hentFagsystemVedtak(fagsak)
    }

    private fun hentFagsystemVedtak(fagsak: Fagsak): List<FagsystemVedtak> = when (fagsak.fagsystem) {
        Fagsystem.EF -> familieEFSakClient.hentVedtak(fagsak.eksternId)
        else -> throw Feil("Ikke implementert henting av vedtak for BA og KS fagsak=${fagsak.id}")
    }

    fun hentFagsystemVedtakForPåklagetBehandlingId(
        behandlingId: UUID,
        påklagetBehandlingId: String
    ): FagsystemVedtak =
        hentFagsystemVedtak(behandlingId)
            .singleOrNull { it.eksternBehandlingId == påklagetBehandlingId }
            ?: error("Finner ikke vedtak for behandling=$behandlingId eksternBehandling=$påklagetBehandlingId")
}
