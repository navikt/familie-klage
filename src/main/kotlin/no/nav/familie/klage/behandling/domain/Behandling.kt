package no.nav.familie.klage.behandling.domain

import org.springframework.data.annotation.Id
import java.time.LocalDateTime
import java.util.UUID

data class Behandling(
    @Id
    val id: UUID = UUID.randomUUID(),
    val fagsakId: UUID,
    val steg: BehandlingSteg,
    val status: BehandlingStatus,
    val sistEndret: LocalDateTime,
    val resultat: BehandlingResultat? = null,
    val opprettet: LocalDateTime,
    val fagsystem: Fagsystem,
    val vedtaksdato: LocalDateTime? = null
)

enum class BehandlingResultat(val displayName: String) {
    MEDHOLD(displayName = "Medhold"),
    IKKE_MEDHOLD(displayName = "Ikke medhold"),
}

enum class BehandlingStatus {
    OPPRETTET,
    UTREDES,
    FERDIGSTILT,
    ;
}

enum class BehandlingSteg {
    FORMALKRAV,
    VURDERING,
    KABAL,
    BEHANDLING_FERDIGSTILT,
}

enum class Fagsystem {
    EF,
    BA,
    KS
}