package no.nav.familie.klage.blankett

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.brev.BrevClient
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.formkrav.FormService
import no.nav.familie.klage.formkrav.dto.FormkravDto
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import no.nav.familie.klage.vurdering.VurderingService
import no.nav.familie.klage.vurdering.dto.VurderingDto
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BlankettService(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val personopplysningerService: PersonopplysningerService,
    private val formService: FormService,
    private val vurderingService: VurderingService,
    private val brevClient: BrevClient,
) {

    fun lagBlankett(behandlingId: UUID): ByteArray {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
        val formkrav = formService.hentFormDto(behandlingId)
        val vurdering = vurderingService.hentVurderingDto(behandlingId)
        val påklagetVedtak = mapPåklagetVedtak(behandling)

        val blankettPdfRequest = BlankettPdfRequest(
            behandling = BlankettPdfBehandling(
                eksternFagsakId = fagsak.eksternId,
                stønadstype = fagsak.stønadstype,
                klageMottatt = behandling.klageMottatt,
                resultat = behandling.resultat,
                påklagetVedtak = påklagetVedtak,
            ),
            personopplysninger = lagPersonopplysningerDto(behandling, fagsak),
            formkrav = mapFormkrav(formkrav),
            vurdering = mapVurdering(vurdering),
        )
        return brevClient.genererBlankett(blankettPdfRequest)
    }

    private fun mapPåklagetVedtak(behandling: Behandling): BlankettPåklagetVedtakDto? {
        return behandling.påklagetVedtak.påklagetVedtakDetaljer?.let { påklagetVedtakDetaljer ->
            BlankettPåklagetVedtakDto(
                behandlingstype = påklagetVedtakDetaljer.behandlingstype,
                resultat = påklagetVedtakDetaljer.resultat,
                vedtakstidspunkt = påklagetVedtakDetaljer.vedtakstidspunkt,
            )
        }
    }

    private fun mapVurdering(vurdering: VurderingDto?): BlankettVurderingDto? {
        return vurdering?.let {
            BlankettVurderingDto(
                vedtak = it.vedtak,
                årsak = it.årsak,
                begrunnelseOmgjøring = it.begrunnelseOmgjøring,
                hjemmel = it.hjemmel,
                innstillingKlageinstans = it.innstillingKlageinstans,
                interntNotat = it.interntNotat,
            )
        }
    }

    private fun mapFormkrav(formkrav: FormkravDto) = BlankettFormDto(
        klagePart = formkrav.klagePart,
        klageKonkret = formkrav.klageKonkret,
        klagefristOverholdt = formkrav.klagefristOverholdt,
        klagefristOverholdtUnntak = formkrav.klagefristOverholdtUnntak,
        klageSignert = formkrav.klageSignert,
        saksbehandlerBegrunnelse = formkrav.saksbehandlerBegrunnelse,
        brevtekst = formkrav.brevtekst,
    )

    private fun lagPersonopplysningerDto(behandling: Behandling, fagsak: Fagsak): PersonopplysningerDto {
        val personIdent = fagsak.hentAktivIdent()
        val navn = personopplysningerService.hentPersonopplysninger(behandling.id).navn
        return PersonopplysningerDto(navn, personIdent)
    }
}
