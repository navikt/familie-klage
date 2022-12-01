package no.nav.familie.klage.testutil

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.kabal.AnkebehandlingAvsluttetDetaljer
import no.nav.familie.klage.kabal.AnkebehandlingOpprettetDetaljer
import no.nav.familie.klage.kabal.BehandlingDetaljer
import no.nav.familie.klage.kabal.BehandlingEvent
import no.nav.familie.klage.kabal.KlagebehandlingAvsluttetDetaljer
import no.nav.familie.kontrakter.felles.klage.BehandlingEventType
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.KlageinstansUtfall
import java.time.LocalDateTime
import java.util.UUID

object KabalEventUtil {

    fun klagebehandlingAvsluttet(
        behandling: Behandling,
        utfall: KlageinstansUtfall = KlageinstansUtfall.AVVIST
    ) = kabalEvent(
        behandling,
        BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET,
        BehandlingDetaljer(
            KlagebehandlingAvsluttetDetaljer(
                avsluttet = LocalDateTime.now(),
                utfall = utfall,
                journalpostReferanser = listOf("journalpost1")
            )
        )
    )

    fun ankeOpprettet(
        behandling: Behandling,
    ) = kabalEvent(
        behandling,
        BehandlingEventType.ANKEBEHANDLING_OPPRETTET,
        BehandlingDetaljer(
            ankebehandlingOpprettet = AnkebehandlingOpprettetDetaljer(
                mottattKlageinstans = LocalDateTime.now()
            )
        )
    )

    fun ankeAvsluttet(
        behandling: Behandling,
    ) = kabalEvent(
        behandling,
        BehandlingEventType.ANKEBEHANDLING_AVSLUTTET,
        BehandlingDetaljer(
            ankebehandlingAvsluttet = AnkebehandlingAvsluttetDetaljer(
                avsluttet = LocalDateTime.now(),
                utfall = KlageinstansUtfall.DELVIS_MEDHOLD,
                journalpostReferanser = listOf("1", "2", "3")
            )
        )
    )

    private fun kabalEvent(
        behandling: Behandling,
        type: BehandlingEventType,
        BehandlingDetaljer: BehandlingDetaljer
    ) = BehandlingEvent(
        eventId = UUID.randomUUID(),
        kildeReferanse = behandling.eksternBehandlingId.toString(),
        kilde = Fagsystem.EF.name,
        kabalReferanse = UUID.randomUUID().toString(),
        type = type,
        detaljer = BehandlingDetaljer
    )
}