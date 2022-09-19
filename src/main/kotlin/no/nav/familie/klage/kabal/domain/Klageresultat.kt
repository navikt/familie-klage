package no.nav.familie.klage.kabal.domain

import no.nav.familie.klage.infrastruktur.config.DatabaseConfiguration.StringListWrapper
import no.nav.familie.klage.kabal.BehandlingEventType
import no.nav.familie.klage.kabal.ExternalUtfall
import org.springframework.data.annotation.Id
import java.time.LocalDateTime
import java.util.UUID

class Klageresultat(
    @Id
    val eventId: UUID,
    val type: BehandlingEventType,
    val utfall: ExternalUtfall?,
    val hendelseTidspunkt: LocalDateTime,
    val kildereferanse: UUID,
    val journalpostReferanser: StringListWrapper
)
