package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.BehandlingResultat
import no.nav.familie.klage.behandling.domain.BehandlingStatus
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandling.domain.BehandlingsÅrsak
import no.nav.familie.klage.behandling.domain.StønadsType
import no.nav.familie.klage.behandling.dto.BehandlingDto
import no.nav.familie.kontrakter.felles.Fagsystem
import java.time.LocalDateTime
import java.util.UUID

fun behandlingDto(
        id: UUID = UUID.randomUUID(),
        fagsakId: UUID = UUID.randomUUID(),
        personId: String,
        steg: StegType = StegType.FORMKRAV,
        status: BehandlingStatus = BehandlingStatus.UTREDES,
        sistEndret: LocalDateTime = LocalDateTime.now().minusDays(1),
        resultat: BehandlingResultat? = null,
        opprettet: LocalDateTime = LocalDateTime.now().minusDays(2),
        fagsystem: Fagsystem = Fagsystem.EF,
        vedtaksdato: LocalDateTime? = null,
        stonadsType: StønadsType = StønadsType.BARNETILSYN,
        behandlingsArsak: BehandlingsÅrsak = BehandlingsÅrsak.KLAGE,
): BehandlingDto =
        BehandlingDto(
                id,
                fagsakId,
                personId,
                steg,
                status,
                sistEndret,
                resultat,
                opprettet,
                fagsystem,
                vedtaksdato,
                stonadsType,
                behandlingsArsak
        )
