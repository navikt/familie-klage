package no.nav.familie.klage.behandlingshistorikk.domain

import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import org.springframework.data.annotation.Id
import java.time.LocalDateTime
import java.util.UUID

data class Behandlingshistorikk(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val steg: StegType,
    val opprettetAv: String = SikkerhetContext.hentSaksbehandler(),
    val endretTid: LocalDateTime? = LocalDateTime.now(),
)

data class BehandlingshistorikkDto(
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val steg: StegType,
    val behandlingStatus: BehandlingStatus,
    val opprettetAv: String = SikkerhetContext.hentSaksbehandler(),
    val endretTid: LocalDateTime? = LocalDateTime.now(),
)

fun Behandlingshistorikk.tilDto() = BehandlingshistorikkDto(
    id = this.id,
    behandlingId = this.behandlingId,
    steg = this.steg,
    behandlingStatus = this.steg.gjelderStatus,
    opprettetAv = this.opprettetAv,
    endretTid = this.endretTid,
)