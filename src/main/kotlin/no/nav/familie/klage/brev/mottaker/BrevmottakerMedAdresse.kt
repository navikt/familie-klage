package no.nav.familie.klage.brev.mottaker

import no.nav.familie.klage.brev.domain.MottakerRolle
import no.nav.familie.klage.felles.domain.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class BrevmottakerMedAdresse(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val mottakerRolle: MottakerRolle,
    val navn: String,
    val adresselinje1: String,
    val adresselinje2: String?,
    val postnummer: String,
    val poststed: String,
    val landkode: String,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)
