package no.nav.familie.klage.kabal

import no.nav.familie.klage.kabal.domain.OversendtDokumentReferanse
import no.nav.familie.klage.kabal.domain.OversendtPartId
import no.nav.familie.klage.kabal.domain.OversendtSak
import no.nav.familie.klage.kabal.domain.Ytelse
import no.nav.familie.kontrakter.felles.Fagsystem
import java.time.LocalDate

// objektet som skal sendes til kabal
data class OversendtKlageAnkeV3(
    val type: Type,
    val klager: OversendtKlager,
    val sakenGjelder: OversendtSakenGjelder? = null,
    val fagsak: OversendtSak? = null,
    val kildeReferanse: String,
    val dvhReferanse: String? = null,
    val innsynUrl: String? = null, // url til vår løsning
    val hjemler: List<KabalHjemmel>,
    val forrigeBehandlendeEnhet: String,
    val tilknyttedeJournalposter: List<OversendtDokumentReferanse> = emptyList(),
    val brukersHenvendelseMottattNavDato: LocalDate,
    val innsendtTilNav: LocalDate,
    val kilde: Fagsystem,
    val ytelse: Ytelse,
    val kommentar: String? = null,
    val hindreAutomatiskSvarbrev: Boolean,
)

enum class Type(
    val id: String,
    val navn: String,
    val beskrivelse: String,
) {
    KLAGE("1", "Klage", "Klage"),
    ANKE("2", "Anke", "Anke"),
    ANKE_I_TRYGDERETTEN("3", "Anke i trygderetten", "Anke i trygderetten"),
    OMGJOERINGSKRAV("4", "Omgjøringskrav", "Omgjøringskrav"),
}

data class OversendtKlager(
    val id: OversendtPartId,
    val klagersProsessfullmektig: OversendtProsessfullmektig? = null,
)

data class OversendtProsessfullmektig(
    val id: OversendtPartId,
    @Deprecated("Denne er deprecated i Kabal og brukes ikke")
    val skalKlagerMottaKopi: Boolean,
)

data class OversendtSakenGjelder(
    val id: OversendtPartId,
    val skalMottaKopi: Boolean,
)
