package no.nav.familie.klage.brev

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandling.domain.erLåstForVidereBehandling
import no.nav.familie.klage.brev.domain.Brev
import no.nav.familie.klage.brev.dto.FritekstBrevRequestDto
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.felles.domain.Fil
import no.nav.familie.klage.formkrav.FormService
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.infrastruktur.exception.feilHvis
import no.nav.familie.klage.repository.findByIdOrThrow
import no.nav.familie.klage.vurdering.VurderingService
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BrevService(
    private val brevClient: BrevClient,
    private val brevRepository: BrevRepository,
    private val behandlingService: BehandlingService,
    private val familieDokumentClient: FamilieDokumentClient,
    private val brevsignaturService: BrevsignaturService,
    private val fagsakService: FagsakService,
    private val formService: FormService,
    private val vurderingService: VurderingService
) {

    fun lagBrev(behandlingId: UUID): ByteArray {
        val navn = behandlingService.hentNavnFraBehandlingsId(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)

        validerKanLageBrev(behandling)

        val brevRequest = lagBrevRequest(behandlingId, fagsak, navn)

        val signaturMedEnhet = brevsignaturService.lagSignatur(behandling.id)

        val html = brevClient.genererHtmlFritekstbrev(
            fritekstBrev = brevRequest,
            saksbehandlerNavn = signaturMedEnhet.navn,
            enhet = signaturMedEnhet.enhet
        )

        lagreEllerOppdaterBrev(
            behandlingId = behandlingId,
            saksbehandlerHtml = html
        )

        return familieDokumentClient.genererPdfFraHtml(html)
    }

    fun slettBrev(behandlingId: UUID) = brevRepository.deleteById(behandlingId)

    private fun validerKanLageBrev(behandling: Behandling) {
        feilHvis(behandling.status.erLåstForVidereBehandling()) {
            "Kan ikke oppdatere brev når behandlingen er låst"
        }
        feilHvis(behandling.steg != StegType.BREV) {
            "Behandlingen er i feil steg (${behandling.steg}) steg=${StegType.BREV} for å kunne oppdatere brevet"
        }
    }

    private fun lagBrevRequest(behandlingId: UUID, fagsak: Fagsak, navn: String): FritekstBrevRequestDto {
        val behandlingResultat = utledBehandlingResultat(behandlingId)
        val vurdering = vurderingService.hentVurdering(behandlingId)

        return when (behandlingResultat) {
            BehandlingResultat.IKKE_MEDHOLD -> {
                val instillingKlageinstans = vurdering?.innstillingKlageinstans
                    ?: throw Feil("Behandling med resultat $behandlingResultat mangler instillingKlageinstans for generering av brev")

                BrevInnhold.lagOpprettholdelseBrev(fagsak.hentAktivIdent(), instillingKlageinstans, navn, fagsak.stønadstype)
            }
            BehandlingResultat.IKKE_MEDHOLD_FORMKRAV_AVVIST -> {
                val begrunnelse = "Begrunnelse for formkrav avvist" // TODO
                BrevInnhold.lagFormkravAvvistBrev(fagsak.hentAktivIdent(), navn, begrunnelse, fagsak.stønadstype)
            }
            BehandlingResultat.MEDHOLD,
            BehandlingResultat.IKKE_SATT,
            BehandlingResultat.HENLAGT -> throw Feil("Kan ikke lage brev for behandling med behandlingResultat=$behandlingResultat")
        }
    }

    fun hentBrevPdf(behandlingId: UUID): ByteArray {
        return brevRepository.findByIdOrThrow(behandlingId).pdf?.bytes
            ?: error("Finner ikke brev-pdf for behandling=$behandlingId")
    }

    private fun lagreEllerOppdaterBrev(
        behandlingId: UUID,
        saksbehandlerHtml: String
    ): Brev {
        val brev = Brev(
            behandlingId = behandlingId,
            saksbehandlerHtml = saksbehandlerHtml
        )

        return when (brevRepository.existsById(brev.behandlingId)) {
            true -> brevRepository.update(brev)
            false -> brevRepository.insert(brev)
        }
    }

    fun lagBrevPdf(behandlingId: UUID) {
        val brev = brevRepository.findByIdOrThrow(behandlingId)
        feilHvis(brev.pdf != null) {
            "Det finnes allerede en lagret pdf"
        }

        val generertBrev = familieDokumentClient.genererPdfFraHtml(brev.saksbehandlerHtml)
        brevRepository.update(brev.copy(pdf = Fil(generertBrev)))
    }

    private fun utledBehandlingResultat(behandlingId: UUID): BehandlingResultat {
        return if (formService.formkravErOppfyltForBehandling(behandlingId)) {
            vurderingService.hentVurdering(behandlingId)?.vedtak?.tilBehandlingResultat()
                ?: throw Feil("Burde funnet behandling $behandlingId")
        } else {
            BehandlingResultat.IKKE_MEDHOLD_FORMKRAV_AVVIST
        }
    }
}
