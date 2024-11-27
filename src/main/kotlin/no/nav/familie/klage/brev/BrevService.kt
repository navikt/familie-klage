package no.nav.familie.klage.brev

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.PåklagetVedtakDetaljer
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandling.domain.erLåstForVidereBehandling
import no.nav.familie.klage.brev.BrevmottakerUtil.validerMinimumEnMottaker
import no.nav.familie.klage.brev.BrevmottakerUtil.validerUnikeBrevmottakere
import no.nav.familie.klage.brev.domain.Brev
import no.nav.familie.klage.brev.domain.BrevmottakerPerson
import no.nav.familie.klage.brev.domain.Brevmottakere
import no.nav.familie.klage.brev.domain.BrevmottakereJournalposter
import no.nav.familie.klage.brev.domain.MottakerRolle
import no.nav.familie.klage.brev.dto.BrevmottakereDto
import no.nav.familie.klage.brev.dto.FritekstBrevRequestDto
import no.nav.familie.klage.brev.dto.tilDomene
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
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
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
    private val vurderingService: VurderingService,
    private val personopplysningerService: PersonopplysningerService,
) {

    fun hentBrev(behandlingId: UUID): Brev = brevRepository.findByIdOrThrow(behandlingId)

    fun hentBrevmottakere(behandlingId: UUID): Brevmottakere {
        val brev = brevRepository.findByIdOrThrow(behandlingId)
        return brev.mottakere ?: Brevmottakere()
    }

    fun settBrevmottakere(behandlingId: UUID, brevmottakere: BrevmottakereDto) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        validerKanLageBrev(behandling)

        val mottakere = brevmottakere.tilDomene()

        validerUnikeBrevmottakere(mottakere)
        validerMinimumEnMottaker(mottakere)

        val brev = brevRepository.findByIdOrThrow(behandlingId)
        brevRepository.update(brev.copy(mottakere = mottakere))
    }

    fun lagBrev(behandlingId: UUID): ByteArray {
        val personopplysninger = personopplysningerService.hentPersonopplysninger(behandlingId)
        val navn = personopplysninger.navn
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
        val påklagetVedtakDetaljer = behandling.påklagetVedtak.påklagetVedtakDetaljer
        validerKanLageBrev(behandling)

        val brevRequest = lagBrevRequest(behandling, fagsak, navn, påklagetVedtakDetaljer, behandling.klageMottatt)

        val signaturMedEnhet = brevsignaturService.lagSignatur(personopplysninger)

        val html = brevClient.genererHtmlFritekstbrev(
            fritekstBrev = brevRequest,
            saksbehandlerNavn = signaturMedEnhet.navn,
            enhet = signaturMedEnhet.enhet,
        )

        lagreEllerOppdaterBrev(
            behandlingId = behandlingId,
            saksbehandlerHtml = html,
            fagsak = fagsak,
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
                BrevInnhold.lagOpprettholdelseBrev(
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
                    PåklagetVedtakstype.UTEN_VEDTAK -> BrevInnhold.lagFormkravAvvistBrevIkkePåklagetVedtak(
                        ident = fagsak.hentAktivIdent(),
                        navn = navn,
                        formkrav = formkrav,
                        stønadstype = fagsak.stønadstype,
                    )
                    else -> BrevInnhold.lagFormkravAvvistBrev(
                        ident = fagsak.hentAktivIdent(),
                        navn = navn,
                        formkrav = formkrav,
                        stønadstype = fagsak.stønadstype,
                        påklagetVedtakDetaljer = påklagetVedtakDetaljer,
                        fagsystem = fagsak.fagsystem
                    )
                }
            }
            BehandlingResultat.MEDHOLD,
            BehandlingResultat.IKKE_SATT,
            BehandlingResultat.HENLAGT,
            -> throw Feil("Kan ikke lage brev for behandling med behandlingResultat=$behandlingResultat")
        }
    }

    fun hentBrevPdf(behandlingId: UUID): ByteArray {
        return brevRepository.findByIdOrThrow(behandlingId).pdf?.bytes
            ?: error("Finner ikke brev-pdf for behandling=$behandlingId")
    }

    private fun lagreEllerOppdaterBrev(
        behandlingId: UUID,
        saksbehandlerHtml: String,
        fagsak: Fagsak,
    ): Brev {
        val brev = brevRepository.findByIdOrNull(behandlingId)
        return if (brev != null) {
            brevRepository.update(brev.copy(saksbehandlerHtml = saksbehandlerHtml))
        } else {
            brevRepository.insert(
                Brev(
                    behandlingId = behandlingId,
                    saksbehandlerHtml = saksbehandlerHtml,
                    mottakere = initialiserBrevmottakere(behandlingId, fagsak),
                ),
            )
        }
    }

    private fun initialiserBrevmottakere(
        behandlingId: UUID,
        fagsak: Fagsak,
    ) = Brevmottakere(
        personer = listOf(
            BrevmottakerPerson(
                personIdent = fagsak.hentAktivIdent(),
                navn = personopplysningerService.hentPersonopplysninger(behandlingId).navn,
                mottakerRolle = MottakerRolle.BRUKER,
            ),
        ),
    )

    fun lagBrevPdf(behandlingId: UUID) {
        val brev = brevRepository.findByIdOrThrow(behandlingId)
        feilHvis(brev.pdf != null) {
            "Det finnes allerede en lagret pdf"
        }

        val generertBrev = familieDokumentClient.genererPdfFraHtml(brev.saksbehandlerHtml)
        brevRepository.update(brev.copy(pdf = Fil(generertBrev)))
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun oppdaterMottakerJournalpost(behandlingId: UUID, brevmottakereJournalposter: BrevmottakereJournalposter) {
        brevRepository.oppdaterMottakerJournalpost(behandlingId, brevmottakereJournalposter)
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
