package no.nav.familie.klage.behandling.dto

import no.nav.familie.klage.behandling.domain.PåklagetVedtakDetaljer
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak

fun FagsystemVedtak.tilPåklagetVedtakDetaljer() = PåklagetVedtakDetaljer(
    behandlingstype = this.behandlingstype,
    eksternFagsystemBehandlingId = this.eksternBehandlingId,
    fagsystemType = this.fagsystemType,
    resultat = this.resultat,
    vedtakstidspunkt = this.vedtakstidspunkt
)
