package no.nav.familie.klage.brev.dto

data class Flettefelter(
    val navn: List<String>,
    val fodselsnummer: List<String>,
)

data class Henleggelsesbrev(
    val delmaler: Delmaler,
    val flettefelter: Flettefelter,
)

data class Delmal(
    val flettefelter: DelmalFlettefelt,
)

data class Delmaler(
    val stonadstypeKlage: List<Delmal>,
)

data class DelmalFlettefelt(
    val stonadstype: List<String>,
)
