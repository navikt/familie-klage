package no.nav.familie.klage.kabal

import no.nav.familie.klage.kabal.domain.OversendtDokumentReferanse
import no.nav.familie.klage.kabal.domain.OversendtPartId
import no.nav.familie.klage.kabal.domain.OversendtSak
import no.nav.familie.klage.kabal.domain.Ytelse
import java.time.LocalDate
import java.time.LocalDateTime

data class OversendtKlageAnkeV4(
    val type: OversendtType,
    val sakenGjelder: OversendtPartV4,
    val klager: OversendtPartV4?,
    val prosessfullmektig: OversendtProsessfullmektigV4?,
    val fagsak: OversendtSak,
    val kildeReferanse: String,
    val dvhReferanse: String? = null,
    val hjemler: List<KabalHjemmel>,
    val forrigeBehandlendeEnhet: String,
    val tilknyttedeJournalposter: List<OversendtDokumentReferanse>,
    val brukersKlageMottattVedtaksinstans: LocalDate?,
    val frist: LocalDate?,
    val sakMottattKaTidspunkt: LocalDateTime?,
    val ytelse: Ytelse,
    val kommentar: String? = null,
    val hindreAutomatiskSvarbrev: Boolean?,
    val saksbehandlerIdentForTildeling: String?,
)

enum class OversendtType {
    KLAGE,
    ANKE,
}

data class OversendtProsessfullmektigV4(
    val id: OversendtPartId?,
    val navn: String?,
    val adresse: OversendtAdresseV4?,
)

data class OversendtAdresseV4(
    val adresselinje1: String?,
    val adresselinje2: String?,
    val adresselinje3: String?,
    val postnummer: String?,
    val poststed: String?,
    val land: String,
)

data class OversendtPartV4(
    val id: OversendtPartId,
)
