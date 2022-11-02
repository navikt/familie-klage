package no.nav.familie.klage.behandlingsstatistikk

import no.nav.familie.eksterne.kontrakter.saksstatistikk.klage.BehandlingsstatistikkKlage
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import no.nav.familie.klage.vurdering.VurderingService
import no.nav.familie.kontrakter.ef.iverksett.Hendelse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

@Service
class BehandlingsstatistikkService(
    private val behandlingsstatistikkProducer: BehandlingsstatistikkProducer,
    private val behandlingService: BehandlingService,
    private val vurderingService: VurderingService,
    private val fagsakService: FagsakService,
    private val personopplysningerService: PersonopplysningerService
) {

    private val zoneIdOslo = ZoneId.of("Europe/Oslo")

    @Transactional
    fun sendBehandlingstatistikk(behandlingsId: UUID, hendelse: Hendelse, hendelseTidspunkt: LocalDateTime) {
        val behandlingsstatistikkKlage = mapTilBehandlingStatistikkKlage(behandlingsId, hendelse, hendelseTidspunkt)
        behandlingsstatistikkProducer.sendBehandlingsstatistikk(behandlingsstatistikkKlage)
    }

    private fun mapTilBehandlingStatistikkKlage(behandlingId: UUID, hendelse: Hendelse, hendelseTidspunkt: LocalDateTime): BehandlingsstatistikkKlage {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val vurdering = vurderingService.hentVurdering(behandling.id)
        val fagsak = fagsakService.hentFagsakForBehandling(behandling.id)
        val erStrengtFortrolig = personopplysningerService.hentPersonopplysninger(behandlingId).adressebeskyttelse?.erStrengtFortrolig() ?: false

        return BehandlingsstatistikkKlage(
            behandlingId = behandlingId,
            personIdent = fagsak.hentAktivIdent(),
            registrertTid = behandling.sporbar.opprettetTid.atZone(zoneIdOslo),
            endretTid = hendelseTidspunkt.atZone(zoneIdOslo),
            tekniskTid = ZonedDateTime.now(zoneIdOslo),
            sakYtelse = fagsak.stønadstype.name,
            relatertEksternBehandlingId = behandling.påklagetVedtak.eksternFagsystemBehandlingId,
            behandlingStatus = hendelse.name,
            opprettetAv = behandling.sporbar.opprettetAv,
            opprettetEnhet = maskerVerdiHvisStrengtFortrolig(
                erStrengtFortrolig,
                behandling.behandlendeEnhet
            ),
            ansvarligEnhet = maskerVerdiHvisStrengtFortrolig(
                erStrengtFortrolig,
                behandling.behandlendeEnhet
            ),
            mottattTid = behandling.klageMottatt.atStartOfDay(zoneIdOslo),
            ferdigBehandletTid = if (hendelse == Hendelse.FERDIG) hendelseTidspunkt.atZone(zoneIdOslo) else null,
            vedtakTid = if (hendelse == Hendelse.VEDTATT) hendelseTidspunkt.atZone(zoneIdOslo) else null,
            sakUtland = "Nasjonal",
            behandlingResultat = behandling.resultat.name,
            resultatBegrunnelse = vurdering?.arsak?.name,
            behandlingMetode = "MANUELL",
            saksbehandler = behandling.sporbar.endret.endretAv,
            avsender = "Klage familie",
            saksnummer = fagsak.eksternId
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
}
