package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype.VEDTAK
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandling.dto.HenlagtDto
import no.nav.familie.klage.behandling.dto.PåklagetVedtakDto
import no.nav.familie.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.formkrav.FormService
import no.nav.familie.klage.formkrav.domain.FormVilkår
import no.nav.familie.klage.formkrav.dto.tilDto
import no.nav.familie.klage.infrastruktur.TestHendelseController
import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.infrastruktur.config.RolleConfig
import no.nav.familie.klage.kabal.event.BehandlingEventService
import no.nav.familie.klage.testutil.BrukerContextUtil.testWithBrukerContext
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.klage.testutil.DomainUtil.vurderingDto
import no.nav.familie.klage.testutil.KabalEventUtil
import no.nav.familie.klage.vurdering.VurderingService
import no.nav.familie.klage.vurdering.domain.Vedtak
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.HenlagtÅrsak
import no.nav.familie.kontrakter.felles.klage.KlageinstansUtfall
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class BehandlingFlytTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var opprettBehandlingService: OpprettBehandlingService

    @Autowired
    private lateinit var formService: FormService

    @Autowired
    private lateinit var vurderingService: VurderingService

    @Autowired
    private lateinit var brevService: BrevService

    @Autowired
    private lateinit var ferdigstillBehandlingService: FerdigstillBehandlingService

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var behandlingshistorikkService: BehandlingshistorikkService

    @Autowired
    private lateinit var rolleConfig: RolleConfig

    @Autowired
    private lateinit var behandlingEventService: BehandlingEventService

    @Nested
    inner class Historikk {

        @Test
        internal fun `OPPRETTHOLD_VEDTAK - når man har sendt brev skal man vente på svar`() {
            val behandlingId = testWithBrukerContext(groups = listOf(rolleConfig.ef.saksbehandler)) {
                val behandlingId = opprettBehandlingService.opprettBehandling(opprettKlagebehandlingRequest)
                formService.oppdaterFormkrav(oppfyltFormDto(behandlingId, påklagetVedtakDto))
                vurderingService.opprettEllerOppdaterVurdering(vurderingDto(behandlingId, Vedtak.OPPRETTHOLD_VEDTAK))
                formService.oppdaterFormkrav(oppfyltFormDto(behandlingId, påklagetVedtakDto))
                vurderingService.opprettEllerOppdaterVurdering(vurderingDto(behandlingId, Vedtak.OPPRETTHOLD_VEDTAK))
                lagEllerOppdaterBrev(behandlingId)
                ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId)
                behandlingId
            }

            val behandlingshistorikk = behandlingshistorikkService.hentBehandlingshistorikk(behandlingId)

            assertThat(behandlingService.hentBehandling(behandlingId).steg).isEqualTo(StegType.KABAL_VENTER_SVAR)
            assertThat(behandlingshistorikk.map { it.steg to it.resultat }).containsExactly(
                StegType.OVERFØRING_TIL_KABAL to null,
                StegType.BREV to null,
                StegType.VURDERING to Vedtak.OPPRETTHOLD_VEDTAK.name,
                StegType.FORMKRAV to FormVilkår.OPPFYLT.name,
                StegType.VURDERING to Vedtak.OPPRETTHOLD_VEDTAK.name,
                StegType.FORMKRAV to FormVilkår.OPPFYLT.name,
                StegType.OPPRETTET to null
            )
        }

        @Test
        internal fun `OPPRETTHOLD_VEDTAK - skal kunne hoppe mellom steg`() {
            val behandlingId = testWithBrukerContext(groups = listOf(rolleConfig.ef.saksbehandler)) {
                val behandlingId = opprettBehandlingService.opprettBehandling(opprettKlagebehandlingRequest)
                formService.oppdaterFormkrav(oppfyltFormDto(behandlingId, påklagetVedtakDto))
                vurderingService.opprettEllerOppdaterVurdering(vurderingDto(behandlingId, Vedtak.OPPRETTHOLD_VEDTAK))

                lagEllerOppdaterBrev(behandlingId)

                formService.oppdaterFormkrav(oppfyltFormDto(behandlingId, påklagetVedtakDto))
                vurderingService.opprettEllerOppdaterVurdering(vurderingDto(behandlingId))

                lagEllerOppdaterBrev(behandlingId)
                ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId)
                behandlingId
            }
            val behandling = behandlingService.hentBehandling(behandlingId)

            behandlingEventService.handleEvent(KabalEventUtil.klagebehandlingAvsluttet(behandling, KlageinstansUtfall.DELVIS_MEDHOLD))

            val behandlingshistorikk = behandlingshistorikkService.hentBehandlingshistorikk(behandlingId)

            assertThat(behandlingService.hentBehandling(behandlingId).steg).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
            assertThat(behandlingshistorikk.map { it.steg to it.resultat }).containsExactly(
                StegType.BEHANDLING_FERDIGSTILT to KlageinstansUtfall.DELVIS_MEDHOLD.name,
                StegType.KABAL_VENTER_SVAR to null,
                StegType.OVERFØRING_TIL_KABAL to null,
                StegType.BREV to null,
                StegType.VURDERING to Vedtak.OPPRETTHOLD_VEDTAK.name,
                StegType.FORMKRAV to FormVilkår.OPPFYLT.name,
                StegType.VURDERING to Vedtak.OPPRETTHOLD_VEDTAK.name,
                StegType.FORMKRAV to FormVilkår.OPPFYLT.name,
                StegType.OPPRETTET to null
            )
        }

        @Test
        internal fun `OMGJØR_VEDTAK - når man har ferdigstilt klagebehandling skal man vente på svar`() {
            val behandlingId = testWithBrukerContext(groups = listOf(rolleConfig.ef.saksbehandler)) {
                val behandlingId = opprettBehandlingService.opprettBehandling(opprettKlagebehandlingRequest)
                formService.oppdaterFormkrav(oppfyltFormDto(behandlingId))
                vurderingService.opprettEllerOppdaterVurdering(
                    vurderingDto(
                        behandlingId = behandlingId,
                        vedtak = Vedtak.OMGJØR_VEDTAK,
                        begrunnelseOmgjøring = "begrunnelse"
                    )
                )
                ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId)
                behandlingId
            }

            val behandlingshistorikk = behandlingshistorikkService.hentBehandlingshistorikk(behandlingId)

            assertThat(behandlingService.hentBehandling(behandlingId).steg).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
            assertThat(behandlingshistorikk.map { it.steg to it.resultat }).containsExactly(
                StegType.BEHANDLING_FERDIGSTILT to BehandlingResultat.MEDHOLD.name,
                StegType.VURDERING to Vedtak.OMGJØR_VEDTAK.name,
                StegType.FORMKRAV to FormVilkår.OPPFYLT.name,
                StegType.OPPRETTET to null
            )
        }

        @Test
        internal fun `Ikke oppfylt formkrav skal ikke vurderes`() {
            val behandlingId = testWithBrukerContext(groups = listOf(rolleConfig.ef.saksbehandler)) {
                val behandlingId = opprettBehandlingService.opprettBehandling(opprettKlagebehandlingRequest)
                formService.oppdaterFormkrav(ikkeOppfyltFormDto(behandlingId))
                lagEllerOppdaterBrev(behandlingId)
                ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId)
                behandlingId
            }

            val behandlingshistorikk = behandlingshistorikkService.hentBehandlingshistorikk(behandlingId)

            assertThat(behandlingService.hentBehandling(behandlingId).steg).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
            assertThat(behandlingshistorikk.map { it.steg to it.resultat }).containsExactly(
                StegType.BEHANDLING_FERDIGSTILT to BehandlingResultat.IKKE_MEDHOLD_FORMKRAV_AVVIST.name,
                StegType.BREV to null,
                StegType.FORMKRAV to FormVilkår.IKKE_OPPFYLT.name,
                StegType.OPPRETTET to null
            )
        }

        @Test
        internal fun `henlegger behandling`() {
            val behandlingId = testWithBrukerContext(groups = listOf(rolleConfig.ef.saksbehandler)) {
                val behandlingId = opprettBehandlingService.opprettBehandling(opprettKlagebehandlingRequest)
                behandlingService.henleggBehandling(behandlingId, HenlagtDto(HenlagtÅrsak.FEILREGISTRERT)) // TODO få inn detaljer i historikk?
                behandlingId
            }

            val behandlingshistorikk = behandlingshistorikkService.hentBehandlingshistorikk(behandlingId)

            assertThat(behandlingService.hentBehandling(behandlingId).steg).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
            assertThat(behandlingshistorikk.map { it.steg to it.resultat }).containsExactly(
                StegType.BEHANDLING_FERDIGSTILT to BehandlingResultat.HENLAGT.name,
                StegType.OPPRETTET to null
            )
        }

        @Test
        internal fun `ikke ferdigutfylde krav`() {
            val behandlingId = testWithBrukerContext(groups = listOf(rolleConfig.ef.saksbehandler)) {
                val behandlingId = opprettBehandlingService.opprettBehandling(opprettKlagebehandlingRequest)
                formService.oppdaterFormkrav(oppfyltFormDto(behandlingId).copy(klagePart = FormVilkår.IKKE_SATT))
                behandlingId
            }

            val behandlingshistorikk = behandlingshistorikkService.hentBehandlingshistorikk(behandlingId)

            assertThat(behandlingService.hentBehandling(behandlingId).steg).isEqualTo(StegType.FORMKRAV)
            assertThat(behandlingshistorikk.map { it.steg to it.resultat }).containsExactly(
                StegType.FORMKRAV to FormVilkår.IKKE_SATT.name,
                StegType.OPPRETTET to null
            )
        }

        private fun lagEllerOppdaterBrev(behandlingId: UUID) {
            brevService.lagBrev(behandlingId)
        }
    }

    private val påklagetVedtakDto = PåklagetVedtakDto(eksternFagsystemBehandlingId = "123", VEDTAK)

    private val opprettKlagebehandlingRequest =
        OpprettKlagebehandlingRequest(
            "ident",
            Stønadstype.OVERGANGSSTØNAD,
            UUID.randomUUID().toString(),
            Fagsystem.EF,
            LocalDate.now(),
            "enhet"
        )

    private fun oppfyltFormDto(behandlingId: UUID, påklagetVedtakDto: PåklagetVedtakDto = DomainUtil.påklagetVedtakDto()) =
        DomainUtil.oppfyltForm(behandlingId).tilDto(påklagetVedtakDto)

    private fun ikkeOppfyltFormDto(behandlingId: UUID) =
        DomainUtil.oppfyltForm(behandlingId).tilDto(DomainUtil.påklagetVedtakDto()).copy(
            klagePart = FormVilkår.IKKE_OPPFYLT,
            saksbehandlerBegrunnelse = "Ok",
            brevtekst = "brevtekst"
        )
}
