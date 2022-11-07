package no.nav.familie.klage.metrics.domain

import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.Stønadstype

data class VedtakPerUke(
    val år: Int,
    val uke: Int,
    val stonadstype: Stønadstype,
    val resultat: BehandlingResultat,
    val antall: Int
)
