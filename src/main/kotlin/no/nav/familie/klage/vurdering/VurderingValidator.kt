package no.nav.familie.klage.vurdering

import no.nav.familie.klage.infrastruktur.exception.feilHvis
import no.nav.familie.klage.vurdering.domain.Vedtak
import no.nav.familie.klage.vurdering.dto.VurderingDto
import no.nav.familie.kontrakter.felles.klage.Fagsystem

object VurderingValidator {
    fun validerVurdering(
        vurdering: VurderingDto,
        fagsystem: Fagsystem,
    ) {
        when (vurdering.vedtak) {
            Vedtak.OMGJØR_VEDTAK -> {
                feilHvis(vurdering.årsak == null) {
                    "Mangler årsak på omgjør vedtak"
                }
                feilHvis(vurdering.begrunnelseOmgjøring == null) {
                    "Mangler begrunnelse for omgjøring på omgjør vedtak"
                }
                feilHvis(vurdering.hjemmel != null) {
                    "Kan ikke lagre hjemmel på omgjør vedtak"
                }
                feilHvis(
                    vurdering.innstillingKlageinstans != null ||
                        vurdering.dokumentasjonOgUtredning != null ||
                        vurdering.spørsmåletISaken != null ||
                        vurdering.aktuelleRettskilder != null ||
                        vurdering.klagersAnførsler != null ||
                        vurdering.vurderingAvKlagen != null,
                ) {
                    "Skal ikke ha innstilling til klageinstans ved omgjøring av vedtak"
                }
            }
            Vedtak.OPPRETTHOLD_VEDTAK -> {
                feilHvis(vurdering.hjemmel == null) {
                    "Mangler hjemmel på oppretthold vedtak"
                }
                feilHvis(vurdering.årsak != null) {
                    "Kan ikke lagre årsak på oppretthold vedtak"
                }
                feilHvis(vurdering.begrunnelseOmgjøring != null) {
                    "Kan ikke lagre begrunnelse på oppretthold vedtak"
                }
                if (fagsystem == Fagsystem.EF) {
                    feilHvis(vurdering.innstillingKlageinstans.isNullOrBlank()) {
                        "Må skrive innstilling til klageinstans ved opprettholdelse av vedtak"
                    }
                }
                if (fagsystem == Fagsystem.BA || fagsystem == Fagsystem.KS) {
                    val felterSomMangler = mutableListOf<String>()
                    if (vurdering.dokumentasjonOgUtredning.isNullOrBlank()) {
                        felterSomMangler.add("'Dokumentasjon og utredning'")
                    }
                    if (vurdering.spørsmåletISaken.isNullOrBlank()) {
                        felterSomMangler.add("'Spørsmålet i saken'")
                    }
                    if (vurdering.aktuelleRettskilder.isNullOrBlank()) {
                        felterSomMangler.add("'Aktuelle rettskilder'")
                    }
                    if (vurdering.klagersAnførsler.isNullOrBlank()) {
                        felterSomMangler.add("'Klagers anførsler'")
                    }
                    if (vurdering.vurderingAvKlagen.isNullOrBlank()) {
                        felterSomMangler.add("'Vurdering av klagen'")
                    }

                    feilHvis(felterSomMangler.isNotEmpty()) {
                        val felterSomManglerFormatert =
                            if (felterSomMangler.size > 1) {
                                "Feltene ${
                                    felterSomMangler.dropLast(1).joinToString(", ")
                                } og ${felterSomMangler.last()}"
                            } else {
                                "Feltet ${felterSomMangler.first()}"
                            }
                        "$felterSomManglerFormatert må fylles ut ved opprettholdelse av vedtak."
                    }
                }
            }
        }
    }
}
