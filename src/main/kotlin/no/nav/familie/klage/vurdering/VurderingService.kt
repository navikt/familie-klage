package no.nav.familie.klage.vurdering

import no.nav.familie.klage.repository.findByIdOrThrow
import no.nav.familie.klage.vurdering.domain.Vedtak
import no.nav.familie.klage.vurdering.domain.Vurdering
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class VurderingService(private val vurderingRepository: VurderingRepository) {

    fun hentVurdering(id: UUID): Vurdering = vurderingRepository.findByBehandlingId(id)

    fun hentVedtak(id: UUID): Vedtak?{
        return vurderingRepository.findVedtakByBehandlingIdOrThrow(id)
    }

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

    fun  klageTasIkkeTilFølge(behandlingId: UUID): Boolean{
        val vurdering = vurderingRepository.findByIdOrThrow(behandlingId)
        return (vurdering.vedtak == Vedtak.OPPRETTHOLD_VEDTAK)
    }
}