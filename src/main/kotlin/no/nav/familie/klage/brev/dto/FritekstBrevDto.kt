package no.nav.familie.klage.brev.dto

import no.nav.familie.ef.sak.brev.dto.FritekstBrevKategori
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import java.util.UUID

data class FritekstBrevDto(
    val overskrift: String,
    val avsnitt: List<AvsnittDto>,
    val behandlingId: UUID,
    val brevType: FritekstBrevKategori
)

data class AvsnittDto(
    val deloverskrift: String,
    val innhold: String,
    val skalSkjulesIBrevbygger: Boolean? = false)
data class Avsnitt(
    @Id
    val avsnittId: UUID,
    val brevId: UUID,
    val deloverskrift: String,
    val innhold: String,
    @Column("skal_skjules_i_brevbygger")
    val skalSkjulesIBrevbygger: Boolean? = false)

data class FritekstBrevRequestDto(
    val overskrift: String,
    val avsnitt: List<AvsnittDto>,
    val personIdent: String,
    val navn: String
)
