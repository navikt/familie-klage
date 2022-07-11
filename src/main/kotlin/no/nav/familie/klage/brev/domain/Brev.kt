package no.nav.familie.klage.brev.domain

import no.nav.familie.klage.brev.dto.FrittståendeBrevAvsnitt

data class Brev(
    val overskrift: String,
    val avsnitt: List<FrittståendeBrevAvsnitt>)

enum class FormVilkår {
    OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_SATT
}
