package no.nav.familie.klage.behandlingshistorikk.domain

import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import org.springframework.data.annotation.Id
import java.time.LocalDateTime
import java.util.UUID

data class Behandlingshistorikk(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val steg: StegType,
    val opprettetAv: String = SikkerhetContext.hentSaksbehandler(),
    val endretTid: LocalDateTime? = LocalDateTime.now()
)
