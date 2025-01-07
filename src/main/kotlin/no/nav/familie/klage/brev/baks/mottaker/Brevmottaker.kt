package no.nav.familie.klage.brev.baks.mottaker

import no.nav.familie.klage.felles.domain.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

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

fun Brevmottaker.mapTilBrevmottakerDto(): BrevmottakerDto =
    BrevmottakerDto(
        id = this.id,
        mottakertype = this.mottakertype,
        navn = this.navn,
        adresselinje1 = this.adresselinje1,
        adresselinje2 = this.adresselinje2,
        postnummer = this.postnummer,
        poststed = this.poststed,
        landkode = this.landkode,
    )
