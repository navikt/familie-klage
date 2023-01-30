package no.nav.familie.klage.oppgave

import no.nav.familie.klage.felles.domain.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class BehandleSakOppgave(
    @Id
    val behandlingId: UUID,
    val oppgaveId: Long,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)
