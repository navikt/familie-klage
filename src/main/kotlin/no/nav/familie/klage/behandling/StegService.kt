package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandling.domain.erLåstForVidereBehandling
import no.nav.familie.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.klage.felles.domain.BehandlerRolle
import no.nav.familie.klage.infrastruktur.config.RolleConfig
import no.nav.familie.klage.infrastruktur.exception.feilHvis
import no.nav.familie.klage.infrastruktur.exception.feilHvisIkke
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.klage.repository.findByIdOrThrow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class StegService(
    private val behandlingRepository: BehandlingRepository,
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val rolleConfig: RolleConfig
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun oppdaterSteg(behandlingId: UUID, nesteSteg: StegType) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)

        validerHarSaksbehandlerRolle()
        validerGyldigNesteSteg(behandling)

        if (nesteSteg != behandling.steg) {
            oppdaterBehandlingOgHistorikk(behandling, nesteSteg)
        } else {
            logger.info("Behandling=$behandlingId er allerede i steg=$nesteSteg")
        }
    }

    private fun oppdaterBehandlingOgHistorikk(
        behandling: Behandling,
        nesteSteg: StegType
    ) {
        val behandlingId = behandling.id
        behandlingRepository.updateSteg(behandlingId, nesteSteg)
        behandlingRepository.updateStatus(behandlingId, nesteSteg.gjelderStatus)

        behandlingshistorikkService.opprettBehandlingshistorikk(behandlingId, behandling.steg)

        if (nesteSteg == StegType.KABAL_VENTER_SVAR) {
            behandlingshistorikkService.opprettBehandlingshistorikk(behandlingId, StegType.OVERFØRING_TIL_KABAL)
        } else if (nesteSteg == StegType.BEHANDLING_FERDIGSTILT) {
            behandlingshistorikkService.opprettBehandlingshistorikk(behandlingId, StegType.BEHANDLING_FERDIGSTILT)
        }
    }

    private fun validerGyldigNesteSteg(behandling: Behandling) =
        feilHvis(behandling.status.erLåstForVidereBehandling()) {
            "Behandlingen er låst for videre behandling"
        }

    private fun validerHarSaksbehandlerRolle() =
        feilHvisIkke(
            SikkerhetContext.harTilgangTilGittRolle(rolleConfig, BehandlerRolle.SAKSBEHANDLER)
        ) { "Saksbehandler har ikke tilgang til å oppdatere behandlingssteg" }
}
