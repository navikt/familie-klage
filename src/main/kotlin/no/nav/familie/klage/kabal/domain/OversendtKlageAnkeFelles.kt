package no.nav.familie.klage.kabal.domain

import no.nav.familie.klage.brevmottaker.domain.Brevmottakere
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle
import no.nav.familie.kontrakter.felles.Fagsystem

sealed interface OversendtKlageAnke

enum class OversendtPartIdType {
    PERSON,
    VIRKSOMHET,
}

data class OversendtPartId(
    val type: OversendtPartIdType,
    val verdi: String,
)

data class OversendtSak(
    val fagsakId: String? = null,
    val fagsystem: Fagsystem,
)

data class OversendtDokumentReferanse(
    val type: MottakDokumentType,
    val journalpostId: String,
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

fun utledFullmektigEllerVerge(brevmottakere: Brevmottakere) =
    brevmottakere.personer.firstOrNull { it.mottakerRolle == MottakerRolle.FULLMAKT }
        ?: brevmottakere.personer.firstOrNull { it.mottakerRolle == MottakerRolle.VERGE }
        ?: brevmottakere.organisasjoner.firstOrNull()
