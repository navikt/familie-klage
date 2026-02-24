package no.nav.familie.klage.journalpost

import no.nav.familie.klage.infrastruktur.exception.ManglerTilgang
import no.nav.familie.klage.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.familie.klage.infrastruktur.exception.feilHvis
import no.nav.familie.klage.integrasjoner.FamilieIntegrasjonerClient
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariantformat
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import no.nav.familie.restklient.client.RessursException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException

@Service
class JournalpostService(
    private val familieIntegrasjonerClient: FamilieIntegrasjonerClient,
) {
    fun hentJournalpost(journalpostId: String): Journalpost = familieIntegrasjonerClient.hentJournalpost(journalpostId)

    fun finnJournalposter(
        personIdent: String,
        stønadType: Stønadstype,
        antall: Int = 200,
        typer: List<Journalposttype> = Journalposttype.entries,
    ): List<Journalpost> =
        try {
            familieIntegrasjonerClient.finnJournalposter(
                JournalposterForBrukerRequest(
                    brukerId =
                        Bruker(
                            id = personIdent,
                            type = BrukerIdType.FNR,
                        ),
                    antall = antall,
                    tema = listOf(stønadType.tilTema()),
                    journalposttype = typer,
                ),
            )
        } catch (exception: HttpClientErrorException) {
            if (exception.statusCode == HttpStatus.FORBIDDEN) {
                throw ManglerTilgang(
                    melding = "Bruker mangler tilgang til etterspurt oppgave",
                    frontendFeilmelding = "Behandlingen er koblet til en oppgave du ikke har tilgang til. Visning av ansvarlig saksbehandler er derfor ikke mulig",
                )
            } else {
                throw exception
            }
        } catch (exception: RessursException) {
            if (exception.httpStatus == HttpStatus.FORBIDDEN) {
                throw ManglerTilgang(
                    melding = "Bruker mangler tilgang til etterspurt oppgave",
                    frontendFeilmelding = "Behandlingen er koblet til en oppgave du ikke har tilgang til. Visning av ansvarlig saksbehandler er derfor ikke mulig",
                )
            } else {
                throw exception
            }
        }

    fun hentDokument(
        journalpost: Journalpost,
        dokumentInfoId: String,
    ): ByteArray {
        validerDokumentKanHentes(journalpost, dokumentInfoId)
        return familieIntegrasjonerClient.hentDokument(journalpost.journalpostId, dokumentInfoId)
    }

    private fun validerDokumentKanHentes(
        journalpost: Journalpost,
        dokumentInfoId: String,
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
