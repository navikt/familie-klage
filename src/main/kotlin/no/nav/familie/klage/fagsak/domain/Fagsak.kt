package no.nav.familie.klage.fagsak.domain

import no.nav.familie.klage.felles.domain.Sporbar
import no.nav.familie.kontrakter.ef.søknad.SøknadType
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class Fagsak(
    @Id
    val id: UUID = UUID.randomUUID(),
    //val fagsystem: Fagsystem,
    val person_id: String,
    @Column("stonadstype")
    val søknadsType: SøknadType,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),)