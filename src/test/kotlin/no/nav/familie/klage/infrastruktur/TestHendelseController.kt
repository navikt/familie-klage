package no.nav.familie.klage.infrastruktur

import no.nav.familie.klage.behandling.BehandlingRepository
import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.kabal.BehandlingDetaljer
import no.nav.familie.klage.kabal.BehandlingEvent
import no.nav.familie.klage.kabal.BehandlingEventType
import no.nav.familie.klage.kabal.ExternalUtfall
import no.nav.familie.klage.kabal.KlagebehandlingAvsluttetDetaljer
import no.nav.familie.klage.kabal.event.BehandlingEventService
import no.nav.familie.klage.repository.findByIdOrThrow
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/test/kabal"])
@Validated
class TestHendelseController(
    private val behandlingRepository: BehandlingRepository,
    private val behandlingEventService: BehandlingEventService
) {

    @GetMapping("{behandlingId}")
    fun hentBehandling(@PathVariable behandlingId: UUID): Behandling {
        return behandlingRepository.findByIdOrThrow(behandlingId)
    }

    @PostMapping
    fun opprettKabalEvent(@RequestBody behandlingEvent: BehandlingEvent) {
        behandlingEventService.handleEvent(behandlingEvent)
    }

    @PostMapping("{behandlingId}/dummy")
    fun opprettDummyKabalEvent(@PathVariable behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandlingEventService.handleEvent(
            BehandlingEvent(
                eventId = UUID.randomUUID(),
                kildeReferanse = behandling.eksternBehandlingId.toString(),
                kilde = Fagsystem.EF.name,
                kabalReferanse = UUID.randomUUID().toString(),
                type = BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET,
                detaljer = BehandlingDetaljer(
                    KlagebehandlingAvsluttetDetaljer(
                        avsluttet = LocalDateTime.now(),
                        utfall = ExternalUtfall.MEDHOLD,
                        journalpostReferanser = listOf("journalpost1")
                    )
                )
            )
        )
    }
}