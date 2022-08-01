package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.klage.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.familie.klage.brev.BrevsignaturService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class StegService(
    private val behandlingsRepository: BehandlingsRepository,
    private val brevsignaturService: BrevsignaturService,
    private val behandlingshistorikkService: BehandlingshistorikkService
) {
    @Transactional
    fun oppdaterSteg(behandlingId: UUID, steg: StegType, stegFremover: Boolean){
        if (stegFremover) behandlingsRepository.updateSteg(behandlingId, steg.hentNesteSteg())
        else behandlingsRepository.updateSteg(behandlingId, steg)

        val signatur = brevsignaturService.lagSignatur(behandlingId)
        behandlingshistorikkService.opprettBehandlingshistorikk(
            behandlingshistorikk = Behandlingshistorikk(
                behandlingId = behandlingId,
                steg = steg,
                opprettetAv = signatur.navn
            )
        )
    }
}