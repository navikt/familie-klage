package no.nav.familie.klage.formkrav.dto

import no.nav.familie.klage.formkrav.domain.Form
import java.time.LocalDate
import java.util.UUID

data class FormDto(
    val behandlingId: UUID,
    val fagsakId: UUID,
    val vedtaksdato: LocalDate = LocalDate.now(),
    val klageMottat: LocalDate = LocalDate.now(),
    val klageaarsak: String,
    val klageBeskrivelse: String,
    val sakSistEndret: LocalDate = LocalDate.now()
)

fun Form.tilDto(): FormDto =
    FormDto(
        behandlingId = this.behandlingId,
        fagsakId = this.fagsakId,
        vedtaksdato = this.vedtaksdato,
        klageMottat = this.klageMottat,
        klageaarsak = this.klageaarsak,
        klageBeskrivelse = this.klageBeskrivelse,
        sakSistEndret = this.sakSistEndret
    )