package no.nav.familie.klage.vurdering

import VurderingDto
import no.nav.familie.klage.repository.findByIdOrThrow
import no.nav.familie.klage.vurdering.domain.Vedtak
import no.nav.familie.klage.vurdering.domain.Vurdering
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import tilDto
import java.util.UUID

@Service
class VurderingService(private val vurderingRepository: VurderingRepository) {

    fun hentVurdering(behandlingId: UUID): VurderingDto{
        val vurdering = vurderingRepository.findByIdOrNull(behandlingId)
            ?: return opprettEllerOppdaterVurdering(lagTomVurdering(behandlingId)).tilDto()
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

    fun  klageTasIkkeTilFølge(behandlingId: UUID): Boolean{
        val vurdering = vurderingRepository.findByIdOrThrow(behandlingId)
        return (vurdering.vedtak == Vedtak.OPPRETTHOLD_VEDTAK)
    }
}