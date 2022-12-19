package no.nav.familie.klage.behandling.dto

import no.nav.familie.klage.behandling.domain.PåklagetVedtak
import no.nav.familie.klage.behandling.domain.PåklagetVedtakDetaljer
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype
import no.nav.familie.kontrakter.felles.klage.FagsystemType
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import no.nav.familie.kontrakter.felles.klage.VedtakType

fun FagsystemVedtak.tilPåklagetVedtakDetaljer() = PåklagetVedtakDetaljer(
    behandlingstype = this.behandlingstype,
    eksternFagsystemBehandlingId = this.eksternBehandlingId,
    fagsystemType = this.vedtakType,
    resultat = this.resultat,
    vedtakstidspunkt = this.vedtakstidspunkt
)

fun PåklagetVedtakDetaljer.tilFagsystemVedtak() = FagsystemVedtak(
    behandlingstype = this.behandlingstype,
    eksternBehandlingId = this.eksternFagsystemBehandlingId ?: "",
    fagsystemType = vedtakTypeTilFagsystemType(this.fagsystemType), // Todo: Skal fjernes
    vedtakType = this.fagsystemType,
    resultat = this.resultat,
    vedtakstidspunkt = this.vedtakstidspunkt
)

fun PåklagetVedtak.tilDto(): PåklagetVedtakDto =
    PåklagetVedtakDto(
        eksternFagsystemBehandlingId = this.påklagetVedtakDetaljer?.eksternFagsystemBehandlingId,
        påklagetVedtakstype = this.påklagetVedtakstype,
        fagsystemVedtak = this.påklagetVedtakDetaljer?.tilFagsystemVedtak(),
        vedtaksdatoInfotrygd = if (påklagetVedtakstype == PåklagetVedtakstype.INFOTRYGD_TILBAKEKREVING) this.påklagetVedtakDetaljer?.vedtakstidspunkt?.toLocalDate() else null
    )

private fun vedtakTypeTilFagsystemType(vedtakType: VedtakType) = when (vedtakType) {
    VedtakType.ORDINÆR, VedtakType.SANKSJON_1_MND -> FagsystemType.ORDNIÆR
    VedtakType.TILBAKEKREVING -> FagsystemType.TILBAKEKREVING
}
