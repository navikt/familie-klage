package no.nav.familie.klage.behandling.domain

import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.klage.KlagebehandlingDto
import no.nav.familie.kontrakter.felles.klage.KlageinstansResultatDto
import no.nav.familie.kontrakter.felles.klage.Årsak
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * Aggregering av behandling, fagsak, vedtak for å hente ut relevant informasjon i en spørring
 */
data class Klagebehandlingsesultat(
    val id: UUID,
    val fagsakId: UUID,
    val fagsakPersonId: UUID,
    val status: BehandlingStatus,
    val opprettet: LocalDateTime,
    val mottattDato: LocalDate,
    val resultat: BehandlingResultat,
    val årsak: Årsak?,
    val vedtaksdato: LocalDateTime?
)

fun Klagebehandlingsesultat.tilEksternKlagebehandlingDto(klageinstansResultat: List<KlageinstansResultatDto>) = KlagebehandlingDto(
    id = this.id,
    fagsakId = this.fagsakId,
    status = this.status,
    opprettet = this.opprettet,
    mottattDato = this.mottattDato,
    resultat = this.resultat,
    årsak = this.årsak,
    vedtaksdato = this.vedtaksdato,
    klageinstansResultat = klageinstansResultat
)
