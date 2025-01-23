package no.nav.familie.klage.brev.baks.brevmottaker

import no.nav.familie.klage.felles.domain.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

// TODO : Delete me
data class Brevmottaker(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val mottakertype: Mottakertype,
    val navn: String,
    @Column("adresselinje_1")
    val adresselinje1: String,
    @Column("adresselinje_2")
    val adresselinje2: String?,
    val postnummer: String?,
    val poststed: String?,
    val landkode: String,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)
