package no.nav.familie.klage.infrastruktur

import no.nav.familie.klage.behandling.BehandlingRepository
import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.kabal.BehandlingEvent
import no.nav.familie.klage.kabal.event.BehandlingEventService
import no.nav.familie.klage.repository.findByIdOrThrow
import no.nav.familie.klage.testutil.KabalEventUtil.ankeAvsluttet
import no.nav.familie.klage.testutil.KabalEventUtil.ankeOpprettet
import no.nav.familie.klage.testutil.KabalEventUtil.klagebehandlingAvsluttet
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/test/kabal"])
@Validated
@Unprotected
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
        behandlingEventService.handleEvent(klagebehandlingAvsluttet(behandling))
    }

    @PostMapping("{behandlingId}/startanke")
    fun opprettDummyKabalAnkeEvent(@PathVariable behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandlingEventService.handleEvent(ankeOpprettet(behandling))
    }

    @PostMapping("{behandlingId}/avsluttanke")
    fun opprettDummyKabalAvsluttAnkeEvent(@PathVariable behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandlingEventService.handleEvent(ankeAvsluttet(behandling))
    }
}
