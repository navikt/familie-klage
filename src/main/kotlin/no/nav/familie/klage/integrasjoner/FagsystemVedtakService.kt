package no.nav.familie.klage.integrasjoner

import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import no.nav.familie.kontrakter.felles.klage.IkkeOpprettet
import no.nav.familie.kontrakter.felles.klage.IkkeOpprettetÅrsak
import no.nav.familie.kontrakter.felles.klage.OpprettRevurderingResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

private val ukjentFeilVedOpprettRevurdering = OpprettRevurderingResponse(
    IkkeOpprettet(
        IkkeOpprettetÅrsak.FEIL,
        "Ukjent feil ved opprettelse av revurdering"
    )
)

@Service
class FagsystemVedtakService(
    private val familieEFSakClient: FamilieEFSakClient,
    private val fagsakService: FagsakService
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")

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

    fun opprettRevurdering(behandlingId: UUID): OpprettRevurderingResponse {
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        return when (fagsak.fagsystem) {
            Fagsystem.EF -> try {
                familieEFSakClient.opprettRevurdering(fagsak.eksternId)
            } catch (e: Exception) {
                val errorSuffix = "Feilet opprettelse av revurdering for behandling=$behandlingId eksternFagsakId=${fagsak.eksternId}"
                logger.warn("$errorSuffix, se detaljer i secureLogs")
                secureLogger.warn(errorSuffix, e)

                ukjentFeilVedOpprettRevurdering
            }
            else -> throw Feil("Ikke implementert opprette revurdering for BA og KS fagsak=${fagsak.id}")
        }
    }
}
