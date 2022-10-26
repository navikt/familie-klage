package no.nav.familie.klage.blankett

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.dto.BehandlingDto
import no.nav.familie.klage.formkrav.FormService
import no.nav.familie.klage.formkrav.dto.FormDto
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
) {

    fun lagBlankett(behandlingId: UUID): ByteArray {
        val behandling = behandlingService.hentBehandlingDto(behandlingId)
        val formkrav = formService.hentFormDto(behandlingId)
        val vurdering = vurderingService.hentVurderingDto(behandlingId)

        val blankettPdfRequest = BlankettPdfRequest(
            behandling = BlankettPdfBehandling(
                stønadstype = behandling.stønadstype,
                klageMottatt = behandling.klageMottatt,
                resultat = behandling.resultat ?: error("Mangler resultat på behandling=$behandlingId"),
                påklagetVedtak = BlankettPåklagetVedtakDto(behandling.påklagetVedtak.eksternFagsystemBehandlingId)
            ),
            personopplysninger = lagPersonopplysningerDto(behandling),
            formkrav = mapFormkrav(formkrav),
            vurdering = mapVurdering(vurdering)
        )
        return blankettClient.genererBlankett(blankettPdfRequest)
    }

    private fun mapVurdering(vurdering: VurderingDto?): BlankettVurderingDto? {
        return vurdering?.let {
            BlankettVurderingDto(
                vedtak = it.vedtak,
                arsak = it.arsak,
                hjemmel = it.hjemmel,
                innstillingKlageinstans = it.innstillingKlageinstans,
                interntNotat = it.interntNotat
            )
        }
    }

    private fun mapFormkrav(formkrav: FormDto) = BlankettFormDto(
        klagePart = formkrav.klagePart,
        klageKonkret = formkrav.klageKonkret,
        klagefristOverholdt = formkrav.klagefristOverholdt,
        klageSignert = formkrav.klageSignert,
        saksbehandlerBegrunnelse = formkrav.saksbehandlerBegrunnelse,
    )

    private fun lagPersonopplysningerDto(behandling: BehandlingDto): PersonopplysningerDto {
        val (personIdent, _) = behandlingService.hentAktivIdent(behandling.id)
        val navn = personopplysningerService.hentPersonopplysninger(behandling.id).navn
        return PersonopplysningerDto(navn, personIdent)
    }

}
