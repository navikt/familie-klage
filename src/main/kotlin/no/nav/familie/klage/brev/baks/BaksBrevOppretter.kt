package no.nav.familie.klage.brev.baks

import jakarta.transaction.Transactional
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandling.domain.erLåstForVidereBehandling
import no.nav.familie.klage.brev.BrevClient
import no.nav.familie.klage.brev.BrevsignaturService
import no.nav.familie.klage.brev.FamilieDokumentClient
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class BaksBrevOppretter(
    private val brevClient: BrevClient,
    private val baksBrevRepository: BaksBrevRepository,
    private val behandlingService: BehandlingService,
    private val familieDokumentClient: FamilieDokumentClient,
    private val brevsignaturService: BrevsignaturService,
    private val fagsakService: FagsakService,
    private val personopplysningerService: PersonopplysningerService,
    private val fritekstBrevRequestDtoUtleder: FritekstBrevRequestDtoUtleder,
) {
    private val logger = LoggerFactory.getLogger(BaksBrevOppretter::class.java)

    @Transactional
    fun opprettBrev(behandlingId: UUID): ByteArray {
        logger.debug("Oppretter brev for behandling {}", behandlingId)

        val behandling = behandlingService.hentBehandling(behandlingId)
        validerRedigerbarBehandling(behandling)
        validerKorrektBehandlingssteg(behandling)

        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
        val personopplysninger = personopplysningerService.hentPersonopplysninger(behandlingId)

        val fritekstBrevRequestDto = fritekstBrevRequestDtoUtleder.utled(
            fagsak,
            behandling,
            personopplysninger.navn,
        )

        val signaturDto = brevsignaturService.lagSignatur(
            personopplysninger,
            fagsak.fagsystem,
        )

        val fritekstbrevHtml = brevClient.genererHtmlFritekstbrev(
            fritekstBrev = fritekstBrevRequestDto,
            saksbehandlerNavn = signaturDto.navn,
            enhet = signaturDto.enhet,
        )

        lagreEllerOppdaterBrev(
            behandlingId = behandlingId,
            html = fritekstbrevHtml,
        )

        return familieDokumentClient.genererPdfFraHtml(fritekstbrevHtml)
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
    ): BaksBrev {
        val brev = baksBrevRepository.findByIdOrNull(behandlingId)
        if (brev != null) {
            val oppdatertBaksBrev = brev.copy(html = html)
            return baksBrevRepository.update(oppdatertBaksBrev)
        }
        val nyttBaksBrev = BaksBrev(behandlingId = behandlingId, html = html)
        return baksBrevRepository.insert(nyttBaksBrev)
    }
}
