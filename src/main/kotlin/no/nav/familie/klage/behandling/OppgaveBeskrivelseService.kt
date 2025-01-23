package no.nav.familie.klage.behandling

import no.nav.familie.klage.felles.util.dagensDatoMedNorskFormat
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.klage.oppgave.OppgaveService
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet
import org.springframework.stereotype.Service

@Service
class OppgaveBeskrivelseService(
    private val oppgaveService: OppgaveService,
) {
    fun utledOppgavebeskrivelse(
        oppgave: Oppgave,
        saksbehandler: String,
        prioritetEndring: OppgavePrioritet,
        fristEndring: String,
        mappeEndring: Long?,
        beskrivelse: String,
    ): String {
        val tilordnetSaksbehandler = utledTilordnetSaksbehandlerBeskrivelse(
            oppgave = oppgave,
            saksbehandler = saksbehandler,
        )
        val prioritet = utledPrioritetBeskrivelse(oppgave = oppgave, prioritetEndring = prioritetEndring)
        val frist = utledFristBeskrivelse(oppgave = oppgave, fristEndring = fristEndring)
        val mappe = utledMappeBeskrivelse(oppgave = oppgave, mappeEndring = mappeEndring)

        val endringer = listOf(tilordnetSaksbehandler, prioritet, frist, mappe)
        val harEndringer = endringer.any { it.isNotBlank() }
        val nyBeskrivelse = utledNyBeskrivelse(beskrivelse = beskrivelse)
        val skalOppdatereBeskrivelse = harEndringer || nyBeskrivelse.isNotBlank()

        val tidligereBeskrivelse = oppgave.beskrivelse.takeIf { skalOppdatereBeskrivelse && it?.isNotBlank() == true }
            ?.let { "\n$it" }.orEmpty()

        val prefix = utledBeskrivelsePrefix()

        return if (skalOppdatereBeskrivelse) {
            (prefix + nyBeskrivelse + endringer.joinToString("") + tidligereBeskrivelse).trimEnd()
        } else {
            tidligereBeskrivelse.trimEnd()
        }
    }

    private fun utledBeskrivelsePrefix(): String {
        val (innloggetSaksbehandlerIdent, saksbehandlerNavn) = with(SikkerhetContext) {
            hentSaksbehandler() to hentSaksbehandlerNavn(strict = false)
        }
        return "--- ${dagensDatoMedNorskFormat()} $saksbehandlerNavn ($innloggetSaksbehandlerIdent) ---\n"
    }

    private fun utledTilordnetSaksbehandlerBeskrivelse(
        oppgave: Oppgave,
        saksbehandler: String,
    ): String {
        val eksisterendeSaksbehandler = oppgave.tilordnetRessurs ?: INGEN_PLACEHOLDER
        val nySaksbehandler = saksbehandler.ifEmpty { INGEN_PLACEHOLDER }

        return if (eksisterendeSaksbehandler == nySaksbehandler) {
            ""
        } else {
            "Oppgave flyttet fra saksbehandler $eksisterendeSaksbehandler til $nySaksbehandler\n"
        }
    }

    private fun utledPrioritetBeskrivelse(
        oppgave: Oppgave,
        prioritetEndring: OppgavePrioritet,
    ): String {
        return oppgave.prioritet.takeIf { eksisterendePrioritet ->
            eksisterendePrioritet != prioritetEndring
        }?.let { prioritet ->
            "Oppgave endret fra prioritet ${prioritet.name} til $prioritetEndring\n"
        } ?: ""
    }

    private fun utledFristBeskrivelse(
        oppgave: Oppgave,
        fristEndring: String,
    ): String {
        return oppgave.fristFerdigstillelse.takeIf { eksisterendeFrist -> eksisterendeFrist != fristEndring }?.let {
            "Oppgave endret frist fra $it til $fristEndring\n"
        } ?: ""
    }

    private fun utledMappeBeskrivelse(
        oppgave: Oppgave,
        mappeEndring: Long?,
    ): String {
        val mapper = oppgaveService.finnMapper(
            listOfNotNull(oppgave.tildeltEnhetsnr).ifEmpty { throw Feil("Kan ikke finne mapper når oppgave mangler enhet") },
        )

        val eksisterendeMappe = oppgave.mappeId?.let { id ->
            mapper.find { it.id.toLong() == id }?.navn
        } ?: INGEN_PLACEHOLDER

        val nyMappe = mappeEndring?.let { id ->
            mapper.find { it.id.toLong() == id }?.navn
        } ?: INGEN_PLACEHOLDER

        return if (eksisterendeMappe == nyMappe) "" else "Oppgave flyttet fra mappe $eksisterendeMappe til $nyMappe\n"
    }

    private fun utledNyBeskrivelse(
        beskrivelse: String,
    ): String {
        return beskrivelse.takeIf { it.isNotBlank() }?.plus("\n") ?: ""
    }

    companion object {
        const val INGEN_PLACEHOLDER = "<ingen>"
    }
}
