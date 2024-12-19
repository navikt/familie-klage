package no.nav.familie.klage.oppgave

import no.nav.familie.klage.behandling.dto.OppgaveDto
import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.klage.infrastruktur.featuretoggle.Toggle
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.klage.oppgave.dto.SaksbehandlerDto
import no.nav.familie.klage.oppgave.dto.SaksbehandlerRolle
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.StatusEnum
import org.springframework.stereotype.Service
import java.util.*

@Service
class TilordnetRessursService(
    private val oppgaveClient: OppgaveClient,
    private val featureToggleService: FeatureToggleService,
    private val behandleSakOppgaveRepository: BehandleSakOppgaveRepository,
) {

    fun hentAnsvarligSaksbehandlerForBehandlingsId(behandlingId: UUID): SaksbehandlerDto {
        val behandleSakOppgave = behandleSakOppgaveRepository.findByBehandlingId(behandlingId)
        val oppgave = behandleSakOppgave?.let { oppgaveClient.finnOppgaveMedId(it.oppgaveId) }

        val rolle = utledSaksbehandlerRolle(oppgave)
        val saksbehandler = oppgave?.tilordnetRessurs?.let { oppgaveClient.hentSaksbehandlerInfo(it) }

        return SaksbehandlerDto(
            etternavn = saksbehandler?.etternavn ?: "",
            fornavn = saksbehandler?.fornavn ?: "",
            rolle = rolle,
        )
    }

    fun hentOppgave(behandlingId: UUID): OppgaveDto? {
        val behandleSakOppgave = behandleSakOppgaveRepository.findByBehandlingId(behandlingId)
        val oppgave = behandleSakOppgave?.let { oppgaveClient.finnOppgaveMedId(it.oppgaveId) }
        val saksbehandler = oppgave?.tilordnetRessurs?.let { oppgaveClient.hentSaksbehandlerInfo(it) }

        return if (oppgave != null) {
            OppgaveDto(
                oppgaveId = oppgave.id,
                beskrivelse = oppgave.beskrivelse,
                tilordnetRessurs = saksbehandler?.navIdent ?: "",
                prioritet = oppgave.prioritet,
                fristFerdigstillelse = oppgave.fristFerdigstillelse ?: "",
                mappeId = oppgave.mappeId,
            )
        } else {
            null
        }
    }

    private fun utledSaksbehandlerRolle(oppgave: Oppgave?): SaksbehandlerRolle {
        if (erUtviklerMedVeilederrolle()) {
            return SaksbehandlerRolle.UTVIKLER_MED_VEILDERROLLE
        }

        if (oppgave == null) {
            return SaksbehandlerRolle.OPPGAVE_FINNES_IKKE
        }

        if (oppgave.tema != Tema.ENF || oppgave.status == StatusEnum.FEILREGISTRERT) {
            return SaksbehandlerRolle.OPPGAVE_TILHØRER_IKKE_ENF
        }

        val innloggetSaksbehandler = SikkerhetContext.hentSaksbehandler()
        return when (oppgave.tilordnetRessurs) {
            innloggetSaksbehandler -> SaksbehandlerRolle.INNLOGGET_SAKSBEHANDLER
            null -> SaksbehandlerRolle.IKKE_SATT
            else -> SaksbehandlerRolle.ANNEN_SAKSBEHANDLER
        }
    }

    private fun erUtviklerMedVeilederrolle(): Boolean =
        featureToggleService.isEnabled(Toggle.UTVIKLER_MED_VEILEDERRROLLE)
}
