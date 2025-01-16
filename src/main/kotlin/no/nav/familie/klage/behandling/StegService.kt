package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandling.domain.erLåstForVidereBehandling
import no.nav.familie.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.klage.felles.domain.BehandlerRolle
import no.nav.familie.klage.infrastruktur.exception.feilHvis
import no.nav.familie.klage.infrastruktur.exception.feilHvisIkke
import no.nav.familie.klage.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.klage.repository.findByIdOrThrow
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.Klagebehandlingsårsak
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class StegService(
    private val behandlingRepository: BehandlingRepository,
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val tilgangService: TilgangService,
) {

    @Transactional
    fun oppdaterSteg(
        behandlingId: UUID,
        nåværendeSteg: StegType,
        nesteSteg: StegType,
        behandlingsresultat: BehandlingResultat? = null,
    ) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        validerHarSaksbehandlerRolle(behandlingId)
        validerGyldigNesteSteg(behandling)
        oppdaterBehandlingOgHistorikk(behandling, nåværendeSteg, nesteSteg, behandlingsresultat)
    }

    private fun oppdaterBehandlingOgHistorikk(
        behandling: Behandling,
        nåværendeSteg: StegType,
        nesteSteg: StegType,
        behandlingsresultat: BehandlingResultat? = null,
    ) {
        behandlingRepository.updateSteg(behandling.id, nesteSteg)
        behandlingRepository.updateStatus(behandling.id, nesteSteg.gjelderStatus)

        if (skalOppretteHistorikkradForNåværendeSteg(nåværendeSteg, nesteSteg, behandlingsresultat, behandling.årsak)) {
            behandlingshistorikkService.opprettBehandlingshistorikk(
                behandlingId = behandling.id,
                steg = nåværendeSteg,
                historikkHendelse = null,
            )
        }
        if (nesteSteg == StegType.KABAL_VENTER_SVAR) {
            behandlingshistorikkService.opprettBehandlingshistorikk(
                behandlingId = behandling.id,
                steg = StegType.OVERFØRING_TIL_KABAL,
                historikkHendelse = null,
            )
        }
        if (nesteSteg == StegType.BEHANDLING_FERDIGSTILT) {
            behandlingshistorikkService.opprettBehandlingshistorikk(
                behandlingId = behandling.id,
                steg = StegType.BEHANDLING_FERDIGSTILT,
                historikkHendelse = null,
            )
        }
    }

    private fun skalOppretteHistorikkradForNåværendeSteg(
        nåværendeSteg: StegType,
        nesteSteg: StegType,
        behandlingsresultat: BehandlingResultat? = null,
        behandlingsårsak: Klagebehandlingsårsak,
    ): Boolean {
        return if (nåværendeSteg == StegType.BREV) {
            when (nesteSteg) {
                StegType.BEHANDLING_FERDIGSTILT -> behandlingsresultat != BehandlingResultat.MEDHOLD
                StegType.KABAL_VENTER_SVAR -> behandlingsårsak != Klagebehandlingsårsak.HENVENDELSE_FRA_KABAL
                else -> true
            }
        } else {
            true
        }
    }

    private fun validerGyldigNesteSteg(behandling: Behandling) =
        feilHvis(behandling.status.erLåstForVidereBehandling()) {
            "Behandlingen er låst for videre behandling"
        }

    private fun validerHarSaksbehandlerRolle(behandlingId: UUID) =
        feilHvisIkke(
            tilgangService.harTilgangTilBehandlingGittRolle(behandlingId, BehandlerRolle.SAKSBEHANDLER),
        ) { "Saksbehandler har ikke tilgang til å oppdatere behandlingssteg" }
}
