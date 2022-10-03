package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.brev.dto.FritekstBrevtype
import no.nav.familie.klage.distribusjon.FerdigstillBehandlingService
import no.nav.familie.klage.formkrav.FormService
import no.nav.familie.klage.formkrav.domain.FormVilkår
import no.nav.familie.klage.formkrav.dto.tilDto
import no.nav.familie.klage.infrastruktur.TestHendelseController
import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.infrastruktur.config.RolleConfig
import no.nav.familie.klage.testutil.BrukerContextUtil.testWithBrukerContext
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.klage.testutil.DomainUtil.vurderingDto
import no.nav.familie.klage.vurdering.VurderingService
import no.nav.familie.klage.vurdering.domain.Vedtak
import no.nav.familie.kontrakter.felles.klage.Fagsystem
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
    private lateinit var testHendelseController: TestHendelseController

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var behandlingshistorikkService: BehandlingshistorikkService

    @Autowired
    private lateinit var rolleConfig: RolleConfig

    @Nested
    inner class Historikk {

        @Test
        internal fun `OPPRETTHOLD_VEDTAK - når man sendt brev skal man vente på svar`() {
            val behandlingId = testWithBrukerContext(groups = listOf(rolleConfig.ef.saksbehandler)) {
                val behandlingId = opprettBehandlingService.opprettBehandling(opprettKlagebehandlingRequest)
                formService.oppdaterForm(oppfyltFormDto(behandlingId))
                vurderingService.opprettEllerOppdaterVurdering(vurderingDto(behandlingId, Vedtak.OPPRETTHOLD_VEDTAK))
                formService.oppdaterForm(oppfyltFormDto(behandlingId))
                vurderingService.opprettEllerOppdaterVurdering(vurderingDto(behandlingId, Vedtak.OPPRETTHOLD_VEDTAK))
                brevService.lagEllerOppdaterBrev(behandlingId, "", "", FritekstBrevtype.VEDTAK_AVSLAG)
                ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId)
                behandlingId
            }

            val behandlingshistorikk = behandlingshistorikkService.hentBehandlingshistorikker(behandlingId)

            assertThat(behandlingService.hentBehandling(behandlingId).steg).isEqualTo(StegType.KABAL_VENTER_SVAR)
            assertThat(behandlingshistorikk.map { it.steg }).containsExactly(
                StegType.OVERFØRING_TIL_KABAL,
                StegType.BREV,
                StegType.VURDERING,
                StegType.FORMKRAV,
                StegType.VURDERING,
                StegType.FORMKRAV
            )
        }

        @Test
        internal fun `OPPRETTHOLD_VEDTAK - skal kunne hoppe mellom steg`() {
            val behandlingId = testWithBrukerContext(groups = listOf(rolleConfig.ef.saksbehandler)) {
                val behandlingId = opprettBehandlingService.opprettBehandling(opprettKlagebehandlingRequest)
                formService.oppdaterForm(oppfyltFormDto(behandlingId))
                vurderingService.opprettEllerOppdaterVurdering(vurderingDto(behandlingId, Vedtak.OPPRETTHOLD_VEDTAK))

                brevService.lagEllerOppdaterBrev(behandlingId, "", "", FritekstBrevtype.VEDTAK_AVSLAG)

                formService.oppdaterForm(oppfyltFormDto(behandlingId))
                vurderingService.opprettEllerOppdaterVurdering(vurderingDto(behandlingId))

                brevService.lagEllerOppdaterBrev(behandlingId, "", "", FritekstBrevtype.VEDTAK_AVSLAG)
                ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId)
                behandlingId
            }

            testHendelseController.opprettDummyKabalEvent(behandlingId)

            val behandlingshistorikk = behandlingshistorikkService.hentBehandlingshistorikker(behandlingId)

            assertThat(behandlingService.hentBehandling(behandlingId).steg).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
            assertThat(behandlingshistorikk.map { it.steg }).containsExactly(
                StegType.BEHANDLING_FERDIGSTILT,
                StegType.KABAL_VENTER_SVAR,
                StegType.OVERFØRING_TIL_KABAL,
                StegType.BREV,
                StegType.VURDERING,
                StegType.FORMKRAV,
                StegType.VURDERING,
                StegType.FORMKRAV
            )
        }

        @Test
        internal fun `OMGJØR_VEDTAK - når man ferdigstilt klagebehandling skal man vente på svar`() {
            val behandlingId = testWithBrukerContext(groups = listOf(rolleConfig.ef.saksbehandler)) {
                val behandlingId = opprettBehandlingService.opprettBehandling(opprettKlagebehandlingRequest)
                formService.oppdaterForm(oppfyltFormDto(behandlingId))
                vurderingService.opprettEllerOppdaterVurdering(vurderingDto(behandlingId, Vedtak.OMGJØR_VEDTAK))
                brevService.lagEllerOppdaterBrev(behandlingId, "", "", FritekstBrevtype.VEDTAK_AVSLAG)
                ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId)
                behandlingId
            }

            val behandlingshistorikk = behandlingshistorikkService.hentBehandlingshistorikker(behandlingId)

            assertThat(behandlingService.hentBehandling(behandlingId).steg).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
            assertThat(behandlingshistorikk.map { it.steg }).containsExactly(
                StegType.BEHANDLING_FERDIGSTILT,
                StegType.BREV,
                StegType.VURDERING,
                StegType.FORMKRAV
            )
        }

        @Test
        internal fun `Ikke oppfylt formkrav skal ikke vurderes`() {
            val behandlingId = testWithBrukerContext(groups = listOf(rolleConfig.ef.saksbehandler)) {
                val behandlingId = opprettBehandlingService.opprettBehandling(opprettKlagebehandlingRequest)
                formService.oppdaterForm(ikkeOppfyltFormDto(behandlingId))
                brevService.lagEllerOppdaterBrev(behandlingId, "", "", FritekstBrevtype.VEDTAK_AVSLAG)
                ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId)
                behandlingId
            }

            val behandlingshistorikk = behandlingshistorikkService.hentBehandlingshistorikker(behandlingId)

            assertThat(behandlingService.hentBehandling(behandlingId).steg).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
            assertThat(behandlingshistorikk.map { it.steg }).containsExactly(
                StegType.BEHANDLING_FERDIGSTILT,
                StegType.BREV,
                StegType.FORMKRAV
            )
        }
    }

    private val opprettKlagebehandlingRequest =
        OpprettKlagebehandlingRequest(
            "ident",
            Stønadstype.OVERGANGSSTØNAD,
            "eksternid",
            "eksternId",
            Fagsystem.EF,
            LocalDate.now(),
            "enhet"
        )

    private fun oppfyltFormDto(behandlingId: UUID) =
        DomainUtil.oppfyltForm(behandlingId).tilDto()

    private fun ikkeOppfyltFormDto(behandlingId: UUID) =
        DomainUtil.oppfyltForm(behandlingId).tilDto().copy(klagePart = FormVilkår.IKKE_OPPFYLT)

    private fun ikkeFerdigutfylt(behandlingId: UUID) =
        DomainUtil.oppfyltForm(behandlingId).tilDto().copy(klagePart = FormVilkår.IKKE_SATT)
}
