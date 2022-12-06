package no.nav.familie.klage.behandling.dto

import no.nav.familie.klage.behandling.domain.PåklagetVedtak
import no.nav.familie.klage.behandling.domain.PåklagetVedtakDetaljer
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak

fun FagsystemVedtak.tilPåklagetVedtakDetaljer() = PåklagetVedtakDetaljer(
    behandlingstype = this.behandlingstype,
    eksternFagsystemBehandlingId = this.eksternBehandlingId,
    fagsystemType = this.fagsystemType,
    resultat = this.resultat,
    vedtakstidspunkt = this.vedtakstidspunkt
)

fun PåklagetVedtakDetaljer.tilFagsystemVedtak() = FagsystemVedtak(
    behandlingstype = this.behandlingstype,
    eksternBehandlingId = this.eksternFagsystemBehandlingId ?: "",
    fagsystemType = this.fagsystemType,
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
