package no.nav.familie.klage.brev.baks.brevmottaker

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
) {
    fun erFullmektigEllerVerge(): Boolean {
        return mottakertype == Mottakertype.FULLMEKTIG || mottakertype == Mottakertype.VERGE
    }

    companion object Fabrikk {
        fun opprett(behandlingId: UUID, nyBrevmottaker: NyBrevmottaker): Brevmottaker {
            return Brevmottaker(
                behandlingId = behandlingId,
                mottakertype = nyBrevmottaker.mottakertype,
                navn = nyBrevmottaker.navn,
                adresselinje1 = nyBrevmottaker.adresselinje1,
                adresselinje2 = nyBrevmottaker.adresselinje2,
                postnummer = nyBrevmottaker.postnummer,
                poststed = nyBrevmottaker.poststed,
                landkode = nyBrevmottaker.landkode,
            )
        }
    }
}
