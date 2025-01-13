package no.nav.familie.klage.brev.baks

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype
import no.nav.familie.klage.brev.BrevInnholdUtleder
import no.nav.familie.klage.brev.FritekstBrevRequestDto
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.formkrav.FormService
import no.nav.familie.klage.infrastruktur.exception.ApiFeil
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.vurdering.VurderingService
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class FritekstBrevRequestDtoUtleder(
    private val formService: FormService,
    private val vurderingService: VurderingService,
    private val brevInnholdUtleder: BrevInnholdUtleder,
) {
    fun utled(
        fagsak: Fagsak,
        behandling: Behandling,
        navn: String,
    ): FritekstBrevRequestDto {
        val behandlingResultat = utledBehandlingResultat(behandling.id)
        return when (behandlingResultat) {
            BehandlingResultat.IKKE_MEDHOLD,
            -> vedIkkeMedhold(behandlingResultat, fagsak, behandling, navn)

            BehandlingResultat.IKKE_MEDHOLD_FORMKRAV_AVVIST,
            -> vedIkkeMedholdFormkravAvvist(fagsak, behandling, navn)

            BehandlingResultat.MEDHOLD,
            BehandlingResultat.IKKE_SATT,
            BehandlingResultat.HENLAGT,
            -> throw Feil("Kan ikke lage brev for behandling med behandlingResultat=$behandlingResultat")
        }
    }

    private fun utledBehandlingResultat(behandlingId: UUID): BehandlingResultat {
        val erFormkravErOppfyltForBehandling = formService.formkravErOppfyltForBehandling(behandlingId)
        return if (erFormkravErOppfyltForBehandling) {
            val vurdering = vurderingService.hentVurdering(behandlingId)
            val behandlingResultat = vurdering?.vedtak?.tilBehandlingResultat()
            if (behandlingResultat == null) {
                throw Feil("BehandlingResultat er null for behandling $behandlingId")
            }
            behandlingResultat
        } else {
            BehandlingResultat.IKKE_MEDHOLD_FORMKRAV_AVVIST
        }
    }

    private fun vedIkkeMedhold(
        behandlingResultat: BehandlingResultat,
        fagsak: Fagsak,
        behandling: Behandling,
        navn: String,
    ): FritekstBrevRequestDto {
        if (behandling.påklagetVedtak.påklagetVedtakDetaljer == null) {
            // TODO : Dette burde byttes ut med et exception fra domenelaget
            throw ApiFeil.badRequest("Kan ikke opprette brev til klageinstansen når det ikke er valgt et påklaget vedtak")
        }
        val instillingKlageinstans = vurderingService.hentVurdering(behandling.id)?.innstillingKlageinstans
        if (instillingKlageinstans == null) {
            throw Feil("Behandling med resultat $behandlingResultat mangler instillingKlageinstans for generering av brev")
        }
        return brevInnholdUtleder.lagOpprettholdelseBrev(
            ident = fagsak.hentAktivIdent(),
            instillingKlageinstans = instillingKlageinstans,
            navn = navn,
            stønadstype = fagsak.stønadstype,
            påklagetVedtakDetaljer = behandling.påklagetVedtak.påklagetVedtakDetaljer,
            klageMottatt = behandling.klageMottatt,
        )
    }

    private fun vedIkkeMedholdFormkravAvvist(
        fagsak: Fagsak,
        behandling: Behandling,
        navn: String,
    ): FritekstBrevRequestDto {
        val formkrav = formService.hentForm(behandling.id)
        return when (behandling.påklagetVedtak.påklagetVedtakstype) {
            PåklagetVedtakstype.UTEN_VEDTAK -> brevInnholdUtleder.lagFormkravAvvistBrevIkkePåklagetVedtak(
                ident = fagsak.hentAktivIdent(),
                navn = navn,
                formkrav = formkrav,
                stønadstype = fagsak.stønadstype,
            )

            else -> brevInnholdUtleder.lagFormkravAvvistBrev(
                ident = fagsak.hentAktivIdent(),
                navn = navn,
                form = formkrav,
                stønadstype = fagsak.stønadstype,
                påklagetVedtakDetaljer = behandling.påklagetVedtak.påklagetVedtakDetaljer,
                fagsystem = fagsak.fagsystem,
            )
        }
    }
}
