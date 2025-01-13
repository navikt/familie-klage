package no.nav.familie.klage.kabal

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
    override val id: String,
    override val navn: String,
    override val beskrivelse: String,
) : Kode {
    KLAGE("1", "Klage", "Klage"),
    ANKE("2", "Anke", "Anke"),
    ANKE_I_TRYGDERETTEN("3", "Anke i trygderetten", "Anke i trygderetten"),
    OMGJOERINGSKRAV("4", "Omgjøringskrav", "Omgjøringskrav"),
}

enum class OversendtPartIdType {
    PERSON,
    VIRKSOMHET,
}

data class OversendtPartId(
    val type: OversendtPartIdType,
    val verdi: String,
)

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

data class OversendtSak(
    val fagsakId: String? = null,
    val fagsystem: Fagsystem,
)

data class OversendtDokumentReferanse(
    val type: MottakDokumentType,
)

enum class MottakDokumentType {
    BRUKERS_SOEKNAD,
    OPPRINNELIG_VEDTAK,
    BRUKERS_KLAGE,
    BRUKERS_ANKE,
    OVERSENDELSESBREV,
    KLAGE_VEDTAK,
    ANNET,
}

enum class Ytelse {
    ENF_ENF,
    BAR_BAR,
    KON_KON,
}

interface Kode {
    val id: String
    val navn: String
    val beskrivelse: String
}

enum class LovKilde(
    override val id: String,
    override val navn: String,
    override val beskrivelse: String,
) : Kode {
    FOLKETRYGDLOVEN("1", "Folketrygdloven", "Ftrl"),
    NORDISK_KONVENSJON("15", "Nordisk konvensjon", "Nordisk konvensjon"),
    ANDRE_TRYGDEAVTALER("21", "Andre trygdeavtaler", "Andre trygdeavtaler"),
    BARNETRYGDLOVEN("29", "Barnetrygdloven", "Btrl"),
    EØS_AVTALEN("30", "EØS-avtalen", "EØS-avtalen"),
    KONTANTSTØTTELOVEN("31", "Kontantstøtteloven", "Kontsl"),
    FORVALTNINGSLOVEN("8", "Forvaltningsloven", "Fvl"),
}
