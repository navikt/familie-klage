package no.nav.familie.klage.vurdering

import no.nav.familie.klage.behandling.StegService
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.brev.BrevRepository
import no.nav.familie.klage.vurdering.VurderingValidator.validerVurdering
import no.nav.familie.klage.vurdering.domain.Vedtak
import no.nav.familie.klage.vurdering.domain.Vurdering
import no.nav.familie.klage.vurdering.dto.VurderingDto
import no.nav.familie.klage.vurdering.dto.tilDto
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class VurderingService(
    private val vurderingRepository: VurderingRepository,
    private val stegService: StegService,
    private val brevRepository: BrevRepository
) {

    fun hentVurdering(behandlingId: UUID): Vurdering? =
        vurderingRepository.findByIdOrNull(behandlingId)

    fun hentVurderingDto(behandlingId: UUID): VurderingDto? =
        hentVurdering(behandlingId)?.tilDto()

    @Transactional
    fun opprettEllerOppdaterVurdering(vurdering: VurderingDto): VurderingDto {
        validerVurdering(vurdering)
        if (vurdering.vedtak === Vedtak.OMGJØR_VEDTAK) {
            brevRepository.deleteById(vurdering.behandlingId)
        }

        stegService.oppdaterSteg(vurdering.behandlingId, StegType.VURDERING, StegType.BREV)

        val eksisterendeVurdering = vurderingRepository.findByIdOrNull(vurdering.behandlingId)
        return if (eksisterendeVurdering != null) {
            oppdaterVurdering(vurdering, eksisterendeVurdering).tilDto()
        } else {
            opprettNyVurdering(vurdering).tilDto()
        }
    }

    fun slettVurderingForBehandling(behandlingId: UUID) {
        vurderingRepository.deleteById(behandlingId)
    }

    private fun opprettNyVurdering(vurdering: VurderingDto) = vurderingRepository.insert(
        Vurdering(
            behandlingId = vurdering.behandlingId,
            vedtak = vurdering.vedtak,
            årsak = vurdering.årsak,
            begrunnelseOmgjøring = vurdering.begrunnelseOmgjøring,
            hjemmel = vurdering.hjemmel,
            innstillingKlageinstans = vurdering.innstillingKlageinstans,
            interntNotat = vurdering.interntNotat
        )
    )

    private fun oppdaterVurdering(vurdering: VurderingDto, eksisterendeVurdering: Vurdering): Vurdering {
        return vurderingRepository.update(
            eksisterendeVurdering.copy(
                vedtak = vurdering.vedtak,
                innstillingKlageinstans = vurdering.innstillingKlageinstans,
                årsak = vurdering.årsak,
                begrunnelseOmgjøring = vurdering.begrunnelseOmgjøring,
                hjemmel = vurdering.hjemmel,
                interntNotat = vurdering.interntNotat
            )
        )
    }
}
