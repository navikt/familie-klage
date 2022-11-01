package no.nav.familie.klage.blankett

import no.nav.familie.klage.formkrav.domain.FormVilkår
import no.nav.familie.klage.vurdering.domain.Hjemmel
import no.nav.familie.klage.vurdering.domain.Vedtak
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import no.nav.familie.kontrakter.felles.klage.Årsak
import java.time.LocalDate
import java.time.LocalDateTime

data class BlankettPdfRequest(
    val behandling: BlankettPdfBehandling,
    val personopplysninger: PersonopplysningerDto,
    val formkrav: BlankettFormDto,
    val vurdering: BlankettVurderingDto?
)

data class BlankettPdfBehandling(
    val eksternFagsakId: String,
    val stønadstype: Stønadstype,
    val klageMottatt: LocalDate,
    val resultat: BehandlingResultat,
    val påklagetVedtak: BlankettPåklagetVedtakDto?
)

data class BlankettPåklagetVedtakDto(
    val behandlingstype: String,
    val resultat: String,
    val vedtakstidspunkt: LocalDateTime
)

data class PersonopplysningerDto(
    val navn: String,
    val personIdent: String
)

data class BlankettFormDto(
    val klagePart: FormVilkår,
    val klageKonkret: FormVilkår,
    val klagefristOverholdt: FormVilkår,
    val klageSignert: FormVilkår,
    val saksbehandlerBegrunnelse: String,
    val brevtekst: String
)

data class BlankettVurderingDto(
    val vedtak: Vedtak,
    val arsak: Årsak?,
    val hjemmel: Hjemmel?,
    val innstillingKlageinstans: String?,
    val interntNotat: String?
)
