package no.nav.familie.klage.integrasjoner

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import no.nav.familie.kontrakter.felles.klage.IkkeOpprettet
import no.nav.familie.kontrakter.felles.klage.IkkeOpprettetÅrsak
import no.nav.familie.kontrakter.felles.klage.KanOppretteRevurderingResponse
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
    private val familieKSSakClient: FamilieKSSakClient,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")

    fun hentFagsystemVedtak(behandlingId: UUID): List<FagsystemVedtak> {
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        return hentFagsystemVedtak(fagsak)
    }

    private fun hentFagsystemVedtak(fagsak: Fagsak): List<FagsystemVedtak> = when (fagsak.fagsystem) {
        Fagsystem.EF -> familieEFSakClient.hentVedtak(fagsak.eksternId)
        Fagsystem.KS -> familieKSSakClient.hentVedtak(fagsak.eksternId)
        else -> throw Feil("Ikke implementert henting av vedtak for BA og KS fagsak=${fagsak.id}")
    }

    fun hentFagsystemVedtakForPåklagetBehandlingId(
        behandlingId: UUID,
        påklagetBehandlingId: String
    ): FagsystemVedtak =
        hentFagsystemVedtak(behandlingId)
            .singleOrNull { it.eksternBehandlingId == påklagetBehandlingId }
            ?: error("Finner ikke vedtak for behandling=$behandlingId eksternBehandling=$påklagetBehandlingId")

    fun kanOppretteRevurdering(behandlingId: UUID): KanOppretteRevurderingResponse {
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        return when (fagsak.fagsystem) {
            Fagsystem.EF -> familieEFSakClient.kanOppretteRevurdering(fagsak.eksternId)
            Fagsystem.KS -> familieKSSakClient.kanOppretteRevurdering(fagsak.eksternId)
            else -> throw Feil("Ikke implementert sjekk for å opprette revurdering for fagsak=${fagsak.id} fagsystem=${fagsak.fagsystem}")
        }
    }

    fun opprettRevurdering(behandlingId: UUID): OpprettRevurderingResponse {
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)
        return try {
            when (fagsak.fagsystem) {
                Fagsystem.EF -> familieEFSakClient.opprettRevurdering(fagsak.eksternId)

                Fagsystem.KS -> {
                    val eksternFagsystemBehandlingId =
                        behandling.påklagetVedtak.påklagetVedtakDetaljer?.eksternFagsystemBehandlingId
                            ?: throw Feil("eksternFagsystemBehandlingId er ikke satt på påklagetVedtak for ks-behandling")

                    familieKSSakClient.opprettRevurdering(eksternFagsystemBehandlingId)
                }

                Fagsystem.BA -> throw Feil("Kan ikke opprette revurdering for BA enda")
            }
        } catch (e: Exception) {
            val errorSuffix = "Feilet opprettelse av revurdering for behandling=$behandlingId eksternFagsakId=${fagsak.eksternId}"
            logger.warn("$errorSuffix, se detaljer i secureLogs")
            secureLogger.warn(errorSuffix, e)

            ukjentFeilVedOpprettRevurdering
        }
    }
}
