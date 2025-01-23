package no.nav.familie.klage.brev.baks

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype
import no.nav.familie.klage.brev.felles.BrevInnholdUtleder
import no.nav.familie.klage.formkrav.FormService
import no.nav.familie.klage.infrastruktur.exception.ApiFeil
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.klage.vurdering.VurderingService
import no.nav.familie.kontrakter.felles.Regelverk
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.FagsystemType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.UUID

class FritekstBrevRequestDtoUtlederTest {
    private val formService: FormService = mockk()
    private val vurderingService: VurderingService = mockk()
    private val brevInnholdUtleder: BrevInnholdUtleder = mockk()
    private val fritekstBrevRequestDtoUtleder: FritekstBrevRequestDtoUtleder = FritekstBrevRequestDtoUtleder(
        formService = formService,
        vurderingService = vurderingService,
        brevInnholdUtleder = brevInnholdUtleder,
    )

    @Nested
    inner class UtledTest {
        @Test
        fun `skal kaste exception om vurdering er null`() {
            // Arrange
            val fagsak = DomainUtil.fagsak()

            val behandling = DomainUtil.behandling(
                fagsak = fagsak,
                påklagetVedtak = DomainUtil.lagPåklagetVedtak(
                    påklagetVedtakstype = PåklagetVedtakstype.VEDTAK,
                    påklagetVedtakDetaljer = DomainUtil.lagPåklagetVedtakDetaljer(
                        fagsystemType = FagsystemType.ORDNIÆR,
                        eksternFagsystemBehandlingId = UUID.randomUUID().toString(),
                        behandlingstype = "type",
                        resultat = "resultat",
                        vedtakstidspunkt = LocalDateTime.now(),
                        regelverk = Regelverk.NASJONAL,
                    ),
                ),
            )

            every {
                formService.formkravErOppfyltForBehandling(behandling.id)
            } returns true

            every {
                vurderingService.hentVurdering(behandling.id)
            } returns null

            // Act & assert
            val exception = assertThrows<Feil> {
                fritekstBrevRequestDtoUtleder.utled(fagsak, behandling, "navn")
            }
            assertThat(exception.message).isEqualTo("Vurdering er null for behandling ${behandling.id}")
        }

        @Test
        fun `skal kaste exception hvis påklagetVedtakDetaljer er null for ikke medhold brev`() {
            // Arrange
            val fagsak = DomainUtil.fagsak()

            val behandling = DomainUtil.behandling(
                fagsak = fagsak,
                påklagetVedtak = DomainUtil.lagPåklagetVedtak(
                    påklagetVedtakstype = PåklagetVedtakstype.VEDTAK,
                    påklagetVedtakDetaljer = null,
                ),
            )

            val vurdering = DomainUtil.vurdering(behandlingId = behandling.id)

            every {
                formService.formkravErOppfyltForBehandling(behandling.id)
            } returns true

            every {
                vurderingService.hentVurdering(behandling.id)
            } returns vurdering

            // Act & assert
            val exception = assertThrows<ApiFeil> {
                fritekstBrevRequestDtoUtleder.utled(fagsak, behandling, "navn")
            }
            assertThat(exception.message).isEqualTo("Kan ikke opprette brev til klageinstansen når det ikke er valgt et påklaget vedtak")
        }

        @Test
        fun `skal kaste exception hvis innstillingKlageinstans er null for ikke medhold brev`() {
            // Arrange
            val fagsak = DomainUtil.fagsak()

            val påklagetVedtakDetaljer = DomainUtil.lagPåklagetVedtakDetaljer(
                fagsystemType = FagsystemType.ORDNIÆR,
                eksternFagsystemBehandlingId = UUID.randomUUID().toString(),
                behandlingstype = "type",
                resultat = "resultat",
                vedtakstidspunkt = LocalDateTime.now(),
                regelverk = Regelverk.NASJONAL,
            )

            val behandling = DomainUtil.behandling(
                fagsak = fagsak,
                påklagetVedtak = DomainUtil.lagPåklagetVedtak(
                    påklagetVedtakstype = PåklagetVedtakstype.VEDTAK,
                    påklagetVedtakDetaljer = påklagetVedtakDetaljer,
                ),
            )

            val vurdering = DomainUtil.vurdering(
                behandlingId = behandling.id,
                innstillingKlageinstans = null,
            )

            val fakeFritekstBrevRequestDto = DomainUtil.lagFritekstBrevRequestDto(
                overskrift = "overskrift",
                avsnitt = listOf(),
                personIdent = "123",
                navn = "navn",
            )

            every {
                formService.formkravErOppfyltForBehandling(behandling.id)
            } returns true

            every {
                vurderingService.hentVurdering(behandling.id)
            } returns vurdering

            every {
                brevInnholdUtleder.lagOpprettholdelseBrev(
                    any(),
                    any(),
                    any(),
                    any(),
                    eq(påklagetVedtakDetaljer),
                    any(),
                )
            } returns fakeFritekstBrevRequestDto

            // Act & assert
            val exception = assertThrows<Feil> {
                fritekstBrevRequestDtoUtleder.utled(fagsak, behandling, "navn")
            }
            assertThat(exception.message).isEqualTo(
                "Behandling med resultat ${BehandlingResultat.IKKE_MEDHOLD} mangler instillingKlageinstans for generering av brev",
            )
        }

        @Test
        fun `skal utlede ikke medhold FritekstBrevRequestDto`() {
            // Arrange
            val fagsak = DomainUtil.fagsak()

            val påklagetVedtakDetaljer = DomainUtil.lagPåklagetVedtakDetaljer(
                fagsystemType = FagsystemType.ORDNIÆR,
                eksternFagsystemBehandlingId = UUID.randomUUID().toString(),
                behandlingstype = "type",
                resultat = "resultat",
                vedtakstidspunkt = LocalDateTime.now(),
                regelverk = Regelverk.NASJONAL,
            )

            val behandling = DomainUtil.behandling(
                fagsak = fagsak,
                påklagetVedtak = DomainUtil.lagPåklagetVedtak(
                    påklagetVedtakstype = PåklagetVedtakstype.VEDTAK,
                    påklagetVedtakDetaljer = påklagetVedtakDetaljer,
                ),
            )

            val vurdering = DomainUtil.vurdering(behandlingId = behandling.id)

            val fakeFritekstBrevRequestDto = DomainUtil.lagFritekstBrevRequestDto(
                overskrift = "overskrift",
                avsnitt = listOf(),
                personIdent = "123",
                navn = "navn",
            )

            every {
                formService.formkravErOppfyltForBehandling(behandling.id)
            } returns true

            every {
                vurderingService.hentVurdering(behandling.id)
            } returns vurdering

            every {
                brevInnholdUtleder.lagOpprettholdelseBrev(
                    any(),
                    any(),
                    any(),
                    any(),
                    eq(påklagetVedtakDetaljer),
                    any(),
                )
            } returns fakeFritekstBrevRequestDto

            // Act
            val fritekstBrevRequestDto = fritekstBrevRequestDtoUtleder.utled(fagsak, behandling, "navn")

            // Assert
            assertThat(fritekstBrevRequestDto).isEqualTo(fakeFritekstBrevRequestDto)
        }

        @Test
        fun `skal utlede ikke medhold formkrav avvist FritekstBrevRequestDto`() {
            // Arrange
            val fagsak = DomainUtil.fagsak()

            val påklagetVedtakDetaljer = DomainUtil.lagPåklagetVedtakDetaljer(
                fagsystemType = FagsystemType.ORDNIÆR,
                eksternFagsystemBehandlingId = UUID.randomUUID().toString(),
                behandlingstype = "type",
                resultat = "resultat",
                vedtakstidspunkt = LocalDateTime.now(),
                regelverk = Regelverk.NASJONAL,
            )

            val behandling = DomainUtil.behandling(
                fagsak = fagsak,
                påklagetVedtak = DomainUtil.lagPåklagetVedtak(
                    påklagetVedtakstype = PåklagetVedtakstype.VEDTAK,
                    påklagetVedtakDetaljer = påklagetVedtakDetaljer,
                ),
            )

            val fakeFritekstBrevRequestDto = DomainUtil.lagFritekstBrevRequestDto(
                overskrift = "overskrift",
                avsnitt = listOf(),
                personIdent = "123",
                navn = "navn",
            )

            val form = DomainUtil.oppfyltForm(behandlingId = behandling.id)

            every {
                formService.formkravErOppfyltForBehandling(behandling.id)
            } returns false

            every {
                formService.hentForm(behandlingId = behandling.id)
            } returns form

            every {
                brevInnholdUtleder.lagFormkravAvvistBrev(
                    any(),
                    any(),
                    eq(form),
                    any(),
                    eq(påklagetVedtakDetaljer),
                    any(),
                )
            } returns fakeFritekstBrevRequestDto

            // Act
            val fritekstBrevRequestDto = fritekstBrevRequestDtoUtleder.utled(fagsak, behandling, "navn")

            // Assert
            assertThat(fritekstBrevRequestDto).isEqualTo(fakeFritekstBrevRequestDto)
        }

        @Test
        fun `skal utlede ikke medhold formkrav avvist ikke påklaget vedtak FritekstBrevRequestDto`() {
            // Arrange
            val fagsak = DomainUtil.fagsak()

            val behandling = DomainUtil.behandling(
                fagsak = fagsak,
                påklagetVedtak = DomainUtil.lagPåklagetVedtak(
                    påklagetVedtakstype = PåklagetVedtakstype.UTEN_VEDTAK,
                    påklagetVedtakDetaljer = DomainUtil.lagPåklagetVedtakDetaljer(
                        fagsystemType = FagsystemType.ORDNIÆR,
                        eksternFagsystemBehandlingId = UUID.randomUUID().toString(),
                        behandlingstype = "type",
                        resultat = "resultat",
                        vedtakstidspunkt = LocalDateTime.now(),
                        regelverk = Regelverk.NASJONAL,
                    ),
                ),
            )

            val fakeFritekstBrevRequestDto = DomainUtil.lagFritekstBrevRequestDto(
                overskrift = "overskrift",
                avsnitt = listOf(),
                personIdent = "123",
                navn = "navn",
            )

            val form = DomainUtil.oppfyltForm(behandlingId = behandling.id)

            every {
                formService.formkravErOppfyltForBehandling(behandling.id)
            } returns false

            every {
                formService.hentForm(behandlingId = behandling.id)
            } returns form

            every {
                brevInnholdUtleder.lagFormkravAvvistBrevIkkePåklagetVedtak(
                    any(),
                    any(),
                    eq(form),
                    any(),
                )
            } returns fakeFritekstBrevRequestDto

            // Act
            val fritekstBrevRequestDto = fritekstBrevRequestDtoUtleder.utled(fagsak, behandling, "navn")

            // Assert
            assertThat(fritekstBrevRequestDto).isEqualTo(fakeFritekstBrevRequestDto)
        }
    }
}
