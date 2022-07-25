package no.nav.familie.klage.behandling.dto

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.BehandlingResultat
import no.nav.familie.klage.behandling.domain.BehandlingStatus
import no.nav.familie.klage.behandling.domain.BehandlingSteg
import no.nav.familie.klage.behandling.domain.BehandlingsÅrsak
import no.nav.familie.klage.behandling.domain.StønadsType
import no.nav.familie.kontrakter.felles.Fagsystem
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

fun Behandling.tilDto(): BehandlingDto =
        BehandlingDto(
                id = this.id,
                fagsakId = this.fagsakId,
                steg = this.steg,
                status = this.status,
                sistEndret = this.sporbar.endret.endretTid,
                resultat = this.resultat,
                opprettet = this.sporbar.opprettetTid,
                stonadsType = this.stonadsType,
                fagsystem = this.fagsystem
        )


