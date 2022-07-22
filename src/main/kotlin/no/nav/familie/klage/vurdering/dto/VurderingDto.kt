import no.nav.familie.klage.vurdering.domain.Arsak
import no.nav.familie.klage.vurdering.domain.Hjemmel
import no.nav.familie.klage.vurdering.domain.Vedtak
import no.nav.familie.klage.vurdering.domain.Vurdering
import org.springframework.data.annotation.Id
import java.util.UUID

data class VurderingDto(
    @Id
    val behandlingId: UUID,
    val vedtak: Vedtak,
    val arsak: Arsak? = null,
    val hjemmel: Hjemmel? = null,
    val beskrivelse: String,
)

fun Vurdering.tilDto(): VurderingDto =
    VurderingDto(
        behandlingId = this.behandlingId,
        vedtak = this.vedtak,
        arsak = this.arsak,
        hjemmel = this.hjemmel,
        beskrivelse = this.beskrivelse,
    )

