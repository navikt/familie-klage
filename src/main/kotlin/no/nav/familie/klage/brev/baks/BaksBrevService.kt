package no.nav.familie.klage.brev.baks

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.PåklagetVedtakDetaljer
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandling.domain.erLåstForVidereBehandling
import no.nav.familie.klage.brev.BrevClient
import no.nav.familie.klage.brev.BrevInnholdUtleder
import no.nav.familie.klage.brev.BrevsignaturService
import no.nav.familie.klage.brev.FamilieDokumentClient
import no.nav.familie.klage.brev.FritekstBrevRequestDto
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.felles.domain.Fil
import no.nav.familie.klage.formkrav.FormService
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.klage.infrastruktur.exception.feilHvis
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import no.nav.familie.klage.repository.findByIdOrThrow
import no.nav.familie.klage.vurdering.VurderingService
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class BaksBrevService(
    private val brevClient: BrevClient,
    private val baksBrevRepository: BaksBrevRepository,
    private val behandlingService: BehandlingService,
    private val familieDokumentClient: FamilieDokumentClient,
    private val brevsignaturService: BrevsignaturService,
    private val fagsakService: FagsakService,
    private val formService: FormService,
    private val vurderingService: VurderingService,
    private val personopplysningerService: PersonopplysningerService,
    private val brevInnholdUtleder: BrevInnholdUtleder,
) {
    fun hentBrev(behandlingId: UUID): BaksBrev = baksBrevRepository.findByIdOrThrow(behandlingId)

    fun lagBrev(behandlingId: UUID): ByteArray {
        val personopplysninger = personopplysningerService.hentPersonopplysninger(behandlingId)
        val navn = personopplysninger.navn
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
        val påklagetVedtakDetaljer = behandling.påklagetVedtak.påklagetVedtakDetaljer
        validerKanLageBrev(behandling)

        val brevRequest = lagBrevRequest(behandling, fagsak, navn, påklagetVedtakDetaljer, behandling.klageMottatt)

        val signaturMedEnhet = brevsignaturService.lagSignatur(personopplysninger, fagsak.fagsystem)

        val html = brevClient.genererHtmlFritekstbrev(
            fritekstBrev = brevRequest,
            saksbehandlerNavn = signaturMedEnhet.navn,
            enhet = signaturMedEnhet.enhet,
        )

        lagreEllerOppdaterBrev(
            behandlingId = behandlingId,
            saksbehandlerHtml = html,
        )

        return familieDokumentClient.genererPdfFraHtml(html)
    }

    private fun validerKanLageBrev(behandling: Behandling) {
        feilHvis(behandling.status.erLåstForVidereBehandling()) {
            "Kan ikke oppdatere brev når behandlingen er låst"
        }
        feilHvis(behandling.steg != StegType.BREV) {
            "Behandlingen er i feil steg (${behandling.steg}) steg=${StegType.BREV} for å kunne oppdatere brevet"
        }
    }

    private fun lagBrevRequest(
        behandling: Behandling,
        fagsak: Fagsak,
        navn: String,
        påklagetVedtakDetaljer: PåklagetVedtakDetaljer?,
        klageMottatt: LocalDate,
    ): FritekstBrevRequestDto {
        val behandlingResultat = utledBehandlingResultat(behandling.id)
        val vurdering = vurderingService.hentVurdering(behandling.id)

        return when (behandlingResultat) {
            BehandlingResultat.IKKE_MEDHOLD -> {
                val instillingKlageinstans = vurdering?.innstillingKlageinstans
                    ?: throw Feil("Behandling med resultat $behandlingResultat mangler instillingKlageinstans for generering av brev")
                brukerfeilHvis(påklagetVedtakDetaljer == null) {
                    "Kan ikke opprette brev til klageinstansen når det ikke er valgt et påklaget vedtak"
                }
                brevInnholdUtleder.lagOpprettholdelseBrev(
                    ident = fagsak.hentAktivIdent(),
                    instillingKlageinstans = instillingKlageinstans,
                    navn = navn,
                    stønadstype = fagsak.stønadstype,
                    påklagetVedtakDetaljer = påklagetVedtakDetaljer,
                    klageMottatt = klageMottatt,
                )
            }

            BehandlingResultat.IKKE_MEDHOLD_FORMKRAV_AVVIST -> {
                val formkrav = formService.hentForm(behandling.id)
                return when (behandling.påklagetVedtak.påklagetVedtakstype) {
                    PåklagetVedtakstype.UTEN_VEDTAK -> brevInnholdUtleder.lagFormkravAvvistBrevIkkePåklagetVedtak(
                        ident = fagsak.hentAktivIdent(),
                        navn = navn,
                        formkrav = formkrav,
                        stønadstype = fagsak.stønadstype,
                    )

                    else -> brevInnholdUtleder.lagFormkravAvvistBrev(
                        ident = fagsak.hentAktivIdent(),
                        navn = navn,
                        form = formkrav,
                        stønadstype = fagsak.stønadstype,
                        påklagetVedtakDetaljer = påklagetVedtakDetaljer,
                        fagsystem = fagsak.fagsystem,
                    )
                }
            }

            BehandlingResultat.MEDHOLD,
            BehandlingResultat.IKKE_SATT,
            BehandlingResultat.HENLAGT,
            -> throw Feil("Kan ikke lage brev for behandling med behandlingResultat=$behandlingResultat")
        }
    }

    private fun lagreEllerOppdaterBrev(
        behandlingId: UUID,
        saksbehandlerHtml: String,
    ): BaksBrev {
        val brev = baksBrevRepository.findByIdOrNull(behandlingId)
        return if (brev != null) {
            baksBrevRepository.update(brev.copy(saksbehandlerHtml = saksbehandlerHtml))
        } else {
            baksBrevRepository.insert(
                BaksBrev(
                    behandlingId = behandlingId,
                    saksbehandlerHtml = saksbehandlerHtml,
                ),
            )
        }
    }

    fun lagBrevPdf(behandlingId: UUID) {
        val brev = baksBrevRepository.findByIdOrThrow(behandlingId)
        feilHvis(brev.pdf != null) {
            "Det finnes allerede en lagret pdf"
        }

        val generertBrev = familieDokumentClient.genererPdfFraHtml(brev.saksbehandlerHtml)
        baksBrevRepository.update(brev.copy(pdf = Fil(generertBrev)))
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
