package no.nav.familie.klage.fagsak.domain

import no.nav.familie.klage.behandling.domain.Fagsystem
import org.springframework.data.annotation.Id
import java.time.LocalDateTime
import java.util.UUID

data class Fagsak(
    @Id
    val id: UUID = UUID.randomUUID(),
    val eksternFagsakId: UUID,
    val sistEndret: LocalDateTime,
    val opprettet: LocalDateTime,
    val fagsystem: Fagsystem,
    val personIdent: String
)