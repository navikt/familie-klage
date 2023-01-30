package no.nav.familie.klage.vedlegg

import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus
import java.time.LocalDateTime

data class DokumentinfoDto(
    val dokumentinfoId: String,
    val filnavn: String?,
    val tittel: String,
    val journalpostId: String,
    val dato: LocalDateTime?,
    val journalstatus: Journalstatus,
    val journalposttype: Journalposttype,
    val logiskeVedlegg: List<LogiskVedleggDto>,
)

data class LogiskVedleggDto(val tittel: String)
