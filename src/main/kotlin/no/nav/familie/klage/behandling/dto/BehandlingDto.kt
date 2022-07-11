package no.nav.familie.klage.behandling.dto

import no.nav.familie.klage.behandling.domain.BehandlingResultat
import no.nav.familie.klage.behandling.domain.BehandlingStatus
import no.nav.familie.klage.behandling.domain.BehandlingSteg
import no.nav.familie.klage.behandling.domain.BehandlingsÅrsak
import no.nav.familie.klage.behandling.domain.Fagsystem
import no.nav.familie.klage.behandling.domain.StønadsType
import java.time.LocalDateTime
import java.util.UUID

data class BehandlingDto(
        val id: UUID,
        val fagsakId: UUID,
        val steg: BehandlingSteg,
        val status: BehandlingStatus,
        val sistEndret: LocalDateTime,
        val resultat: BehandlingResultat?,
        val opprettet: LocalDateTime,
        val fagsystem: Fagsystem,
        val vedtaksdato: LocalDateTime? = null,
        val stonadsType: StønadsType = StønadsType.BARNETILSYN,
        val behandlingsArsak: BehandlingsÅrsak = BehandlingsÅrsak.KLAGE,

)


