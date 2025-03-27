package no.nav.familie.klage.brev

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.PåklagetVedtakDetaljer
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandling.domain.erLåstForVidereBehandling
import no.nav.familie.klage.brev.domain.Brev
import no.nav.familie.klage.brev.domain.BrevmottakereJournalposter
import no.nav.familie.klage.brev.dto.Delmal
import no.nav.familie.klage.brev.dto.DelmalFlettefelt
import no.nav.familie.klage.brev.dto.Delmaler
import no.nav.familie.klage.brev.dto.Flettefelter
import no.nav.familie.klage.brev.dto.FritekstBrevRequestDto
import no.nav.familie.klage.brev.dto.Henleggelsesbrev
import no.nav.familie.klage.brev.dto.SignaturDto
import no.nav.familie.klage.brevmottaker.BrevmottakerUtil.validerMinimumEnMottaker
import no.nav.familie.klage.brevmottaker.BrevmottakerUtil.validerUnikeBrevmottakere
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonMedIdent
import no.nav.familie.klage.brevmottaker.domain.Brevmottakere
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle
import no.nav.familie.klage.brevmottaker.dto.BrevmottakereDto
import no.nav.familie.klage.brevmottaker.dto.tilDomene
import no.nav.familie.klage.distribusjon.JournalførBrevTask
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.felles.domain.Fil
import no.nav.familie.klage.felles.util.StønadstypeVisningsnavn.visningsnavn
import no.nav.familie.klage.felles.util.TaskMetadata.saksbehandlerMetadataKey
import no.nav.familie.klage.felles.util.isEqualOrAfter
import no.nav.familie.klage.formkrav.FormService
import no.nav.familie.klage.henlegg.HenlagtDto
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.klage.infrastruktur.exception.feilHvis
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import no.nav.familie.klage.repository.findByIdOrThrow
import no.nav.familie.klage.vurdering.VurderingService
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.HenlagtÅrsak
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

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
    private val brevInnholdUtleder: BrevInnholdUtleder,
    private val taskService: TaskService,
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

        val signaturMedEnhet = brevsignaturService.lagSignatur(personopplysninger, fagsak.fagsystem)

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
                brukerfeilHvis(påklagetVedtakDetaljer == null) {
                    "Kan ikke opprette brev til klageinstansen når det ikke er valgt et påklaget vedtak"
                }
                if (fagsak.fagsystem == Fagsystem.EF) {
                    val instillingKlageinstans = vurdering?.innstillingKlageinstans
                        ?: throw Feil("Behandling med resultat $behandlingResultat mangler instillingKlageinstans for generering av brev")
                    brevInnholdUtleder.lagOpprettholdelseBrev(
                        ident = fagsak.hentAktivIdent(),
                        instillingKlageinstans = instillingKlageinstans,
                        navn = navn,
                        stønadstype = fagsak.stønadstype,
                        påklagetVedtakDetaljer = påklagetVedtakDetaljer,
                        klageMottatt = klageMottatt,
                    )
                } else {
                    fun getOrThrow(verdi: String?, felt: String) = verdi
                        ?: throw Feil("Behandling med resultat $behandlingResultat mangler $felt for generering av brev")

                    val dokumentasjonOgUtredning = getOrThrow(vurdering?.dokumentasjonOgUtredning, "dokumentasjonOgUtredning")
                    val spørsmåletISaken = getOrThrow(vurdering?.spørsmåletISaken, "spørsmåletISaken")
                    val aktuelleRettskilder = getOrThrow(vurdering?.aktuelleRettskilder, "aktuelleRettskilder")
                    val klagersAnførsler = getOrThrow(vurdering?.klagersAnførsler, "klagersAnførsler")
                    val vurderingAvKlagen = getOrThrow(vurdering?.vurderingAvKlagen, "vurderingAvKlagen")

                    brevInnholdUtleder.lagOpprettholdelseBrev(
                        ident = fagsak.hentAktivIdent(),
                        dokumentasjonOgUtredning = dokumentasjonOgUtredning,
                        spørsmåletISaken = spørsmåletISaken,
                        aktuelleRettskilder = aktuelleRettskilder,
                        klagersAnførsler = klagersAnførsler,
                        vurderingAvKlagen = vurderingAvKlagen,
                        navn = navn,
                        stønadstype = fagsak.stønadstype,
                        påklagetVedtakDetaljer = påklagetVedtakDetaljer,
                        klageMottatt = klageMottatt,
                    )
                }
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

    fun hentBrevPdf(behandlingId: UUID): ByteArray {
        return brevRepository.findByIdOrThrow(behandlingId).pdf?.bytes
            ?: error("Finner ikke brev-pdf for behandling=$behandlingId")
    }

    fun lagreEllerOppdaterBrev(
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
            BrevmottakerPersonMedIdent(
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

    fun opprettJournalførHenleggelsesbrevTask(behandlingId: UUID, henlagt: HenlagtDto) {
        validerIkkeSendTrukketKlageBrevPåFeilType(henlagt)
        validerIkkeSendTrukketKlageBrevHvisVergemålEllerFullmakt(behandlingId)
        val html = lagHenleggelsesbrevHtml(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)

        lagreEllerOppdaterBrev(
            behandlingId = behandlingId,
            saksbehandlerHtml = html,
            fagsak = fagsak,
        )

        lagBrevPdf(behandlingId)

        val journalførBrevTask = Task(
            type = JournalførBrevTask.TYPE,
            payload = behandlingId.toString(),
            properties = Properties().apply {
                this[saksbehandlerMetadataKey] = SikkerhetContext.hentSaksbehandler(strict = true)
                this["eksterFagsakId"] = fagsak.eksternId
                this["fagsystem"] = fagsak.fagsystem.name
            },
        )
        taskService.save(journalførBrevTask)
    }

    fun genererHenleggelsesbrev(
        behandlingId: UUID,
    ): ByteArray {
        val html =
            lagHenleggelsesbrevHtml(behandlingId)

        return familieDokumentClient.genererPdfFraHtml(html)
    }

    private fun lagHenleggelsesbrevHtml(behandlingId: UUID): String {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
        val personopplysninger = personopplysningerService.hentPersonopplysninger(behandlingId)
        val signaturMedEnhet = brevsignaturService.lagSignatur(personopplysninger, fagsak.fagsystem)
        val stønadstype = fagsak.stønadstype

        val html = when (stønadstype) {
            Stønadstype.BARNETRYGD,
            Stønadstype.KONTANTSTØTTE,
            -> lagHenleggelsesbrevHtmlBaks(signaturMedEnhet, personopplysninger.navn, fagsak)

            Stønadstype.OVERGANGSSTØNAD,
            Stønadstype.BARNETILSYN,
            Stønadstype.SKOLEPENGER,
            -> lagHenleggelsesbrevHtmlEf(behandlingId, signaturMedEnhet, fagsak)
        }

        return html
    }

    private fun lagHenleggelsesbrevHtmlEf(
        behandlingId: UUID,
        signaturMedEnhet: SignaturDto,
        fagsak: Fagsak,
    ): String {
        val henleggelsesbrev = lagHenleggelsesbrevRequest(behandlingId)

        val html =
            brevClient
                .genererHtml(
                    brevmal = "informasjonsbrevTrukketKlage",
                    saksbehandlerBrevrequest = objectMapper.valueToTree(henleggelsesbrev),
                    saksbehandlersignatur = signaturMedEnhet.navn,
                    enhet = signaturMedEnhet.enhet,
                    skjulBeslutterSignatur = true,
                    stønadstype = fagsak.stønadstype,
                )
        return html
    }

    private fun lagHenleggelsesbrevHtmlBaks(
        signaturMedEnhet: SignaturDto,
        navn: String,
        fagsak: Fagsak,
    ): String {
        val henleggelsesbrevInnhold = brevInnholdUtleder.lagHenleggelsesbrevBaksInnhold(
            ident = fagsak.hentAktivIdent(),
            navn = navn,
            stønadstype = fagsak.stønadstype,
        )

        return brevClient.genererHtmlFritekstbrev(
            fritekstBrev = henleggelsesbrevInnhold,
            saksbehandlerNavn = signaturMedEnhet.navn,
            enhet = signaturMedEnhet.enhet,
        )
    }

    private fun lagHenleggelsesbrevRequest(behandlingId: UUID): Henleggelsesbrev {
        val stønadstype = behandlingService.hentBehandlingDto(behandlingId).stønadstype
        return Henleggelsesbrev(
            lagDemalMedFlettefeltForStønadstype(stønadstype),
            lagNavnOgIdentFlettefelt(behandlingId),
        )
    }

    private fun lagNavnOgIdentFlettefelt(behandlingId: UUID): Flettefelter {
        val visningsNavn = personopplysningerService.hentPersonopplysninger(behandlingId).navn
        val navnOgIdentFlettefelt = Flettefelter(
            navn = listOf(visningsNavn),
            fodselsnummer = listOf(personopplysningerService.hentPersonopplysninger(behandlingId).personIdent),
        )
        return navnOgIdentFlettefelt
    }

    private fun lagDemalMedFlettefeltForStønadstype(stønadstype: Stønadstype) =
        Delmaler(
            listOf(
                Delmal(
                    DelmalFlettefelt(
                        listOf(
                            stønadstype.visningsnavn(),
                        ),
                    ),
                ),
            ),
        )

    private fun validerIkkeSendTrukketKlageBrevPåFeilType(henlagt: HenlagtDto) {
        feilHvis(henlagt.skalSendeHenleggelsesbrev && henlagt.årsak == HenlagtÅrsak.FEILREGISTRERT) {
            "Skal ikke sende brev hvis type er ulik trukket tilbake"
        }
    }

    private fun validerIkkeSendTrukketKlageBrevHvisVergemålEllerFullmakt(
        ident: UUID,
    ) {
        val personopplysninger = personopplysningerService.hentPersonopplysninger(ident)
        val harVerge = personopplysninger.vergemål.isNotEmpty()
        val harFullmakt: Boolean =
            personopplysninger.fullmakt
                .filter {
                    it.gyldigTilOgMed == null ||
                        (
                            it.gyldigTilOgMed.isEqualOrAfter(
                                LocalDate.now(),
                            )
                            )
                }.isNotEmpty()
        feilHvis(harVerge || harFullmakt) {
            "Skal ikke sende brev hvis person er tilknyttet vergemål eller fullmakt"
        }
    }
}
