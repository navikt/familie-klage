package no.nav.familie.klage.vurdering

import no.nav.familie.klage.behandling.StegService
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.repository.findByIdOrThrow
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
    private val stegService: StegService
) {

    fun hentVurdering(behandlingId: UUID): Vurdering? =
        vurderingRepository.findByIdOrNull(behandlingId)

    fun hentVurderingDto(behandlingId: UUID): VurderingDto? =
        hentVurdering(behandlingId)?.tilDto()

    fun hentVedtak(behandlingId: UUID): Vedtak? {
        return vurderingRepository.findByIdOrNull(behandlingId)?.vedtak
    }

    @Transactional
    fun opprettEllerOppdaterVurdering(vurdering: VurderingDto): VurderingDto {
        validerVurdering(vurdering)

        stegService.oppdaterSteg(vurdering.behandlingId, StegType.BREV)

        val eksisterendeVurdering = vurderingRepository.findByIdOrNull(vurdering.behandlingId)
        return if (eksisterendeVurdering != null) {
            oppdaterVurdering(vurdering, eksisterendeVurdering).tilDto()
        } else {
            opprettNyVurdering(vurdering).tilDto()
        }
    }

    private fun opprettNyVurdering(vurdering: VurderingDto) = vurderingRepository.insert(
        Vurdering(
            behandlingId = vurdering.behandlingId,
            vedtak = vurdering.vedtak,
            arsak = vurdering.arsak,
            hjemmel = vurdering.hjemmel,
            beskrivelse = vurdering.beskrivelse
        )
    )

    private fun oppdaterVurdering(vurdering: VurderingDto, eksisterendeVurdering: Vurdering): Vurdering {
        return vurderingRepository.update(
            eksisterendeVurdering.copy(
                vedtak = vurdering.vedtak,
                beskrivelse = vurdering.beskrivelse,
                arsak = vurdering.arsak,
                hjemmel = vurdering.hjemmel
            )
        )
    }

    fun klageTasIkkeTilFølge(behandlingId: UUID): Boolean {
        val vurdering = vurderingRepository.findByIdOrThrow(behandlingId)
        return vurdering.vedtak == Vedtak.OPPRETTHOLD_VEDTAK
    }
}
