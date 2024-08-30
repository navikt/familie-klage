package no.nav.familie.klage.oppgave

import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.klage.infrastruktur.featuretoggle.Toggle
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.klage.oppgave.dto.SaksbehandlerDto
import no.nav.familie.klage.oppgave.dto.SaksbehandlerRolle
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TilordnetRessursService(
    private val oppgaveClient: OppgaveClient,
    private val featureToggleService: FeatureToggleService,
    private val behandleSakOppgaveRepository: BehandleSakOppgaveRepository,
) {

    fun hentAnsvarligSaksbehandlerForBehandlingsId(behandlingId: UUID): SaksbehandlerDto {
        val oppgave = behandleSakOppgaveRepository.findByBehandlingId(behandlingId)
        val ident = oppgaveClient.finnOppgaveMedId(oppgave.oppgaveId).tilordnetRessurs ?: ""

        val rolle = utledSaksbehandlerRolle(ident)
        val tilordnet = hentSaksbehandlerInfo(ident)
        return SaksbehandlerDto(
            etternavn = tilordnet.etternavn,
            fornavn = tilordnet.fornavn,
            rolle = rolle,
        )
    }

    fun hentSaksbehandlerInfo(navIdent: String) = oppgaveClient.hentSaksbehandlerInfo(navIdent)

    fun utledSaksbehandlerRolle(ident: String): SaksbehandlerRolle {
        if (erUtviklerMedVeilderrolle()) {
            return SaksbehandlerRolle.UTVIKLER_MED_VEILDERROLLE
        }

        if (ident.isNullOrBlank()) {
            return SaksbehandlerRolle.OPPGAVE_FINNES_IKKE
        }

        val innloggetSaksbehandler = SikkerhetContext.hentSaksbehandler()
        return when (ident) {
            innloggetSaksbehandler -> SaksbehandlerRolle.INNLOGGET_SAKSBEHANDLER
            null -> SaksbehandlerRolle.IKKE_SATT
            else -> SaksbehandlerRolle.ANNEN_SAKSBEHANDLER
        }
    }

    private fun erUtviklerMedVeilderrolle(): Boolean =
        featureToggleService.isEnabled(Toggle.UTVIKLER_MED_VEILEDERRROLLE)
}
