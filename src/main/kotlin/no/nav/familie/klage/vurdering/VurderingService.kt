package no.nav.familie.klage.vurdering

import no.nav.familie.klage.vurdering.domain.Vurdering
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class VurderingService(private val vurderingRepository: VurderingRepository) {

    fun hentVurdering(id: UUID): Vurdering = vurderingRepository.findByBehandlingId(id)

    fun opprettVurdering(vurdering: Vurdering): Vurdering {
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
                    fullfortDato = vurdering.fullfortDato
                )
            )
        }
    }

    fun oppdaterVurdering(vurdering: Vurdering): Vurdering {
        return vurderingRepository.update(vurdering.copy())
    }

    fun sjekkOmVurderingEksiterer(id: UUID): Boolean{
        return vurderingRepository.findById(id).isPresent
    }
}