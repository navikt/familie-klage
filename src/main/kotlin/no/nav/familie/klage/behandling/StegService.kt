package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.klage.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.familie.klage.felles.domain.BehandlerRolle
import no.nav.familie.klage.infrastruktur.config.RolleConfig
import no.nav.familie.klage.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.klage.infrastruktur.exception.feilHvis
import no.nav.familie.klage.infrastruktur.exception.feilHvisIkke
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.klage.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class StegService(
    private val behandlingsRepository: BehandlingsRepository,
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val rolleConfig: RolleConfig,
) {

    @Transactional
    fun oppdaterSteg(behandlingId: UUID, nesteSteg: StegType) {
        val behandling = behandlingsRepository.findByIdOrThrow(behandlingId)

        validerHarSaksbehandlerRolle()
        validerGyldigNesteSteg(behandling)

        behandlingsRepository.updateSteg(behandlingId, nesteSteg)
        behandlingsRepository.updateStatus(behandlingId, nesteSteg.gjelderStatus)

        behandlingshistorikkService.opprettBehandlingshistorikk(
            behandlingshistorikk = Behandlingshistorikk(
                behandlingId = behandlingId,
                steg = behandling.steg,
                opprettetAv = SikkerhetContext.hentSaksbehandler()
            )
        )
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
