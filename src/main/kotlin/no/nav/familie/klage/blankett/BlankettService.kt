package no.nav.familie.klage.blankett

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.dto.BehandlingDto
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.formkrav.FormService
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import no.nav.familie.klage.vurdering.VurderingService
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
        val blankettPdfRequest = BlankettPdfRequest(
            behandling = BlankettPdfBehandling(
                stønadstype = behandling.stønadstype,
                klageMottatt = behandling.klageMottatt,
                resultat = behandling.resultat,
                påklagetVedtak = behandling.påklagetVedtak
            ),
            personopplysninger = lagPersonopplysningerDto(behandling),
            formkrav = formService.hentFormDto(behandlingId),
            vurdering = vurderingService.hentVurderingDto(behandlingId)
        )
        return blankettClient.genererBlankett(blankettPdfRequest)
    }

    private fun lagPersonopplysningerDto(behandling: BehandlingDto): PersonopplysningerDto {
        val (personIdent, _) = behandlingService.hentAktivIdent(behandling.id)
        val navn = personopplysningerService.hentPersonopplysninger(behandling.id).navn
        return PersonopplysningerDto(navn, personIdent)
    }

}
