package no.nav.familie.klage.henlegg

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.brev.BrevClient
import no.nav.familie.klage.fagsak.FagsakService
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
        val trukketKlageDto = objectMapper.readValue<TrukketKlageDto>(task.payload)
        val saksbehandlerIdent = trukketKlageDto.saksbehandlerIdent
        val journalførendeEnhet = behandlingService.hentBehandling(trukketKlageDto.behandlingId).behandlendeEnhet
        val henleggBrev = henleggBehandlingService.genererHenleggelsesbrev(behandlingId = trukketKlageDto.behandlingId, saksbehandlerSignatur = trukketKlageDto.saksbehandlerSignatur)

        val hennleggbrevDto =
            FrittståendeBrevDto(
                personIdent = personopplysningerService.hentPersonopplysninger(trukketKlageDto.behandlingId).personIdent,
                eksternFagsakId = fagsakService.hentFagsak(trukketKlageDto.behandlingId).eksternId.toLong(),
                stønadType = StønadType.SKOLEPENGER, // TODO
                /* TODO brevtype = FrittståendeBrevType.INFORMASJONSBREV_TRUKKET_KlAGE, */
                brevtype = FrittståendeBrevType.INFORMASJONSBREV_TRUKKET_SØKNAD,
                tittel = "Informasjonsbrev - trukket klage",
                fil = henleggBrev,
                journalførendeEnhet = journalførendeEnhet,
                saksbehandlerIdent = saksbehandlerIdent,
                mottakere = lagBrevMottaker(personopplysningerService.hentPersonopplysninger(trukketKlageDto.behandlingId).personIdent, trukketKlageDto.behandlingId),
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
        ): Task =
            Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(TrukketKlageDto(behandlingId, saksbehandlerSignatur, saksbehandlerIdent)),
            )

        const val TYPE = "SendHenleggelsesbrevOmTrukketKlageTask"
    }
}
