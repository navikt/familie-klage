package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype

object OpprettRevurderingUtil {

    fun skalOppretteRevurderingAutomatisk(påklagetVedtakstype: PåklagetVedtakstype): Boolean =
        setOf(
            PåklagetVedtakstype.VEDTAK,
            PåklagetVedtakstype.INFOTRYGD_ORDINÆRT_VEDTAK
        ).contains(påklagetVedtakstype)
}