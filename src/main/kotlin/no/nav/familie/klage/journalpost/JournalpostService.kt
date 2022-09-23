package no.nav.familie.klage.journalpost

import no.nav.familie.klage.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.familie.klage.infrastruktur.exception.feilHvis
import no.nav.familie.klage.integrasjoner.FamilieIntegrasjonerClient
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariantformat
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import org.springframework.stereotype.Service

@Service
class JournalpostService(private val familieIntegrasjonerClient: FamilieIntegrasjonerClient) {

    fun hentJournalpost(journalpostId: String): Journalpost {
        return familieIntegrasjonerClient.hentJournalpost(journalpostId)
    }

    fun finnJournalposter(
        personIdent: String,
        antall: Int = 200,
        typer: List<Journalposttype> = Journalposttype.values().toList()
    ): List<Journalpost> {
        return familieIntegrasjonerClient.finnJournalposter(
            JournalposterForBrukerRequest(
                brukerId = Bruker(
                    id = personIdent,
                    type = BrukerIdType.FNR
                ),
                antall = antall,
                tema = listOf(Tema.ENF),
                journalposttype = typer
            )
        )
    }

    fun hentDokument(
        journalpost: Journalpost,
        dokumentInfoId: String
    ): ByteArray {
        validerDokumentKanHentes(journalpost, dokumentInfoId)
        return familieIntegrasjonerClient.hentDokument(journalpost.journalpostId, dokumentInfoId)
    }

    private fun validerDokumentKanHentes(
        journalpost: Journalpost,
        dokumentInfoId: String
    ) {
        val dokument = journalpost.dokumenter?.find { it.dokumentInfoId == dokumentInfoId }
        feilHvis(dokument == null) {
            "Finner ikke dokument med $dokumentInfoId for journalpost=${journalpost.journalpostId}"
        }
        brukerfeilHvisIkke(dokument.dokumentvarianter?.any { it.variantformat == Dokumentvariantformat.ARKIV } ?: false) {
            "Vedlegget er sannsynligvis under arbeid, må åpnes i gosys"
        }
    }
}
