package no.nav.familie.klage.behandlingsstatistikk

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.eksterne.kontrakter.saksstatistikk.klage.BehandlingsstatistikkKlage
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.domain.PåklagetVedtak
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.fagsak.domain.PersonIdent
import no.nav.familie.klage.felles.domain.Sporbar
import no.nav.familie.klage.integrasjoner.FagsystemVedtakService
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import no.nav.familie.klage.personopplysninger.dto.Adressebeskyttelse
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import no.nav.familie.klage.testutil.DomainUtil.fagsystemVedtak
import no.nav.familie.klage.testutil.DomainUtil.personopplysningerDto
import no.nav.familie.klage.testutil.DomainUtil.vurdering
import no.nav.familie.klage.vurdering.VurderingService
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.Årsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

internal class BehandlingsstatistikkServiceTest {

    private val behandlingsstatistikkProducer = mockk<BehandlingsstatistikkProducer>()
    private val behandlingService = mockk<BehandlingService>()
    private val vurderingService = mockk<VurderingService>()
    private val fagsakService = mockk<FagsakService>()
    private val personopplysningerService = mockk<PersonopplysningerService>()
    private val fagsystemVedtakService = mockk<FagsystemVedtakService>()

    private val service = BehandlingsstatistikkService(
        behandlingsstatistikkProducer = behandlingsstatistikkProducer,
        behandlingService = behandlingService,
        vurderingService = vurderingService,
        fagsakService = fagsakService,
        personopplysningerService = personopplysningerService,
        fagsystemVedtakService = fagsystemVedtakService
    )

    private val behandlingsstatistikkKlageSlot = slot<BehandlingsstatistikkKlage>()

    private val personIdent = UUID.randomUUID().toString()
    private val fagsak = fagsak(setOf(PersonIdent(personIdent)))
    private val påklagetBehandlingId = UUID.randomUUID().toString()
    private val behandling = behandling(
        fagsak,
        påklagetVedtak = PåklagetVedtak(eksternFagsystemBehandlingId = påklagetBehandlingId, PåklagetVedtakstype.VEDTAK),
        resultat = BehandlingResultat.MEDHOLD,
        sporbar = Sporbar(opprettetAv = "Sakbeh")
    )
    private val vurdering = vurdering(behandling.id, årsak = Årsak.FEIL_I_LOVANDVENDELSE)

    private val zoneId = ZoneId.of("Europe/Oslo")

    @BeforeEach
    internal fun setUp() {
        every { fagsakService.hentFagsak(fagsak.id) } returns fagsak
        every { behandlingService.hentBehandling(behandling.id) } returns behandling

        every { vurderingService.hentVurdering(behandling.id) } returns vurdering
        every { personopplysningerService.hentPersonopplysninger(behandling.id) } returns personopplysningerDto(personIdent = personIdent)
        every { fagsystemVedtakService.hentFagsystemVedtak(any<Fagsak>()) } returns listOf(fagsystemVedtak(påklagetBehandlingId))

        justRun { behandlingsstatistikkProducer.sendBehandlingsstatistikk(capture(behandlingsstatistikkKlageSlot)) }
    }

    @Test
    internal fun `skal mappe verdier`() {
        val hendelseTidspunkt = LocalDateTime.now()
        val gjeldendeSaksbehandler = "gjeldendeSaksbehandler"
        val hendelse = BehandlingsstatistikkHendelse.FERDIG
        service.sendBehandlingstatistikk(behandling.id, hendelse, hendelseTidspunkt, gjeldendeSaksbehandler)

        val captured = behandlingsstatistikkKlageSlot.captured

        assertThat(captured.ansvarligEnhet).isEqualTo(behandling.behandlendeEnhet)
        assertThat(captured.avsender).isEqualTo("Klage familie")
        assertThat(captured.behandlingId).isEqualTo(behandling.eksternBehandlingId)
        assertThat(captured.behandlingMetode).isEqualTo("MANUELL")
        assertThat(captured.behandlingResultat).isEqualTo(BehandlingResultat.MEDHOLD.name)
        assertThat(captured.behandlingStatus).isEqualTo(hendelse.name)
        assertThat(captured.behandlingType).isEqualTo("KLAGE")
        assertThat(captured.endretTid).isEqualTo(hendelseTidspunkt.atZone(zoneId))
        assertThat(captured.fagsystem).isEqualTo(fagsak.fagsystem.name)
        assertThat(captured.ferdigBehandletTid).isEqualTo(hendelseTidspunkt.atZone(zoneId))
        assertThat(captured.mottattTid).isEqualTo(behandling.klageMottatt.atStartOfDay(zoneId))
        assertThat(captured.opprettetAv).isEqualTo(behandling.sporbar.opprettetAv)
        assertThat(captured.opprettetEnhet).isEqualTo(behandling.behandlendeEnhet)
        assertThat(captured.personIdent).isEqualTo(personIdent)
        assertThat(captured.registrertTid).isEqualTo(behandling.sporbar.opprettetTid.atZone(zoneId))
        assertThat(captured.relatertEksternBehandlingId).isEqualTo(påklagetBehandlingId)
        assertThat(captured.relatertFagsystemType).isEqualTo("ORDNIÆR")
        assertThat(captured.resultatBegrunnelse).isEqualTo(Årsak.FEIL_I_LOVANDVENDELSE.name)
        assertThat(captured.sakUtland).isEqualTo("Nasjonal")
        assertThat(captured.sakYtelse).isEqualTo(fagsak.stønadstype.name)
        assertThat(captured.saksbehandler).isEqualTo(gjeldendeSaksbehandler)
        assertThat(captured.saksnummer).isEqualTo(fagsak.eksternId)
        assertThat(captured.tekniskTid.toLocalDate()).isEqualTo(LocalDate.now())
    }

    @Test
    internal fun `skal sette saksbehandler og enhet til anonym hvis strengt fortrolig`() {
        val hendelseTidspunkt = LocalDateTime.now()
        val gjeldendeSaksbehandler = "gjeldendeSaksbehandler"
        val hendelse = BehandlingsstatistikkHendelse.FERDIG

        every { personopplysningerService.hentPersonopplysninger(behandling.id) } returns personopplysningerDto(adressebeskyttelse = Adressebeskyttelse.STRENGT_FORTROLIG)

        service.sendBehandlingstatistikk(behandling.id, hendelse, hendelseTidspunkt, gjeldendeSaksbehandler)

        val captured = behandlingsstatistikkKlageSlot.captured
        assertThat(captured.opprettetAv).isEqualTo("-5")
        assertThat(captured.saksbehandler).isEqualTo("-5")
        assertThat(captured.opprettetEnhet).isEqualTo("-5")
        assertThat(captured.opprettetEnhet).isEqualTo("-5")
    }
}
