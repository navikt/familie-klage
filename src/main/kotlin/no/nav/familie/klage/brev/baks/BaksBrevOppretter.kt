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
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class BaksBrevOppretter(
    private val baksBrevRepository: BaksBrevRepository,
    private val behandlingService: BehandlingService,
    private val familieDokumentClient: FamilieDokumentClient,
    private val fritekstbrevHtmlUtleder: FritekstbrevHtmlUtleder,
) {
    private val logger = LoggerFactory.getLogger(BaksBrevOppretter::class.java)

    @Transactional
    fun opprettBrev(behandlingId: UUID): BaksBrev {
        logger.debug("Oppretter brev for behandling {}", behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)
        validerRedigerbarBehandling(behandling)
        validerKorrektBehandlingssteg(behandling)
        val fritekstbrevHtml = fritekstbrevHtmlUtleder.utledFritekstbrevHtml(behandling)
        val pdfFraHtml = familieDokumentClient.genererPdfFraHtml(fritekstbrevHtml)
        return lagreEllerOppdaterBrev(behandlingId, fritekstbrevHtml, pdfFraHtml)
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

    private fun lagreEllerOppdaterBrev(
        behandlingId: UUID,
        html: String,
        pdf: ByteArray,
    ): BaksBrev {
        val brev = baksBrevRepository.findByIdOrNull(behandlingId)
        if (brev != null) {
            val oppdatertBaksBrev = brev.copy(html = html, pdf = Fil(pdf))
            return baksBrevRepository.update(oppdatertBaksBrev)
        }
        val nyttBaksBrev = BaksBrev(behandlingId = behandlingId, html = html, pdf = Fil(pdf))
        return baksBrevRepository.insert(nyttBaksBrev)
    }
}
