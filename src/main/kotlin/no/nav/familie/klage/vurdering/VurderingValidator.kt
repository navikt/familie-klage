package no.nav.familie.klage.vurdering

import no.nav.familie.klage.infrastruktur.exception.feilHvis
import no.nav.familie.klage.vurdering.domain.Vedtak
import no.nav.familie.klage.vurdering.dto.VurderingDto

object VurderingValidator {

    fun validerVurdering(vurdering: VurderingDto) {
        when (vurdering.vedtak) {
            Vedtak.OMGJØR_VEDTAK -> {
                feilHvis(vurdering.arsak == null) {
                    "Mangler årsak på omgjør vedtak"
                }
                feilHvis(vurdering.hjemmel != null) {
                    "Kan ikke lagre hjemmel på omgjør vedtak"
                }
                feilHvis(vurdering.innstillingKlageinstans != null) {
                    "Skal ikke ha innstilling til klageinstans ved omgjøring av vedtak"
                }
            }
            Vedtak.OPPRETTHOLD_VEDTAK -> {
                feilHvis(vurdering.hjemmel == null) {
                    "Mangler hjemmel på oppretthold vedtak"
                }
                feilHvis(vurdering.arsak != null) {
                    "Kan ikke lagre årsak på oppretthold vedtak"
                }
                feilHvis(vurdering.innstillingKlageinstans.isNullOrBlank()) {
                    "Må skrive innstilling til klageinstans ved opprettholdelse av vedtak"
                }
            }
        }
    }
}
