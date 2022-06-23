package no.nav.familie.klage.brev.dto

import no.nav.familie.ef.sak.brev.dto.FritekstBrevKategori
import java.util.UUID

data class FritekstBrevDto(
    val overskrift: String,
    val avsnitt: List<FrittståendeBrevAvsnitt>,
    val behandlingId: UUID,
    val brevType: FritekstBrevKategori
)

data class FrittståendeBrevAvsnitt(val deloverskrift: String, val innhold: String, val skalSkjulesIBrevbygger: Boolean? = false)

data class FritekstBrevRequestDto(
    val overskrift: String,
    val avsnitt: List<FrittståendeBrevAvsnitt>,
    val personIdent: String,
    val navn: String
)
