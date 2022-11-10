package no.nav.familie.klage.vurdering.dto

import no.nav.familie.klage.vurdering.domain.Hjemmel
import no.nav.familie.klage.vurdering.domain.Vedtak
import no.nav.familie.klage.vurdering.domain.Vurdering
import no.nav.familie.kontrakter.felles.klage.Årsak
import java.util.UUID

data class VurderingDto(
    val behandlingId: UUID,
    val vedtak: Vedtak,
    val årsak: Årsak? = null,
    val begrunnelseOmgjøring: String? = null,
    val hjemmel: Hjemmel? = null,
    val innstillingKlageinstans: String? = null,
    val interntNotat: String?
)

fun Vurdering.tilDto(): VurderingDto =
    VurderingDto(
        behandlingId = this.behandlingId,
        vedtak = this.vedtak,
        årsak = this.årsak,
        begrunnelseOmgjøring = this.begrunnelseOmgjøring,
        hjemmel = this.hjemmel,
        innstillingKlageinstans = this.innstillingKlageinstans,
        interntNotat = this.interntNotat
    )
