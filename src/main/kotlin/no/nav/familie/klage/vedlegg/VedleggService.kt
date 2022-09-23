package no.nav.familie.klage.vedlegg

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.journalpost.JournalpostService
import no.nav.familie.kontrakter.felles.journalpost.DokumentInfo
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariantformat
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class VedleggService(
        private val behandlingService: BehandlingService,
        private val journalpostService: JournalpostService
) {

    fun finnVedleggPåBehandling(behandlingId: UUID): List<DokumentinfoDto> {
        val personIdent = behandlingService.hentAktivIdent(behandlingId)
        val journalposter = journalpostService.finnJournalposter(personIdent)

        return journalposter
                .flatMap { journalpost -> journalpost.dokumenter?.map { tilDokumentInfoDto(it, journalpost) } ?: emptyList() }
    }

    private fun tilDokumentInfoDto(
            dokumentInfo: DokumentInfo,
            journalpost: Journalpost
    ): DokumentinfoDto {
        return DokumentinfoDto(
                dokumentinfoId = dokumentInfo.dokumentInfoId,
                filnavn = dokumentInfo.dokumentvarianter?.find { it.variantformat == Dokumentvariantformat.ARKIV }?.filnavn,
                tittel = dokumentInfo.tittel ?: "Tittel mangler",
                journalpostId = journalpost.journalpostId,
                dato = mestRelevanteDato(journalpost),
                journalstatus = journalpost.journalstatus,
                journalposttype = journalpost.journalposttype,
                logiskeVedlegg = dokumentInfo.logiskeVedlegg?.map { LogiskVedleggDto(tittel = it.tittel) } ?: emptyList()
        )
    }

    private fun mestRelevanteDato(journalpost: Journalpost): LocalDateTime? {
        return journalpost.datoMottatt ?: journalpost.relevanteDatoer?.maxByOrNull { datoTyperSortert(it.datotype) }?.dato
    }

    private fun datoTyperSortert(datoType: String) = when (datoType) {
        "DATO_JOURNALFOERT" -> 4
        "DATO_REGISTRERT" -> 3
        "DATO_DOKUMENT" -> 2
        "DATO_OPPRETTET" -> 1
        else -> 0
    }
}
