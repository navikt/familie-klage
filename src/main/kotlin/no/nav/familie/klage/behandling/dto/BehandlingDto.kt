package no.nav.familie.klage.behandling.dto

import no.nav.familie.klage.behandling.domain.BehandlingResultat
import no.nav.familie.klage.behandling.domain.BehandlingStatus
import no.nav.familie.klage.behandling.domain.BehandlingSteg
import no.nav.familie.klage.behandling.domain.BehandlingType
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.ef.StønadType
import java.time.LocalDateTime
import java.util.UUID

data class BehandlingDto(
        val id: UUID,
        val forrigeBehandlingId: UUID?,
        val fagsakId: UUID,
        val steg: BehandlingSteg,
        val type: BehandlingType,
        val status: BehandlingStatus,
        val sistEndret: LocalDateTime,
        val resultat: BehandlingResultat,
        val opprettet: LocalDateTime,
        val behandlingsårsak: BehandlingÅrsak,
        val stønadstype: StønadType,
        val vedtaksdato: LocalDateTime? = null
)


