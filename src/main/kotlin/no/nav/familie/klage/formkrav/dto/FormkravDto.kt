package no.nav.familie.klage.formkrav.dto

import no.nav.familie.klage.behandling.dto.PåklagetVedtakDto
import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.domain.FormVilkår
import no.nav.familie.klage.formkrav.domain.FormkravFristUnntak
import java.time.LocalDateTime
import java.util.UUID

data class FormkravDto(
    val behandlingId: UUID,
    val klagePart: FormVilkår,
    val klageKonkret: FormVilkår,
    val klagefristOverholdt: FormVilkår,
    val klagefristOverholdtUnntak: FormkravFristUnntak,
    val klageSignert: FormVilkår,
    val saksbehandlerBegrunnelse: String?,
    val brevtekst: String?,
    val endretTid: LocalDateTime,
    val påklagetVedtak: PåklagetVedtakDto,
)

fun Form.tilDto(påklagetVedtak: PåklagetVedtakDto): FormkravDto =
    FormkravDto(
        behandlingId = this.behandlingId,
        klagePart = this.klagePart,
        klageKonkret = this.klageKonkret,
        klagefristOverholdt = this.klagefristOverholdt,
        klagefristOverholdtUnntak = this.klagefristOverholdtUnntak,
        klageSignert = this.klageSignert,
        saksbehandlerBegrunnelse = this.saksbehandlerBegrunnelse,
        brevtekst = this.brevtekst,
        endretTid = this.sporbar.endret.endretTid,
        påklagetVedtak = påklagetVedtak,
    )
