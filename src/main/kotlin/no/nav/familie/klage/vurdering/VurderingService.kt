package no.nav.familie.klage.vurdering

import no.nav.familie.klage.vurdering.domain.Vurdering
import org.springframework.stereotype.Service

@Service
class VurderingService(private val vurderingRepository: VurderingRepository) {

    fun opprettVurdering(vurdering: Vurdering): Vurdering {
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