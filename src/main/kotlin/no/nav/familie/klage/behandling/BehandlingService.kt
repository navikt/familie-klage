package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.Klagebehandlingsesultat
import no.nav.familie.klage.behandling.domain.PåklagetVedtak
import no.nav.familie.klage.behandling.domain.StegType.BEHANDLING_FERDIGSTILT
import no.nav.familie.klage.behandling.domain.erLåstForVidereBehandling
import no.nav.familie.klage.behandling.domain.erUnderArbeidAvSaksbehandler
import no.nav.familie.klage.behandling.dto.BehandlingDto
import no.nav.familie.klage.behandling.dto.HenlagtDto
import no.nav.familie.klage.behandling.dto.PåklagetVedtakDto
import no.nav.familie.klage.behandling.dto.tilDto
import no.nav.familie.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.klage.infrastruktur.exception.feilHvisIkke
import no.nav.familie.klage.kabal.KlageresultatRepository
import no.nav.familie.klage.kabal.domain.tilDto
import no.nav.familie.klage.oppgave.OppgaveTaskService
import no.nav.familie.klage.repository.findByIdOrThrow
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus.FERDIGSTILT
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.KlageinstansResultatDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class BehandlingService(
    private val behandlingRepository: BehandlingRepository,
    private val fagsakService: FagsakService,
    private val klageresultatRepository: KlageresultatRepository,
    private val behandlinghistorikkService: BehandlingshistorikkService,
    private val oppgaveTaskService: OppgaveTaskService
) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun hentBehandling(behandlingId: UUID): Behandling = behandlingRepository.findByIdOrThrow(behandlingId)

    fun hentBehandlingDto(behandlingId: UUID): BehandlingDto {
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        return behandlingRepository.findByIdOrThrow(behandlingId)
            .tilDto(fagsak, hentKlageresultatDto(behandlingId))
    }

    fun opprettBehandling(behandling: Behandling): Behandling {
        validerKanOppretteBehandling(behandling.fagsakId)
        return behandlingRepository.insert(behandling)
    }

    // TODO
    fun hentNavnFraBehandlingsId(behandlingId: UUID): String {
        return "Navn Navnesen"
    }

    fun hentKlageresultatDto(behandlingId: UUID): List<KlageinstansResultatDto> {
        val klageresultater = klageresultatRepository.findByBehandlingId(behandlingId)
        return klageresultater.tilDto()
    }

    fun finnKlagebehandlingsresultat(eksternFagsakId: String, fagsystem: Fagsystem): List<Klagebehandlingsesultat> {
        return behandlingRepository.finnKlagebehandlingsresultat(eksternFagsakId, fagsystem)
    }

    fun hentAktivIdent(behandlingId: UUID): Pair<String, Fagsak> {
        val behandling = hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
        return Pair(fagsak.hentAktivIdent(), fagsak)
    }

    fun oppdaterBehandlingsresultatOgVedtaksdato(behandlingId: UUID, behandlingsresultat: BehandlingResultat) {
        val behandling = hentBehandling(behandlingId)
        if (behandling.resultat != BehandlingResultat.IKKE_SATT) {
            error("Kan ikke endre på et resultat som allerede er satt")
        }
        val oppdatertBehandling = behandling.copy(resultat = behandlingsresultat, vedtakDato = LocalDateTime.now())
        behandlingRepository.update(oppdatertBehandling)
    }

    @Transactional
    fun oppdaterPåklagetVedtak(behandlingId: UUID, påklagetVedtakDto: PåklagetVedtakDto) {
        val behandling = hentBehandling(behandlingId)
        brukerfeilHvis(behandling.status.erLåstForVidereBehandling()) {
            "Kan ikke oppdatere påklaget vedtak siden behandlingen er låst for videre saksbehandling"
        }
        feilHvisIkke(påklagetVedtakDto.erGyldig()) {
            "Påklaget vedtak er i en ugyldig tilstand: EksternFagsystemBehandlingId:${påklagetVedtakDto.eksternFagsystemBehandlingId}, PåklagetVedtakType: ${påklagetVedtakDto.påklagetVedtakstype}"
        }
        val behandlingMedPåklagetVedtak = behandling.copy(
            påklagetVedtak = PåklagetVedtak(
                eksternFagsystemBehandlingId = påklagetVedtakDto.eksternFagsystemBehandlingId,
                påklagetVedtakstype = påklagetVedtakDto.påklagetVedtakstype
            )
        )
        behandlingRepository.update(behandlingMedPåklagetVedtak)
    }

    private fun validerKanOppretteBehandling(fagsakId: UUID) {
        val behandlinger = behandlingRepository.findByFagsakId(fagsakId)

        brukerfeilHvis(behandlinger.any { it.status.erUnderArbeidAvSaksbehandler() }) {
            "Det eksisterer allerede en klagebehandling som ikke er ferdigstilt på fagsak med id=$fagsakId"
        }
    }

    fun henleggBehandling(behandlingId: UUID, henlagt: HenlagtDto) {
        val behandling = hentBehandling(behandlingId)

        validerKanHenleggeBehandling(behandling)

        val henlagtBehandling = behandling.copy(
            henlagtÅrsak = henlagt.årsak,
            resultat = BehandlingResultat.HENLAGT,
            steg = BEHANDLING_FERDIGSTILT,
            status = FERDIGSTILT
        )

        behandlinghistorikkService.opprettBehandlingshistorikk(behandlingId, BEHANDLING_FERDIGSTILT)
        oppgaveTaskService.lagFerdigstillOppgaveForBehandlingTask(behandling.id)

        behandlingRepository.update(henlagtBehandling)
    }

    fun erLåstForVidereBehandling(behandlingId: UUID) =
        behandlingRepository.findByIdOrThrow(behandlingId).status.erLåstForVidereBehandling()

    private fun validerKanHenleggeBehandling(behandling: Behandling) {
        brukerfeilHvis(behandling.status.erLåstForVidereBehandling()) {
            "Kan ikke henlegge behandling med status ${behandling.status}"
        }
    }
}
