package no.nav.familie.klage.oppgave

import no.nav.familie.http.client.RessursException
import no.nav.familie.klage.behandling.dto.OppgaveDto
import no.nav.familie.klage.infrastruktur.exception.ApiFeil
import no.nav.familie.klage.infrastruktur.exception.ManglerTilgang
import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.klage.infrastruktur.featuretoggle.Toggle
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.klage.oppgave.dto.SaksbehandlerDto
import no.nav.familie.klage.oppgave.dto.SaksbehandlerRolle
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.StatusEnum
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import java.util.UUID

@Service
class TilordnetRessursService(
    private val oppgaveClient: OppgaveClient,
    private val featureToggleService: FeatureToggleService,
    private val behandleSakOppgaveRepository: BehandleSakOppgaveRepository,
) {
    fun hentAnsvarligSaksbehandlerForBehandlingsId(behandlingId: UUID): SaksbehandlerDto {
        val behandleSakOppgave = behandleSakOppgaveRepository.findByBehandlingId(behandlingId)
        val oppgave =
            try {
                behandleSakOppgave?.let { oppgaveClient.finnOppgaveMedId(it.oppgaveId) }
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

        return if (oppgave != null) {
            OppgaveDto(
                oppgaveId = oppgave.id,
                tildeltEnhetsnr = oppgave.tildeltEnhetsnr,
                beskrivelse = oppgave.beskrivelse,
                tilordnetRessurs = oppgave.tilordnetRessurs ?: "",
                prioritet = oppgave.prioritet,
                fristFerdigstillelse = oppgave.fristFerdigstillelse ?: "",
                mappeId = oppgave.mappeId,
                versjon = oppgave.versjon,
            )
        } else {
            throw ApiFeil(
                feilmelding = "Finnes ikke oppgave for behandlingen",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
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

    private fun erUtviklerMedVeilederrolle(): Boolean = featureToggleService.isEnabled(Toggle.UTVIKLER_MED_VEILEDERRROLLE)
}
