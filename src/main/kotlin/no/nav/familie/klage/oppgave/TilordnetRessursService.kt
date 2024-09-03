package no.nav.familie.klage.oppgave

import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.klage.infrastruktur.featuretoggle.Toggle
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.klage.oppgave.dto.SaksbehandlerDto
import no.nav.familie.klage.oppgave.dto.SaksbehandlerRolle
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.StatusEnum
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TilordnetRessursService(
    private val oppgaveClient: OppgaveClient,
    private val featureToggleService: FeatureToggleService,
    private val behandleSakOppgaveRepository: BehandleSakOppgaveRepository,
) {

    fun hentAnsvarligSaksbehandlerForBehandlingsId(behandlingId: UUID): SaksbehandlerDto {
        val behandleSakOppgave = behandleSakOppgaveRepository.findByBehandlingId(behandlingId)
        val oppgave = oppgaveClient.finnOppgaveMedId(behandleSakOppgave.oppgaveId)
        val tilordnetRessurs = oppgave.tilordnetRessurs ?: ""

        val rolle = utledSaksbehandlerRolle(oppgave)
        val saksbehandler = oppgaveClient.hentSaksbehandlerInfo(tilordnetRessurs)

        return SaksbehandlerDto(
            etternavn = saksbehandler.etternavn,
            fornavn = saksbehandler.fornavn,
            rolle = rolle,
        )
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
