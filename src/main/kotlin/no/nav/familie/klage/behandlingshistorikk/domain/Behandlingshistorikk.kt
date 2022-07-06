package no.nav.familie.klage.behandlingshistorikk.domain

import org.springframework.data.annotation.Id
import java.time.LocalDateTime
import java.util.UUID

data class Behandlingshistorikk(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val steg: Steg,
    val opprettetAv: String,
    val endretTid: LocalDateTime? = LocalDateTime.now()
) {
}

enum class Steg {
    OPPRETTET,
    AVVIST,
    GODKJENT,
}