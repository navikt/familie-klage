package no.nav.familie.klage.behandling.dto

import no.nav.familie.klage.behandling.domain.PåklagetVedtak
import no.nav.familie.klage.behandling.domain.PåklagetVedtakDetaljer
import no.nav.familie.klage.behandling.domain.harManuellVedtaksdato
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak

fun FagsystemVedtak.tilPåklagetVedtakDetaljer() = PåklagetVedtakDetaljer(
    behandlingstype = this.behandlingstype,
    eksternFagsystemBehandlingId = this.eksternBehandlingId,
    internKlagebehandlingId = null, // TODO..
    fagsystemType = this.fagsystemType,
    resultat = this.resultat,
    vedtakstidspunkt = this.vedtakstidspunkt,
    regelverk = this.regelverk,
)

fun PåklagetVedtakDetaljer.tilFagsystemVedtak() = FagsystemVedtak(
    behandlingstype = this.behandlingstype,
    eksternBehandlingId = this.eksternFagsystemBehandlingId ?: "",
    fagsystemType = this.fagsystemType,
    resultat = this.resultat,
    vedtakstidspunkt = this.vedtakstidspunkt,
    regelverk = this.regelverk,
)

fun PåklagetVedtak.tilDto(): PåklagetVedtakDto =
    PåklagetVedtakDto(
        eksternFagsystemBehandlingId = this.påklagetVedtakDetaljer?.eksternFagsystemBehandlingId,
        internKlagebehandlingId = this.påklagetVedtakDetaljer?.internKlagebehandlingId,
        påklagetVedtakstype = this.påklagetVedtakstype,
        fagsystemVedtak = this.påklagetVedtakDetaljer?.tilFagsystemVedtak(),
        manuellVedtaksdato = if (påklagetVedtakstype.harManuellVedtaksdato()) this.påklagetVedtakDetaljer?.vedtakstidspunkt?.toLocalDate() else null,
        regelverk = this.påklagetVedtakDetaljer?.regelverk,
    )
