package no.nav.familie.klage.henlegg

import no.nav.familie.klage.behandling.BehandlingRepository
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.StegType.BEHANDLING_FERDIGSTILT
import no.nav.familie.klage.behandling.domain.erLåstForVidereBehandling
import no.nav.familie.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.klage.behandlingsstatistikk.BehandlingsstatistikkTask
import no.nav.familie.klage.brev.BrevClient
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.brev.FamilieDokumentClient
import no.nav.familie.klage.distribusjon.JournalførBrevTask
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.felles.domain.SporbarUtils
import no.nav.familie.klage.felles.util.TaskMetadata.saksbehandlerMetadataKey
import no.nav.familie.klage.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.klage.infrastruktur.exception.feilHvis
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.klage.integrasjoner.FagsystemVedtakService
import no.nav.familie.klage.kabal.KlageresultatRepository
import no.nav.familie.klage.oppgave.OppgaveTaskService
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import no.nav.familie.klage.personopplysninger.pdl.secureLogger
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus.FERDIGSTILT
import no.nav.familie.kontrakter.felles.klage.HenlagtÅrsak
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Properties
import java.util.UUID

@Service
class HenleggBehandlingService(
    private val behandlingRepository: BehandlingRepository,
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val klageresultatRepository: KlageresultatRepository,
    private val behandlinghistorikkService: BehandlingshistorikkService,
    private val oppgaveTaskService: OppgaveTaskService,
    private val taskService: TaskService,
    private val fagsystemVedtakService: FagsystemVedtakService,
    private val familieDokumentClient: FamilieDokumentClient,
    private val personopplysningerService: PersonopplysningerService,
    private val brevClient: BrevClient,
    private val brevService: BrevService,
) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun henleggBehandling(behandlingId: UUID, henlagt: HenlagtDto) {
        val behandling = behandlingService.hentBehandling(behandlingId)

        validerKanHenleggeBehandling(behandling)

        val henlagtBehandling = behandling.copy(
            henlagtÅrsak = henlagt.årsak,
            resultat = BehandlingResultat.HENLAGT,
            steg = BEHANDLING_FERDIGSTILT,
            status = FERDIGSTILT,
            vedtakDato = SporbarUtils.now(),
        )

        behandlinghistorikkService.opprettBehandlingshistorikk(
            behandlingId = behandlingId,
            steg = BEHANDLING_FERDIGSTILT,
        )
        oppgaveTaskService.lagFerdigstillOppgaveForBehandlingTask(behandling.id)
        behandlingRepository.update(henlagtBehandling)
        taskService.save(taskService.save(BehandlingsstatistikkTask.opprettFerdigTask(behandlingId = behandlingId)))
    }

    private fun validerKanHenleggeBehandling(behandling: Behandling) {
        brukerfeilHvis(behandling.status.erLåstForVidereBehandling()) {
            "Kan ikke henlegge behandling med status ${behandling.status}"
        }
    }

    fun oppdaterStatusPåBehandling(
        behandlingId: UUID,
        status: BehandlingStatus,
    ): Behandling {
        val behandling = behandlingService.hentBehandling(behandlingId = behandlingId)
        secureLogger.info(
            "${SikkerhetContext.hentSaksbehandler()} endrer status på behandling $behandlingId " +
                "fra ${behandling.status} til $status",
        )
        return behandlingRepository.update(t = behandling.copy(status = status))
    }

    fun opprettJournalførBrevTaskHenlegg(behandlingId: UUID) {
        val html = genererHenleggelsesbrev(behandlingId, SikkerhetContext.hentSaksbehandler(strict = true))
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)

        brevService.lagreEllerOppdaterBrev(
            behandlingId = behandlingId,
            saksbehandlerHtml = html.toString(),
            fagsak = fagsak,
        )

        val journalførBrevTask = Task(
            type = JournalførBrevTask.TYPE,
            payload = behandlingId.toString(),
            properties = Properties().apply {
                this[saksbehandlerMetadataKey] = SikkerhetContext.hentSaksbehandler(strict = true)
            },
        )
        taskService.save(journalførBrevTask)
    }

    private fun validerIkkeSendBrevPåFeilType(henlagt: HenlagtDto) {
        feilHvis(henlagt.skalSendeHenleggelsesbrev && henlagt.årsak == HenlagtÅrsak.FEILREGISTRERT) { "Skal ikke sende brev hvis type er ulik trukket tilbake" }
    }

    fun genererHenleggelsesbrev(
        behandlingId: UUID,
        saksbehandlerSignatur: String,
    ): ByteArray {
        val stønadstype = behandlingService.hentBehandlingDto(behandlingId).stønadstype
        val henleggelsesbrev =
            Henleggelsesbrev(
                lagDemalMedFlettefeltForStønadstype(stønadstype),
                lagNavnOgIdentFlettefelt(behandlingId),
            )

        val html =
            brevClient
                .genererHtml(
                    brevmal = "informasjonsbrevTrukketKlage",
                    saksbehandlerBrevrequest = objectMapper.valueToTree(henleggelsesbrev),
                    saksbehandlersignatur = saksbehandlerSignatur,
                    enhet = "Nav Arbeid og ytelser", /* TODO ?? */
                    skjulBeslutterSignatur = true,
                )

        return familieDokumentClient.genererPdfFraHtml(html)
    }

    private fun lagNavnOgIdentFlettefelt(behandlingId: UUID): Flettefelter {
        val visningsNavn = personopplysningerService.hentPersonopplysninger(behandlingId).navn
        val navnOgIdentFlettefelt = Flettefelter(navn = listOf(visningsNavn), fodselsnummer = listOf(personopplysningerService.hentPersonopplysninger(behandlingId).personIdent))
        return navnOgIdentFlettefelt
    }

    private fun lagDemalMedFlettefeltForStønadstype(stønadstype: Stønadstype) =
        Delmaler(
            listOf(
                Delmal(
                    DelmalFlettefelt(
                        listOf(
                            lagStringForDelmalFlettefelt(stønadstype),
                        ),
                    ),
                ),
            ),
        )

    private fun lagStringForDelmalFlettefelt(stønadstype: Stønadstype): String =
        when (stønadstype) {
            Stønadstype.BARNETILSYN -> "stønad til " + stønadstype.name.lowercase()
            Stønadstype.SKOLEPENGER -> "stønad til " + stønadstype.name.lowercase()
            Stønadstype.BARNETRYGD -> "stønad til " + stønadstype.name.lowercase()
            else -> stønadstype.name.lowercase()
        }

    private data class Flettefelter(
        val navn: List<String>,
        val fodselsnummer: List<String>,
    )

    private data class Henleggelsesbrev(
        val delmaler: Delmaler,
        val flettefelter: Flettefelter,
    )

    private data class Delmal(
        val flettefelter: DelmalFlettefelt,
    )

    private data class Delmaler(
        val stonadstypeKlage: List<Delmal>,
    )

    private data class DelmalFlettefelt(
        val stonadstype: List<String>,
    )
}
