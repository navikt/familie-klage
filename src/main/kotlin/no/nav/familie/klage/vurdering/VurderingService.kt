package no.nav.familie.klage.vurdering

import VurderingDto
import no.nav.familie.klage.vurdering.domain.Vedtak
import no.nav.familie.klage.vurdering.domain.Vurdering
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import tilDto
import java.util.UUID

@Service
class VurderingService(private val vurderingRepository: VurderingRepository) {

    fun hentVurdering(id: UUID): VurderingDto{
        val vurdering = vurderingRepository.findByIdOrNull(id)
            ?: return opprettEllerOppdaterVurdering(lagTomVurdering(id)).tilDto()
        return vurdering.tilDto()
    }

    fun hentVedtak(id: UUID): Vedtak?{
        return vurderingRepository.findVedtakByBehandlingIdOrThrow(id)
    }

    fun opprettEllerOppdaterVurdering(vurdering: Vurdering): Vurdering {
        if(sjekkOmVurderingEksiterer(vurdering.behandlingId)){
            return oppdaterVurdering(vurdering)
        } else {
            return vurderingRepository.insert(
                Vurdering(
                    behandlingId = vurdering.behandlingId,
                    vedtak = vurdering.vedtak,
                    arsak = vurdering.arsak,
                    hjemmel = vurdering.hjemmel,
                    beskrivelse = vurdering.beskrivelse,
                )
            )
        }
    }

    fun oppdaterVurdering(vurdering: Vurdering): Vurdering {
        val vurderingFraDb = vurderingRepository.findByBehandlingId(vurdering.behandlingId)
        return vurderingRepository.update(vurderingFraDb.copy(
            vedtak = vurdering.vedtak,
            beskrivelse = vurdering.beskrivelse,
            arsak = vurdering.arsak,
            hjemmel = vurdering.hjemmel
        ))
    }

    fun sjekkOmVurderingEksiterer(id: UUID): Boolean{
        return vurderingRepository.findById(id).isPresent
    }

    fun lagTomVurdering(behandlingId: UUID): Vurdering{
        return Vurdering(
            behandlingId = behandlingId,
            vedtak = Vedtak.VELG,
            arsak = null,
            hjemmel = null,
            beskrivelse = ""
        )
    }
}