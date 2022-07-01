package no.nav.familie.klage.vurdering

import no.nav.familie.klage.infrastruktur.exception.ApiFeil
import no.nav.familie.klage.vurdering.domain.Vurdering
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class VurderingService(private val vurderingRepository: VurderingRepository) {

    private val logger = LoggerFactory.getLogger(javaClass)


    fun opprettVurdering(vurdering: Vurdering): Vurdering {
        validerVurdering(vurdering)
        return vurderingRepository.insert(
            Vurdering(
                behandlingId = vurdering.behandlingId,
                oppfyltFormkrav = vurdering.oppfyltFormkrav,
                muligFormkrav = vurdering.muligFormkrav,
                begrunnelse = vurdering.begrunnelse,
                vedtakValg = vurdering.vedtakValg,
                årsak = vurdering.årsak,
                hjemmel = vurdering.hjemmel,
                beskrivelse = vurdering.beskrivelse,
                fullførtDato = vurdering.fullførtDato
            )
        )
    }

    fun validerVurdering(vurdering: Vurdering) {
        if (vurdering.oppfyltFormkrav == null) {
            throw ApiFeil("Oppfylt formkrav kan ikke være null", HttpStatus.BAD_REQUEST)
        }
        if (vurdering.muligFormkrav == null) {
            throw ApiFeil("muligFormkrav kan ikke være null", HttpStatus.BAD_REQUEST)
        }
        logger.info("${vurdering}")
    }
}