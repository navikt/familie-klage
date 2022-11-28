package no.nav.familie.klage.blankett

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.dto.BehandlingDto
import no.nav.familie.klage.formkrav.FormService
import no.nav.familie.klage.formkrav.dto.FormkravDto
import no.nav.familie.klage.integrasjoner.FagsystemVedtakService
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import no.nav.familie.klage.vurdering.VurderingService
import no.nav.familie.klage.vurdering.dto.VurderingDto
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BlankettService(
    private val behandlingService: BehandlingService,
    private val personopplysningerService: PersonopplysningerService,
    private val formService: FormService,
    private val vurderingService: VurderingService,
    private val blankettClient: BlankettClient,
    private val fagsystemVedtakService: FagsystemVedtakService
) {

    fun lagBlankett(behandlingId: UUID): ByteArray {
        val behandling = behandlingService.hentBehandlingDto(behandlingId)
        val formkrav = formService.hentFormDto(behandlingId)
        val vurdering = vurderingService.hentVurderingDto(behandlingId)
        val påklagetVedtak = mapPåklagetVedtak(behandling)

        val blankettPdfRequest = BlankettPdfRequest(
            behandling = BlankettPdfBehandling(
                eksternFagsakId = behandling.eksternFagsystemFagsakId,
                stønadstype = behandling.stønadstype,
                klageMottatt = behandling.klageMottatt,
                resultat = behandling.resultat,
                påklagetVedtak = påklagetVedtak
            ),
            personopplysninger = lagPersonopplysningerDto(behandling),
            formkrav = mapFormkrav(formkrav),
            vurdering = mapVurdering(vurdering)
        )
        return blankettClient.genererBlankett(blankettPdfRequest)
    }

    private fun mapPåklagetVedtak(behandling: BehandlingDto): BlankettPåklagetVedtakDto? {
        return behandling.påklagetVedtak.eksternFagsystemBehandlingId?.let { påklagetBehandlingId ->
            val fagsystemVedtak = fagsystemVedtakService.hentFagsystemVedtak(behandling.id)
            val påklagetVedtak = fagsystemVedtak.singleOrNull { it.eksternBehandlingId == påklagetBehandlingId }
                ?: error("Finner ikke fagsystemvedtak med eksternBehandlingId=$påklagetBehandlingId")
            BlankettPåklagetVedtakDto(påklagetVedtak.behandlingstype, påklagetVedtak.resultat, påklagetVedtak.vedtakstidspunkt)
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
                interntNotat = it.interntNotat
            )
        }
    }

    private fun mapFormkrav(formkrav: FormkravDto) = BlankettFormDto(
        klagePart = formkrav.klagePart,
        klageKonkret = formkrav.klageKonkret,
        klagefristOverholdt = formkrav.klagefristOverholdt,
        klageSignert = formkrav.klageSignert,
        saksbehandlerBegrunnelse = formkrav.saksbehandlerBegrunnelse,
        brevtekst = formkrav.brevtekst
    )

    private fun lagPersonopplysningerDto(behandling: BehandlingDto): PersonopplysningerDto {
        val (personIdent, _) = behandlingService.hentAktivIdent(behandling.id)
        val navn = personopplysningerService.hentPersonopplysninger(behandling.id).navn
        return PersonopplysningerDto(navn, personIdent)
    }
}
