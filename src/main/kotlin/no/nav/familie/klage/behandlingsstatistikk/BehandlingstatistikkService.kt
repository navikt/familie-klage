package no.nav.familie.klage.behandlingsstatistikk

import no.nav.familie.eksterne.kontrakter.saksstatistikk.klage.BehandlingsstatistikkKlage
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.vurdering.VurderingService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

@Service
class BehandlingsstatistikkService(
    private val behandlingsstatistikkProducer: BehandlingsstatistikkProducer,
    private val behandlingService: BehandlingService,
    private val vurderingService: VurderingService,
    private val fagsakService: FagsakService
) {

    private val zoneIdOslo = ZoneId.of("Europe/Oslo")

    @Transactional
    fun sendBehandlingstatistikk(behandlingsId: UUID) {
        val behandlingsstatistikkKlage = mapTilBehandlingStatistikkKlage(behandlingsId)
        behandlingsstatistikkProducer.sendBehandlingsstatistikk(behandlingsstatistikkKlage)
    }

    private fun mapTilBehandlingStatistikkKlage(behandlingId: UUID): BehandlingsstatistikkKlage {

        val behandling = behandlingService.hentBehandling(behandlingId)
        val vurdering = vurderingService.hentVurdering(behandling.id)
        val fagsak = fagsakService.hentFagsakForBehandling(behandling.id)
        //val sisteOppgaveForBehandling = finnSisteOppgaveForBehandlingen(behandlingId, oppgaveId)
        //val vedtak = vedtakRepository.findByIdOrNull(behandlingId)

        val resultatBegrunnelse = vurdering?.beskrivelse ?: ""
        //val søker = grunnlagsdataService.hentGrunnlagsdata(behandlingId).grunnlagsdata.søker
        val henvendelseTidspunkt = behandling.klageMottatt
        //val relatertEksternBehandlingId =
        //    saksbehandling.forrigeBehandlingId?.let { behandlingService.hentBehandling(it).eksternId.id }
        //val erAutomatiskGOmregning = saksbehandling.årsak == BehandlingÅrsak.G_OMREGNING && saksbehandling.opprettetAv == "VL"

        val tekniskTid = ZonedDateTime.now(ZoneId.of("Europe/Oslo"))

        return BehandlingsstatistikkKlage(

            behandlingId = behandlingId,
            personIdent = fagsak.hentAktivIdent(),
            registrertTid = behandling.sporbar.opprettetTid.atZone(zoneIdOslo),
            endretTid = behandling.sporbar.endret.endretTid.atZone(zoneIdOslo),
            tekniskTid = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            sakYtelse = fagsak.stønadstype.name,
            behandlingStatus = behandling.status.name,
            opprettetAv = behandling.sporbar.opprettetAv,
            opprettetEnhet = behandling.behandlendeEnhet,
            ansvarligEnhet = behandling.behandlendeEnhet,
            sakId = fagsak.eksternId.toLong(),
            saksnummer = fagsak.eksternId.toLong(),
            sakUtland = "Nasjonal",
            mottattTid = behandling.klageMottatt.atStartOfDay(zoneIdOslo),



            //  ferdigBehandletTid =
            //vedtakTid =
            //venteAarsak


        )
    }

    private fun maskerVerdiHvisStrengtFortrolig(
        erStrengtFortrolig: Boolean,
        verdi: String
    ): String {
        if (erStrengtFortrolig) {
            return "-5"
        }
        return verdi
    }

    fun String?.isNotNullOrEmpty() = this != null && this.isNotEmpty()