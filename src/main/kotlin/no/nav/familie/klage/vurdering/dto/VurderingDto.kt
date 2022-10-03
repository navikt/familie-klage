package no.nav.familie.klage.vurdering.dto

import no.nav.familie.klage.vurdering.domain.Hjemmel
import no.nav.familie.klage.vurdering.domain.Vedtak
import no.nav.familie.klage.vurdering.domain.Vurdering
import no.nav.familie.kontrakter.felles.klage.Årsak
import java.util.UUID

data class VurderingDto(
    val behandlingId: UUID,
    val vedtak: Vedtak,
    val arsak: Årsak? = null,
    val hjemmel: Hjemmel? = null,
    val beskrivelse: String
)

fun Vurdering.tilDto(): VurderingDto =
    VurderingDto(
        behandlingId = this.behandlingId,
        vedtak = this.vedtak,
        arsak = this.arsak,
        hjemmel = this.hjemmel,
        beskrivelse = this.beskrivelse
    )
