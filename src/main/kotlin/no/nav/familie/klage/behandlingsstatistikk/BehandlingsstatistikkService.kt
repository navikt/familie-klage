package no.nav.familie.klage.behandlingsstatistikk

import no.nav.familie.eksterne.kontrakter.saksstatistikk.klage.BehandlingsstatistikkKlage
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import no.nav.familie.klage.vurdering.VurderingService
import no.nav.familie.klage.vurdering.domain.Vurdering
import no.nav.familie.kontrakter.felles.Regelverk
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

enum class BehandlingsstatistikkHendelse {
    MOTTATT,
    PÅBEGYNT,
    FERDIG,
    SENDT_TIL_KA
}

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
    fun sendBehandlingstatistikk(
        behandlingsId: UUID,
        hendelse: BehandlingsstatistikkHendelse,
        hendelseTidspunkt: LocalDateTime,
        gjeldendeSaksbehandler: String?
    ) {
        val behandlingsstatistikkKlage =
            mapTilBehandlingStatistikkKlage(behandlingsId, hendelse, hendelseTidspunkt, gjeldendeSaksbehandler)
        behandlingsstatistikkProducer.sendBehandlingsstatistikk(behandlingsstatistikkKlage)
    }

    private fun mapTilBehandlingStatistikkKlage(
        behandlingId: UUID,
        hendelse: BehandlingsstatistikkHendelse,
        hendelseTidspunkt: LocalDateTime,
        gjeldendeSaksbehandler: String?
    ): BehandlingsstatistikkKlage {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val vurdering = vurderingService.hentVurdering(behandling.id)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
        val erStrengtFortrolig =
            personopplysningerService.hentPersonopplysninger(behandlingId).adressebeskyttelse?.erStrengtFortrolig()
                ?: false

        val behandlendeEnhet = maskerVerdiHvisStrengtFortrolig(
            erStrengtFortrolig,
            behandling.behandlendeEnhet
        )

        val påklagetVedtakDetaljer = behandling.påklagetVedtak.påklagetVedtakDetaljer

        return BehandlingsstatistikkKlage(
            behandlingId = behandling.eksternBehandlingId,
            personIdent = fagsak.hentAktivIdent(),
            registrertTid = behandling.sporbar.opprettetTid.atZone(zoneIdOslo),
            endretTid = hendelseTidspunkt.atZone(zoneIdOslo),
            tekniskTid = ZonedDateTime.now(zoneIdOslo),
            behandlingType = "KLAGE",
            sakYtelse = fagsak.stønadstype.name,
            fagsystem = fagsak.fagsystem.name,
            relatertEksternBehandlingId = påklagetVedtakDetaljer?.eksternFagsystemBehandlingId,
            relatertFagsystemType = påklagetVedtakDetaljer?.fagsystemType?.name,
            behandlingStatus = hendelse.name,
            opprettetAv = maskerVerdiHvisStrengtFortrolig(erStrengtFortrolig, behandling.sporbar.opprettetAv),
            opprettetEnhet = behandlendeEnhet,
            ansvarligEnhet = behandlendeEnhet,
            mottattTid = behandling.klageMottatt.atStartOfDay(zoneIdOslo),
            ferdigBehandletTid = ferdigBehandletTid(hendelse, hendelseTidspunkt),
            sakUtland = behandling.påklagetVedtak.påklagetVedtakDetaljer?.regelverk.tilDVHSakNasjonalitet(),
            behandlingResultat = behandlingResultat(hendelse, behandling),
            resultatBegrunnelse = resultatBegrunnelse(behandling, vurdering),
            behandlingMetode = "MANUELL",
            saksbehandler = maskerVerdiHvisStrengtFortrolig(
                erStrengtFortrolig,
                gjeldendeSaksbehandler ?: behandling.sporbar.endret.endretAv
            ),
            avsender = "Klage familie",
            saksnummer = fagsak.eksternId
        )
    }

    private fun resultatBegrunnelse(
        behandling: Behandling,
        vurdering: Vurdering?
    ) = if (behandling.resultat == BehandlingResultat.HENLAGT) {
        behandling.henlagtÅrsak?.name
    } else {
        vurdering?.årsak?.name
    }

    private fun behandlingResultat(
        hendelse: BehandlingsstatistikkHendelse,
        behandling: Behandling
    ) = if (hendelse == BehandlingsstatistikkHendelse.FERDIG || hendelse == BehandlingsstatistikkHendelse.SENDT_TIL_KA) {
        behandling.resultat.name
    } else {
        null
    }

    private fun ferdigBehandletTid(
        hendelse: BehandlingsstatistikkHendelse,
        hendelseTidspunkt: LocalDateTime
    ) = if (hendelse == BehandlingsstatistikkHendelse.FERDIG || hendelse == BehandlingsstatistikkHendelse.SENDT_TIL_KA) {
        hendelseTidspunkt.atZone(zoneIdOslo)
    } else {
        null
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

    private fun Regelverk?.tilDVHSakNasjonalitet(): String? = when (this) {
        Regelverk.NASJONAL -> "Nasjonal"
        Regelverk.EØS -> "Utland"
        null -> null
    }
}
