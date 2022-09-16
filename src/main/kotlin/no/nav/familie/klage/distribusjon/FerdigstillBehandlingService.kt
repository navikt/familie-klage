package no.nav.familie.klage.distribusjon

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.StegService
import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.BehandlingResultat
import no.nav.familie.klage.behandling.domain.BehandlingResultat.IKKE_MEDHOLD
import no.nav.familie.klage.behandling.domain.BehandlingResultat.IKKE_SATT
import no.nav.familie.klage.behandling.domain.BehandlingResultat.MEDHOLD
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
    private val distribusjonResultatService: DistribusjonResultatService,
    private val vurderingService: VurderingService,
    private val formService: FormService,
    private val stegService: StegService
) {

    /**
     * Skal ikke være @transactional fordi det er mulig å komme delvis igjennom løypa
     */
    fun ferdigstillKlagebehandling(behandlingId: UUID) {
        val distribusjonResultat = distribusjonResultatService.hentEllerOpprettDistribusjonResultat(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)
        val behandlingsresultat = utledBehandlingResultat(behandlingId)

        validerKanFerdigstille(behandling)

        val journalpostId = journalførOgOppdaterResultat(behandlingId, distribusjonResultat)
        distribuerOgOppdaterResultat(journalpostId, behandlingId, distribusjonResultat)

        sendTilKabalOgOppdaterResultat(behandling, distribusjonResultat, behandlingsresultat)
        behandlingService.oppdaterBehandlingsresultatOgVedtaksdato(behandlingId, behandlingsresultat)
        stegService.oppdaterSteg(behandlingId, stegForResultat(behandlingsresultat))
    }

    private fun stegForResultat(resultat: BehandlingResultat): StegType = when (resultat) {
        IKKE_MEDHOLD -> StegType.OVERFØRING_TIL_KABAL
        MEDHOLD -> StegType.BEHANDLING_FERDIGSTILT // TODO: Legg inn IKKE_MEDHOLD_FORMALKRAV_AVVIST,
        IKKE_SATT -> error("Kan ikke utlede neste steg når behandlingsresultatet er IKKE_SATT")
    }

    private fun sendTilKabalOgOppdaterResultat(
        behandling: Behandling,
        distribusjonResultat: DistribusjonResultat,
        behandlingsresultat: BehandlingResultat
    ) {
        if (behandlingsresultat != IKKE_MEDHOLD) {
            logger.info("Skal ikke sende til kabal siden formkrav ikke er oppfylt eller saksbehandler har gitt medhold")
            return
        }
        if (distribusjonResultat.oversendtTilKabalTidspunkt != null) {
            logger.info("Har allerede sendt til kabal")
            return
        }
        logger.info("Sender klage videre til kabal")
        val fagsak = fagsakService.hentFagsakForBehandling(behandling.id)
        val vurdering =
            vurderingService.hentVurdering(behandling.id) ?: error("Mangler vurdering på klagen - kan ikke oversendes til kabal")
        kabalService.sendTilKabal(fagsak, behandling, vurdering)
        distribusjonResultatService.oppdaterSendtTilKabalTid(
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

    private fun distribuerOgOppdaterResultat(journalpostId: String, behandlingId: UUID, distribusjonResultat: DistribusjonResultat) {
        if (distribusjonResultat.brevDistribusjonId != null) {
            logger.info("Distribuerer ikke dokument da dette er gjort fra før for behandling=$behandlingId")
        } else {
            val brevDistribusjonId = distribusjonService.distribuerBrev(journalpostId)
            distribusjonResultatService.oppdaterBrevDistribusjonId(brevDistribusjonId = brevDistribusjonId, behandlingId = behandlingId)
        }
    }

    private fun journalførOgOppdaterResultat(behandlingId: UUID, distribusjonResultat: DistribusjonResultat): String {
        if (distribusjonResultat.journalpostId != null) {
            logger.info("Journalfører ikke da dette allerede er gjort for behandling=$behandlingId")
            return distribusjonResultat.journalpostId
        }
        val journalpostId = distribusjonService.journalførBrev(behandlingId)
        distribusjonResultatService.oppdaterJournalpostId(behandlingId = behandlingId, journalpostId = journalpostId)
        return journalpostId
    }

    private fun utledBehandlingResultat(behandlingId: UUID): BehandlingResultat {
        return if (formService.formkravErOppfyltForBehandling(behandlingId)) {
            if (vurderingService.klageTasIkkeTilFølge(behandlingId)) {
                IKKE_MEDHOLD
            } else {
                MEDHOLD
            }
        } else {
            IKKE_SATT // TODO: Bruk IKKE_MEDHOLD_FORMKRAV_AVVIST
        }
    }
}
