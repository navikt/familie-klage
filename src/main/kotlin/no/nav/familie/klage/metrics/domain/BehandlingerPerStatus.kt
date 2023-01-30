package no.nav.familie.klage.metrics.domain

import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.klage.Stønadstype

data class BehandlingerPerStatus(
    val stonadstype: Stønadstype,
    val status: BehandlingStatus,
    val antall: Int,
)
