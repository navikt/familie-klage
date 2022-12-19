package no.nav.familie.klage.behandling.domain

import no.nav.familie.klage.felles.domain.Sporbar
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.klage.HenlagtÅrsak
import no.nav.familie.kontrakter.felles.klage.VedtakType
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Behandling(
    @Id
    val id: UUID = UUID.randomUUID(),
    val fagsakId: UUID,
    val steg: StegType = StegType.FORMKRAV,
    val status: BehandlingStatus = BehandlingStatus.OPPRETTET,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
    val resultat: BehandlingResultat = BehandlingResultat.IKKE_SATT,
    val vedtakDato: LocalDateTime? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val påklagetVedtak: PåklagetVedtak,
    val klageMottatt: LocalDate,
    val behandlendeEnhet: String,
    val eksternBehandlingId: UUID = UUID.randomUUID(),
    @Column("henlagt_arsak")
    val henlagtÅrsak: HenlagtÅrsak? = null,
    val fagsystemRevurdering: FagsystemRevurdering? = null
)

data class PåklagetVedtakDetaljer(
    val fagsystemType: VedtakType,
    val eksternFagsystemBehandlingId: String?,
    val behandlingstype: String,
    val resultat: String,
    val vedtakstidspunkt: LocalDateTime
)

data class PåklagetVedtak(
    @Column("paklaget_vedtak")
    val påklagetVedtakstype: PåklagetVedtakstype = PåklagetVedtakstype.IKKE_VALGT,
    @Column("paklaget_vedtak_detaljer")
    val påklagetVedtakDetaljer: PåklagetVedtakDetaljer? = null
)

enum class PåklagetVedtakstype {
    VEDTAK,
    INFOTRYGD_TILBAKEKREVING,
    UTEN_VEDTAK,
    IKKE_VALGT
}

fun BehandlingStatus.erLåstForVidereBehandling() =
    when (SikkerhetContext.hentSaksbehandler()) {
        SikkerhetContext.SYSTEM_FORKORTELSE -> this != BehandlingStatus.VENTER
        else -> setOf(BehandlingStatus.VENTER, BehandlingStatus.FERDIGSTILT).contains(this)
    }

fun BehandlingStatus.erUnderArbeidAvSaksbehandler() = setOf(BehandlingStatus.OPPRETTET, BehandlingStatus.UTREDES).contains(this)

enum class StegType(
    val rekkefølge: Int,
    val gjelderStatus: BehandlingStatus
) {
    // Det blir opprettet et innslag i behandlingshistorikken ved opprettelse av behandlingen. Steget blir samtidig satt til FORMKRAV.
    // En behandling vil derfor i praksis aldri befinne seg i steget OPPRETTET. Opprettet-innslaget brukes for visning i frontend.
    OPPRETTET(
        rekkefølge = 0,
        gjelderStatus = BehandlingStatus.OPPRETTET
    ),
    FORMKRAV(
        rekkefølge = 1,
        gjelderStatus = BehandlingStatus.UTREDES
    ),
    VURDERING(
        rekkefølge = 2,
        gjelderStatus = BehandlingStatus.UTREDES
    ),
    BREV(
        rekkefølge = 3,
        gjelderStatus = BehandlingStatus.UTREDES
    ),
    OVERFØRING_TIL_KABAL(
        rekkefølge = 4,
        gjelderStatus = BehandlingStatus.VENTER
    ),
    KABAL_VENTER_SVAR(
        rekkefølge = 5,
        gjelderStatus = BehandlingStatus.VENTER
    ),
    BEHANDLING_FERDIGSTILT(
        rekkefølge = 6,
        gjelderStatus = BehandlingStatus.FERDIGSTILT
    );
}
