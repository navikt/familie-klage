package no.nav.familie.klage.brev.baks

import jakarta.transaction.Transactional
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandling.domain.erLåstForVidereBehandling
import no.nav.familie.klage.brev.FamilieDokumentClient
import no.nav.familie.klage.felles.domain.Fil
import no.nav.familie.klage.infrastruktur.exception.Feil
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class BaksBrevOppdaterer(
    private val baksBrevRepository: BaksBrevRepository,
    private val behandlingService: BehandlingService,
    private val familieDokumentClient: FamilieDokumentClient,
    private val fritekstbrevHtmlUtleder: FritekstbrevHtmlUtleder,
) {
    private val logger = LoggerFactory.getLogger(BaksBrevOppdaterer::class.java)

    @Transactional
    fun oppdaterBrev(baksBrev: BaksBrev): BaksBrev {
        logger.debug("Oppdaterer brev for behandling {}", baksBrev.behandlingId)
        val behandling = behandlingService.hentBehandling(baksBrev.behandlingId)
        validerRedigerbarBehandling(behandling)
        validerKorrektBehandlingssteg(behandling)
        validerEksisterendeBrev(behandling)
        val fritekstbrevHtml = fritekstbrevHtmlUtleder.utledFritekstbrevHtml(behandling)
        val pdfFraHtml = familieDokumentClient.genererPdfFraHtml(fritekstbrevHtml)
        val oppdatertBaksBrev = baksBrev.copy(html = fritekstbrevHtml, pdf = Fil(pdfFraHtml))
        return baksBrevRepository.update(oppdatertBaksBrev)
    }

    private fun validerRedigerbarBehandling(behandling: Behandling) {
        if (behandling.status.erLåstForVidereBehandling()) {
            throw Feil("Behandlingen ${behandling.id} er låst for videre behandling")
        }
    }

    private fun validerKorrektBehandlingssteg(behandling: Behandling) {
        if (behandling.steg != StegType.BREV) {
            throw Feil("Behandlingen er i steg ${behandling.steg}, forventet steg ${StegType.BREV}")
        }
    }

    private fun validerEksisterendeBrev(behandling: Behandling) {
        val brevFinnes = baksBrevRepository.existsByBehandlingId(behandling.id)
        if (!brevFinnes) {
            throw Feil("Brev finnes ikke for behandling ${behandling.id}. Opprett brevet først.")
        }
    }
}
