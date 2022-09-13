package no.nav.familie.klage.testutil

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.BehandlingStatus
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.fagsak.domain.FagsakDomain
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random

object DomainUtil {

    fun fagsakDomain(
        id: UUID = UUID.randomUUID(),
        stønadstype: Stønadstype = Stønadstype.OVERGANGSSTØNAD,
        personId: UUID = UUID.randomUUID(),
        fagsystem: Fagsystem = Fagsystem.EF,
        eksternId: String = Random.nextInt().toString()
    ): FagsakDomain =
        FagsakDomain(
            id = id,
            fagsakPersonId = personId,
            stønadstype = stønadstype,
            eksternId = eksternId,
            fagsystem = fagsystem
        )

    fun behandling(
        id: UUID = UUID.randomUUID(),
        fagsakId: UUID = UUID.randomUUID(),
        eksternBehandlingId: String = Random.nextInt().toString(),
        klageMottatt: LocalDate = LocalDate.now(),
        status: BehandlingStatus = BehandlingStatus.OPPRETTET,
        steg: StegType = StegType.FORMKRAV

    ): Behandling =
        Behandling(
            id = id,
            fagsakId = fagsakId,
            eksternBehandlingId = eksternBehandlingId,
            klageMottatt = klageMottatt,
            status = status,
            steg = steg
        )
}
