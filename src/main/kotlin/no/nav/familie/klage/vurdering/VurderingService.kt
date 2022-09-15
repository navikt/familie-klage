package no.nav.familie.klage.vurdering

import VurderingDto
import no.nav.familie.klage.behandling.StegService
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.repository.findByIdOrThrow
import no.nav.familie.klage.vurdering.domain.Vedtak
import no.nav.familie.klage.vurdering.domain.Vurdering
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tilDto
import java.util.UUID

@Service
class VurderingService(
    private val vurderingRepository: VurderingRepository,
    private val stegService: StegService
) {

    fun hentVurdering(behandlingId: UUID): Vurdering =
        vurderingRepository.findByIdOrNull(behandlingId) ?: error("Kan ikke finne vurdering for behandling=$behandlingId")

    fun hentVurderingDto(behandlingId: UUID): VurderingDto? =
        vurderingRepository.findByIdOrNull(behandlingId)?.tilDto()

    fun hentVedtak(id: UUID): Vedtak? {
        return vurderingRepository.findVedtakByBehandlingIdOrThrow(id)
    }

    @Transactional
    fun opprettEllerOppdaterVurdering(vurdering: Vurdering): Vurdering {
        stegService.oppdaterSteg(vurdering.behandlingId, StegType.BREV)

        if (sjekkOmVurderingEksisterer(vurdering.behandlingId)) {
            return oppdaterVurdering(vurdering)
        }
        return vurderingRepository.insert(
            Vurdering(
                behandlingId = vurdering.behandlingId,
                vedtak = vurdering.vedtak,
                arsak = vurdering.arsak,
                hjemmel = vurdering.hjemmel,
                beskrivelse = vurdering.beskrivelse
            )
        )
    }

    fun oppdaterVurdering(vurdering: Vurdering): Vurdering {
        val vurderingFraDb = vurderingRepository.findByBehandlingId(vurdering.behandlingId)
        return vurderingRepository.update(
            vurderingFraDb.copy(
                vedtak = vurdering.vedtak,
                beskrivelse = vurdering.beskrivelse,
                arsak = vurdering.arsak,
                hjemmel = vurdering.hjemmel
            )
        )
    }

    fun sjekkOmVurderingEksisterer(id: UUID): Boolean {
        return vurderingRepository.findById(id).isPresent
    }

    fun lagTomVurdering(behandlingId: UUID): Vurdering {
        return Vurdering(
            behandlingId = behandlingId,
            vedtak = Vedtak.VELG,
            arsak = null,
            hjemmel = null,
            beskrivelse = ""
        )
    }

    fun klageTasIkkeTilFølge(behandlingId: UUID): Boolean {
        val vurdering = vurderingRepository.findByIdOrThrow(behandlingId)
        return (vurdering.vedtak == Vedtak.OPPRETTHOLD_VEDTAK)
    }
}
