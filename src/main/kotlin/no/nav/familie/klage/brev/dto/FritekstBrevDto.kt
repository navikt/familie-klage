package no.nav.familie.klage.brev.dto

import no.nav.familie.klage.felles.domain.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class FritekstBrevDto(
    val overskrift: String,
    val avsnitt: List<AvsnittDto>,
    val behandlingId: UUID,
    val brevType: FritekstBrevtype
)

data class AvsnittDto(
    val avsnittId: UUID,
    val deloverskrift: String,
    val innhold: String,
    val skalSkjulesIBrevbygger: Boolean? = false)
data class Avsnitt(
    @Id
    val avsnittId: UUID,
    val behandlingId: UUID,
    val deloverskrift: String,
    val innhold: String,
    @Column("skal_skjules_i_brevbygger")
    val skalSkjulesIBrevbygger: Boolean? = false,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar()
)

data class FritekstBrevRequestDto(
    val overskrift: String,
    val avsnitt: List<AvsnittDto>,
    val personIdent: String,
    val navn: String
)
