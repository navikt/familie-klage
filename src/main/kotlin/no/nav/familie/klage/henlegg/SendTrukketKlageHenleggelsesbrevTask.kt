package no.nav.familie.klage.henlegg

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.brev.BrevClient
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import no.nav.familie.kontrakter.ef.felles.FrittståendeBrevDto
import no.nav.familie.kontrakter.ef.felles.FrittståendeBrevType
import no.nav.familie.kontrakter.ef.iverksett.Brevmottaker
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = SendTrukketKlageHenleggelsesbrevTask.TYPE,
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 15 * 60L,
    beskrivelse = "Send henleggelsesbrev om trukket klage",
)
class SendTrukketKlageHenleggelsesbrevTask(
    private val behandlingService: BehandlingService,
    private val henleggBehandlingService: HenleggBehandlingService,
    private val brevClient: BrevClient,
    private val personopplysningerService: PersonopplysningerService,
    private val fagsakService: FagsakService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val trukketKlageBrevDto = objectMapper.readValue<TrukketKlageBrevDto>(task.payload)
        val saksbehandlerIdent = trukketKlageBrevDto.saksbehandlerIdent
        val journalførendeEnhet = behandlingService.hentBehandling(trukketKlageBrevDto.behandlingId).behandlendeEnhet
        val henleggBrev = henleggBehandlingService.genererHenleggelsesbrev(behandlingId = trukketKlageBrevDto.behandlingId, saksbehandlerSignatur = trukketKlageBrevDto.saksbehandlerSignatur)

        val hennleggbrevDto =
            FrittståendeBrevDto(
                personIdent = personopplysningerService.hentPersonopplysninger(trukketKlageBrevDto.behandlingId).personIdent,
                eksternFagsakId = trukketKlageBrevDto.fagSak.eksternId.toLong(),
                stønadType = StønadType.SKOLEPENGER, // TODO
                /* TODO brevtype = FrittståendeBrevType.INFORMASJONSBREV_TRUKKET_KlAGE, */
                brevtype = FrittståendeBrevType.INFORMASJONSBREV_TRUKKET_SØKNAD,
                tittel = "Informasjonsbrev - trukket klage",
                fil = henleggBrev,
                journalførendeEnhet = journalførendeEnhet,
                saksbehandlerIdent = saksbehandlerIdent,
                mottakere = lagBrevMottaker(personopplysningerService.hentPersonopplysninger(trukketKlageBrevDto.behandlingId).personIdent, trukketKlageBrevDto.behandlingId),
            )
        brevClient.sendFrittståendeBrev(frittståendeBrevDto = hennleggbrevDto)
    }

    private fun lagBrevMottaker(personIdent: String, behandlingId: UUID) =
        listOf(
            Brevmottaker(
                ident = personIdent,
                navn = personopplysningerService.hentPersonopplysninger(behandlingId).navn,
                mottakerRolle = Brevmottaker.MottakerRolle.BRUKER,
                identType = Brevmottaker.IdentType.PERSONIDENT,
            ),
        )

    companion object {
        fun opprettTask(
            behandlingId: UUID,
            saksbehandlerSignatur: String,
            saksbehandlerIdent: String,
            fagsak: Fagsak,
        ): Task =
            Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(TrukketKlageBrevDto(behandlingId, saksbehandlerSignatur, saksbehandlerIdent, fagsak)),
            )

        const val TYPE = "SendHenleggelsesbrevOmTrukketKlageTask"
    }
}
