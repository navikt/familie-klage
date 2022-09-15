package no.nav.familie.klage.distribusjon

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.StegService
import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.formkrav.FormService
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.kabal.KabalService
import no.nav.familie.klage.personopplysninger.pdl.logger
import no.nav.familie.klage.vurdering.VurderingService
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class FerdigstillBehandlingService(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val distribusjonService: DistribusjonService,
    private val kabalService: KabalService,
    private val klageresultatService: KlageresultatService,
    private val vurderingService: VurderingService,
    private val formService: FormService,
    private val stegService: StegService
) {

    /**
     * Skal ikke være @transactional fordi det er mulig å komme delvis igjennom løypa
     */
    fun ferdigstillKlagebehandling(behandlingId: UUID) {
        val klageresultat = klageresultatService.hentEllerOpprettKlageresultat(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)

        validerKanFerdigstille(behandling)

        val journalpostId = journalførOgOppdaterKlageresultat(behandlingId, klageresultat)
        distribuerOgOppdaterKlageresultat(journalpostId, behandlingId, klageresultat)

        val steg = if (skalSendeTilKabal(behandlingId)) {
            sendTilKabalOgOppdaterKlageresultat(behandling, klageresultat)
            StegType.OVERFØRING_TIL_KABAL
        } else {
            logger.info("Sender ikke videre til kabal")
            StegType.BEHANDLING_FERDIGSTILT
        }
        stegService.oppdaterSteg(behandlingId, steg)
    }

    private fun sendTilKabalOgOppdaterKlageresultat(behandling: Behandling, klageresultat: Klageresultat) {
        logger.info("Sender klage videre til kabal")
        if (klageresultat.oversendtTilKabalTidspunkt != null) {
            logger.info("Har allerede sendt til kabal")
            return
        }
        val fagsak = fagsakService.hentFagsakForBehandling(behandling.id)
        val vurdering = vurderingService.hentVurdering(behandling.id)

        kabalService.sendTilKabal(fagsak, behandling, vurdering)
        klageresultatService.oppdaterSendtTilKabalTid(
            oversendtTilKabalTidspunkt = LocalDateTime.now(),
            behandlingId = behandling.id
        )
    }

    private fun validerKanFerdigstille(behandling: Behandling) {
        if (behandling.status.erLåstForVidereBehandling()) {
            throw Feil("Kan ikke ferdigstille behandlingen da den er låst for videre behandling")
        }
        if (behandling.steg != StegType.BREV) {
            throw Feil("Kan ikke ferdigstille behandlingen fra steg=${behandling.steg}")
        }
    }

    private fun distribuerOgOppdaterKlageresultat(journalpostId: String, behandlingId: UUID, klageresultat: Klageresultat) {
        if (klageresultat.distribusjonId != null) {
            logger.info("Distribuerer ikke dokument da dette er gjort fra før for behandling=$behandlingId")
        } else {
            val distribusjonId = distribusjonService.distribuerBrev(journalpostId)
            klageresultatService.oppdaterDistribusjonId(distribusjonId = distribusjonId, behandlingId = behandlingId)
        }
    }

    private fun journalførOgOppdaterKlageresultat(behandlingId: UUID, klageresultat: Klageresultat): String {
        if (klageresultat.journalpostId != null) {
            logger.info("Journalfører ikke da dette allerede er gjort for behandling=$behandlingId")
            return klageresultat.journalpostId
        }
        val journalpostId = distribusjonService.journalførBrev(behandlingId)
        klageresultatService.oppdaterJournalpostId(behandlingId = behandlingId, journalpostId = journalpostId)
        return journalpostId
    }

    private fun skalSendeTilKabal(behandlingId: UUID): Boolean {
        return formService.formkravErOppfyltForBehandling(behandlingId) && vurderingService.klageTasIkkeTilFølge(behandlingId)
    }
}
