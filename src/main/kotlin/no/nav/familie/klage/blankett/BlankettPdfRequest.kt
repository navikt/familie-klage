package no.nav.familie.klage.blankett

import no.nav.familie.klage.behandling.dto.PåklagetVedtakDto
import no.nav.familie.klage.formkrav.dto.FormDto
import no.nav.familie.klage.vurdering.dto.VurderingDto
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import java.time.LocalDate

data class BlankettPdfRequest(
    val behandling: BlankettPdfBehandling,
    val personopplysninger: PersonopplysningerDto,
    val formkrav: FormDto,
    val vurdering: VurderingDto?
)

data class BlankettPdfBehandling(
    val stønadstype: Stønadstype,
    val klageMottatt: LocalDate,
    val resultat: BehandlingResultat,
    val påklagetVedtak: PåklagetVedtakDto
)

data class PersonopplysningerDto(
    val navn: String,
    val personIdent: String
)