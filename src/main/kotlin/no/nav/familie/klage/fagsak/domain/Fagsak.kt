package no.nav.familie.klage.fagsak.domain

import no.nav.familie.klage.behandling.domain.StønadsType
import no.nav.familie.klage.felles.domain.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class Fagsak(
    @Id
    val id: UUID = UUID.randomUUID(),
    val personIdent: String,
    @Column("stonadstype")
    val stønadsType: StønadsType,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)
