package no.nav.familie.klage.behandling.dto

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.kabal.dto.KlageresultatDto
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class BehandlingDto(
    val id: UUID,
    val fagsakId: UUID,
    val steg: StegType,
    val status: BehandlingStatus,
    val sistEndret: LocalDateTime,
    val resultat: BehandlingResultat?,
    val opprettet: LocalDateTime,
    val vedtaksdato: LocalDateTime? = null,
    val stønadstype: Stønadstype,
    val klageresultat: List<KlageresultatDto>,
    val eksternFagsystemBehandlingId: String,
    val eksternFagsystemFagsakId: String,
    val fagsystem: Fagsystem,
    val klageMottatt: LocalDate,
)

fun Behandling.tilDto(fagsak: Fagsak, klageresultat: List<KlageresultatDto>): BehandlingDto =
    BehandlingDto(
        id = this.id,
        fagsakId = this.fagsakId,
        steg = this.steg,
        status = this.status,
        sistEndret = this.sporbar.endret.endretTid,
        resultat = this.resultat,
        opprettet = this.sporbar.opprettetTid,
        stønadstype = fagsak.stønadstype,
        fagsystem = fagsak.fagsystem,
        eksternFagsystemFagsakId = fagsak.eksternId,
        klageresultat = klageresultat,
        eksternFagsystemBehandlingId = this.eksternFagsystemBehandlingId,
        klageMottatt = this.klageMottatt
    )
