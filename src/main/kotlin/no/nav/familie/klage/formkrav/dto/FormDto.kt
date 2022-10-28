package no.nav.familie.klage.formkrav.dto

import no.nav.familie.klage.behandling.domain.PåklagetVedtak
import no.nav.familie.klage.behandling.dto.PåklagetVedtakDto
import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.domain.FormVilkår
import java.time.LocalDateTime
import java.util.UUID

data class FormDto(
    val behandlingId: UUID,
    val klagePart: FormVilkår,
    val klageKonkret: FormVilkår,
    val klagefristOverholdt: FormVilkår,
    val klageSignert: FormVilkår,
    val saksbehandlerBegrunnelse: String,
    val endretTid: LocalDateTime,
    val påklagetVedtak: PåklagetVedtakDto
)

fun Form.tilDto(påklagetVedtak: PåklagetVedtakDto): FormDto =
    FormDto(
        behandlingId = this.behandlingId,
        klagePart = this.klagePart,
        klageKonkret = this.klageKonkret,
        klagefristOverholdt = this.klagefristOverholdt,
        klageSignert = this.klageSignert,
        saksbehandlerBegrunnelse = this.saksbehandlerBegrunnelse,
        endretTid = this.sporbar.endret.endretTid,
        påklagetVedtak = påklagetVedtak
    )

fun PåklagetVedtak.tilDto(): PåklagetVedtakDto =
    PåklagetVedtakDto(
        eksternFagsystemBehandlingId = this.eksternFagsystemBehandlingId,
        påklagetVedtakstype = this.påklagetVedtakstype
    )
