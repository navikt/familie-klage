package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.BehandlingResultat
import no.nav.familie.klage.behandling.domain.BehandlingStatus
import no.nav.familie.klage.behandling.domain.BehandlingSteg
import no.nav.familie.klage.behandling.domain.BehandlingType
import no.nav.familie.klage.behandling.dto.BehandlingDto
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.ef.StønadType
import java.time.LocalDateTime
import java.util.UUID

fun behandlingDto(
        id: UUID = UUID.randomUUID(),
        forrigeBehandlingId: UUID?= null,
        fagsakId: UUID = UUID.randomUUID(),
        steg: BehandlingSteg = BehandlingSteg.FORMALKRAV,
        type: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
        status: BehandlingStatus = BehandlingStatus.UTREDES,
        sistEndret: LocalDateTime = LocalDateTime.now().minusDays(1),
        resultat: BehandlingResultat = BehandlingResultat.IKKE_SATT,
        opprettet: LocalDateTime = LocalDateTime.now().minusDays(2),
        behandlingsårsak: BehandlingÅrsak = BehandlingÅrsak.KLAGE,
        stønadstype: StønadType = StønadType.OVERGANGSSTØNAD,
        vedtaksdato: LocalDateTime? = null
): BehandlingDto =
        BehandlingDto(
                id,
                forrigeBehandlingId,
                fagsakId,
                steg,
                type,
                status,
                sistEndret,
                resultat,
                opprettet,
                behandlingsårsak,
                stønadstype,
                vedtaksdato
        )
