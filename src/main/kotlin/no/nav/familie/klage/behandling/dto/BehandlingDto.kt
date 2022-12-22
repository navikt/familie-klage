package no.nav.familie.klage.behandling.dto

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.FagsystemRevurdering
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype.IKKE_VALGT
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype.INFOTRYGD_TILBAKEKREVING
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype.UTEN_VEDTAK
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype.UTESTENGELSE
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype.VEDTAK
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
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
    val fagsystemRevurdering: FagsystemRevurdering?
)

/**
 * @param fagsystemVedtak skal ikke brukes ved innsending, men kun når vi sender ut data
 */
data class PåklagetVedtakDto(
    val eksternFagsystemBehandlingId: String?,
    val påklagetVedtakstype: PåklagetVedtakstype,
    val fagsystemVedtak: FagsystemVedtak? = null,
    @Deprecated("Bruk manuell vedtaksdato")
    val vedtaksdatoInfotrygd: LocalDate? = null,
    val manuellVedtaksdato: LocalDate? = null
) {
    fun erGyldig(): Boolean = when (eksternFagsystemBehandlingId) {
        null -> påklagetVedtakstype != VEDTAK
        else -> påklagetVedtakstype == VEDTAK
    }

    fun harTattStillingTil(): Boolean = when (påklagetVedtakstype) {
        IKKE_VALGT -> false
        INFOTRYGD_TILBAKEKREVING, UTEN_VEDTAK, VEDTAK, UTESTENGELSE -> true
    }

    fun manglerVedtaksDato(): Boolean {
        if (påklagetVedtakstype == INFOTRYGD_TILBAKEKREVING || påklagetVedtakstype == UTESTENGELSE) {
            return utledManuellVedtaksdato() == null
        }
        return false
    }

    fun utledManuellVedtaksdato(): LocalDate? {
        return if(manuellVedtaksdato != null){
            manuellVedtaksdato
        } else if(vedtaksdatoInfotrygd != null){
            vedtaksdatoInfotrygd
        } else {
            null
        }
    }
}

fun Behandling.tilDto(fagsak: Fagsak, klageinstansResultat: List<KlageinstansResultatDto>): BehandlingDto =
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
        fagsystemRevurdering = this.fagsystemRevurdering
    )
