package no.nav.familie.klage.behandling.dto

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.FagsystemRevurdering
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype.AVVIST_KLAGE
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype.IKKE_VALGT
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype.VEDTAK
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandling.domain.harManuellVedtaksdato
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.institusjon.Institusjon
import no.nav.familie.kontrakter.felles.Regelverk
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import no.nav.familie.kontrakter.felles.klage.Klagebehandlingsårsak
import no.nav.familie.kontrakter.felles.klage.KlageinstansResultatDto
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class BehandlingDto(
    val id: UUID,
    val fagsakId: UUID,
    val steg: StegType,
    val status: BehandlingStatus,
    val sistEndret: LocalDateTime,
    val resultat: BehandlingResultat,
    val opprettet: LocalDateTime,
    val vedtaksdato: LocalDateTime? = null,
    val stønadstype: Stønadstype,
    val klageinstansResultat: List<KlageinstansResultatDto>,
    val påklagetVedtak: PåklagetVedtakDto,
    val eksternFagsystemFagsakId: String,
    val fagsystem: Fagsystem,
    val klageMottatt: LocalDate,
    val fagsystemRevurdering: FagsystemRevurdering?,
    val årsak: Klagebehandlingsårsak,
    val behandlendeEnhet: String,
    val institusjon: Institusjon? = null,
)

/**
 * @param fagsystemVedtak skal ikke brukes ved innsending, men kun når vi sender ut data
 */
data class PåklagetVedtakDto(
    val eksternFagsystemBehandlingId: String?,
    val internKlagebehandlingId: String?,
    val påklagetVedtakstype: PåklagetVedtakstype,
    val fagsystemVedtak: FagsystemVedtak? = null,
    val manuellVedtaksdato: LocalDate? = null,
    val regelverk: Regelverk? = null,
) {
    fun erGyldig(): Boolean =
        when (påklagetVedtakstype) {
            AVVIST_KLAGE -> internKlagebehandlingId != null
            VEDTAK -> eksternFagsystemBehandlingId != null
            else -> internKlagebehandlingId == null && eksternFagsystemBehandlingId == null
        }

    fun harTattStillingTil(): Boolean = påklagetVedtakstype != IKKE_VALGT

    fun manglerVedtaksDato(): Boolean {
        if (påklagetVedtakstype.harManuellVedtaksdato()) {
            return manuellVedtaksdato == null
        }
        return false
    }
}

fun Behandling.tilDto(
    fagsak: Fagsak,
    klageinstansResultat: List<KlageinstansResultatDto>,
): BehandlingDto =
    BehandlingDto(
        id = this.id,
        fagsakId = this.fagsakId,
        steg = this.steg,
        status = this.status,
        sistEndret = this.sporbar.endret.endretTid,
        resultat = this.resultat,
        opprettet = this.sporbar.opprettetTid,
        stønadstype = fagsak.stønadstype,
        fagsystem = fagsak.fagsystem,
        eksternFagsystemFagsakId = fagsak.eksternId,
        klageinstansResultat = klageinstansResultat,
        påklagetVedtak = this.påklagetVedtak.tilDto(),
        klageMottatt = this.klageMottatt,
        fagsystemRevurdering = this.fagsystemRevurdering,
        årsak = this.årsak,
        behandlendeEnhet = this.behandlendeEnhet,
        institusjon = fagsak.institusjon,
    )
