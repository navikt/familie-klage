package no.nav.familie.klage.vurdering.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDateTime
import java.util.UUID

data class Vurdering(
    @Id
    val behandlingId: UUID = UUID.randomUUID(),
    val oppfyltFormkrav: Int,
    val muligFormkrav: Int,
    val begrunnelse: String,
    val vedtakValg: VedtakValg,
    @Column("årsak")
    val årsak: Årsak? = null,
    val hjemmel: Hjemmel? = null,
    val beskrivelse: String,
    @Column("fullført_dato")
    val fullførtDato: LocalDateTime? = null
)

enum class VedtakValg {
    OMGJØR_VEDTAK,
    OPPRETTHOLD_VEDTAK,
}

enum class Årsak {
    SAKSBEHANDLINGSFEIL,
    TODO1,
    TODO2,
}

enum class Hjemmel {
    FEMTEN_TO,
    FEMTEN_TRE,
    FEMTEN_FIRE,
}