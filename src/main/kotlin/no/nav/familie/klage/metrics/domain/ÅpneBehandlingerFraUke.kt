package no.nav.familie.klage.metrics.domain

import no.nav.familie.kontrakter.felles.klage.Stønadstype

data class ÅpneBehandlingerFraUke(
    val år: Int,
    val uke: Int,
    val stonadstype: Stønadstype,
    val antall: Int,
)
