package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.FagsystemRevurdering
import no.nav.familie.klage.behandling.domain.Klagebehandlingsresultat
import no.nav.familie.klage.behandling.domain.PåklagetVedtak
import no.nav.familie.klage.behandling.domain.PåklagetVedtakDetaljer
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype
import no.nav.familie.klage.behandling.domain.erLåstForVidereBehandling
import no.nav.familie.klage.behandling.domain.harManuellVedtaksdato
import no.nav.familie.klage.behandling.dto.BehandlingDto
import no.nav.familie.klage.behandling.dto.PåklagetVedtakDto
import no.nav.familie.klage.behandling.dto.tilDto
import no.nav.familie.klage.behandling.dto.tilPåklagetVedtakDetaljer
import no.nav.familie.klage.behandling.enhet.Enhet
import no.nav.familie.klage.behandling.enhet.EnhetValidator
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.klage.infrastruktur.exception.feilHvis
import no.nav.familie.klage.infrastruktur.exception.feilHvisIkke
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.klage.integrasjoner.FagsystemVedtakService
import no.nav.familie.klage.kabal.KlageresultatRepository
import no.nav.familie.klage.kabal.domain.tilDto
import no.nav.familie.klage.personopplysninger.pdl.secureLogger
import no.nav.familie.klage.repository.findByIdOrThrow
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.FagsystemType
import no.nav.familie.kontrakter.felles.klage.KlageinstansResultatDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class BehandlingService(
    private val behandlingRepository: BehandlingRepository,
    private val fagsakService: FagsakService,
    private val klageresultatRepository: KlageresultatRepository,
    private val fagsystemVedtakService: FagsystemVedtakService,
) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun hentBehandling(behandlingId: UUID): Behandling = behandlingRepository.findByIdOrThrow(behandlingId)

    fun hentBehandlingDto(behandlingId: UUID): BehandlingDto {
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        return behandlingRepository
            .findByIdOrThrow(behandlingId)
            .tilDto(fagsak, hentKlageresultatDto(behandlingId))
    }

    fun opprettBehandling(behandling: Behandling): Behandling = behandlingRepository.insert(behandling)

    fun hentKlageresultatDto(behandlingId: UUID): List<KlageinstansResultatDto> {
        val klageresultater = klageresultatRepository.findByBehandlingId(behandlingId)
        return klageresultater.tilDto()
    }

    fun finnKlagebehandlingsresultat(
        eksternFagsakId: String,
        fagsystem: Fagsystem,
    ): List<Klagebehandlingsresultat> = behandlingRepository.finnKlagebehandlingsresultat(eksternFagsakId, fagsystem)

    fun hentAktivIdent(behandlingId: UUID): Pair<String, Fagsak> {
        val behandling = hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
        return Pair(fagsak.hentAktivIdent(), fagsak)
    }

    fun oppdaterBehandlingMedResultat(
        behandlingId: UUID,
        behandlingsresultat: BehandlingResultat,
        opprettetRevurdering: FagsystemRevurdering?,
    ) {
        val behandling = hentBehandling(behandlingId)
        if (behandling.resultat != BehandlingResultat.IKKE_SATT) {
            error("Kan ikke endre på et resultat som allerede er satt")
        }
        val oppdatertBehandling =
            behandling.copy(
                resultat = behandlingsresultat,
                vedtakDato = LocalDateTime.now(),
                fagsystemRevurdering = opprettetRevurdering,
            )
        behandlingRepository.update(oppdatertBehandling)
    }

    @Transactional
    fun oppdaterBehandlendeEnhet(
        behandlingId: UUID,
        behandlendeEnhet: Enhet,
        fagsystem: Fagsystem,
    ) {
        val behandling = hentBehandling(behandlingId)

        if (behandling.status == BehandlingStatus.FERDIGSTILT) {
            throw Feil("Kan ikke endre behandlende enhet på ferdigstilt behandling=${behandling.id}")
        }

        EnhetValidator.validerEnhetForFagsystem(
            enhetsnummer = behandlendeEnhet.enhetsnummer,
            fagsystem = fagsystem,
        )

        val oppdatertBehandling =
            behandling.copy(
                behandlendeEnhet = behandlendeEnhet.enhetsnummer,
            )

        behandlingRepository.update(oppdatertBehandling)
    }

    @Transactional
    fun oppdaterPåklagetVedtak(
        behandlingId: UUID,
        påklagetVedtakDto: PåklagetVedtakDto,
    ) {
        val behandling = hentBehandling(behandlingId)
        brukerfeilHvis(behandling.status.erLåstForVidereBehandling()) {
            "Kan ikke oppdatere påklaget vedtak siden behandlingen er låst for videre saksbehandling"
        }
        feilHvisIkke(påklagetVedtakDto.erGyldig()) {
            "Påklaget vedtak er i en ugyldig tilstand: EksternFagsystemBehandlingId:${påklagetVedtakDto.eksternFagsystemBehandlingId}, InternKlagebehandlingId:${påklagetVedtakDto.internKlagebehandlingId}, PåklagetVedtakType: ${påklagetVedtakDto.påklagetVedtakstype}"
        }

        feilHvis(påklagetVedtakDto.manglerVedtaksDato()) {
            "Må fylle inn vedtaksdato når valgt vedtakstype er ${påklagetVedtakDto.påklagetVedtakstype}"
        }

        val påklagetVedtakDetaljer = påklagetVedtakDetaljer(behandlingId, påklagetVedtakDto)

        val behandlingMedPåklagetVedtak =
            behandling.copy(
                påklagetVedtak =
                    PåklagetVedtak(
                        påklagetVedtakstype = påklagetVedtakDto.påklagetVedtakstype,
                        påklagetVedtakDetaljer = påklagetVedtakDetaljer,
                    ),
            )
        behandlingRepository.update(behandlingMedPåklagetVedtak)
    }

    private fun påklagetVedtakDetaljer(
        behandlingId: UUID,
        påklagetVedtakDto: PåklagetVedtakDto,
    ): PåklagetVedtakDetaljer? {
        if (påklagetVedtakDto.påklagetVedtakstype.harManuellVedtaksdato()) {
            return tilPåklagetVedtakDetaljerMedManuellDato(påklagetVedtakDto)
        }
        if (påklagetVedtakDto.påklagetVedtakstype === PåklagetVedtakstype.AVVIST_KLAGE) {
            return tilPåklagetKlageAvvistVedtak(påklagetVedtakDto)
        }
        return påklagetVedtakDto.eksternFagsystemBehandlingId?.let {
            fagsystemVedtakService
                .hentFagsystemVedtakForPåklagetBehandlingId(behandlingId, it)
                .tilPåklagetVedtakDetaljer()
        }
    }

    private fun tilPåklagetVedtakDetaljerMedManuellDato(påklagetVedtakDto: PåklagetVedtakDto) =
        PåklagetVedtakDetaljer(
            fagsystemType = utledFagsystemType(påklagetVedtakDto),
            eksternFagsystemBehandlingId = null,
            internKlagebehandlingId = null,
            behandlingstype = "",
            resultat = "",
            vedtakstidspunkt = påklagetVedtakDto.manuellVedtaksdato?.atStartOfDay() ?: error("Mangler vedtaksdato"),
            regelverk = påklagetVedtakDto.regelverk,
        )

    private fun tilPåklagetKlageAvvistVedtak(påklagetVedtakDto: PåklagetVedtakDto) =
        PåklagetVedtakDetaljer(
            fagsystemType = utledFagsystemType(påklagetVedtakDto),
            eksternFagsystemBehandlingId = null,
            internKlagebehandlingId = påklagetVedtakDto.internKlagebehandlingId,
            behandlingstype = "Klage",
            resultat = "Ikke medhold formkrav avvist",
            vedtakstidspunkt =
                hentBehandling(UUID.fromString(påklagetVedtakDto.internKlagebehandlingId)).vedtakDato ?: error("Mangler vedtaksdato"),
            regelverk = påklagetVedtakDto.regelverk,
        )

    private fun utledFagsystemType(påklagetVedtakDto: PåklagetVedtakDto): FagsystemType =
        when (påklagetVedtakDto.påklagetVedtakstype) {
            PåklagetVedtakstype.INFOTRYGD_TILBAKEKREVING -> FagsystemType.TILBAKEKREVING
            PåklagetVedtakstype.UTESTENGELSE -> FagsystemType.UTESTENGELSE
            PåklagetVedtakstype.INFOTRYGD_ORDINÆRT_VEDTAK -> FagsystemType.ORDNIÆR
            PåklagetVedtakstype.AVVIST_KLAGE -> FagsystemType.ORDNIÆR
            else -> error("Kan ikke utlede fagsystemType for påklagetVedtakType ${påklagetVedtakDto.påklagetVedtakstype}")
        }

    fun oppdaterStatusPåBehandling(
        behandlingId: UUID,
        status: BehandlingStatus,
    ): Behandling {
        val behandling = hentBehandling(behandlingId = behandlingId)
        secureLogger.info(
            "${SikkerhetContext.hentSaksbehandler()} endrer status på behandling $behandlingId " +
                "fra ${behandling.status} til $status",
        )
        return behandlingRepository.update(t = behandling.copy(status = status))
    }

    fun hentKlagerIkkeMedholdFormkravAvvist(behandlingId: UUID): List<Klagebehandlingsresultat> {
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        return finnKlagebehandlingsresultat(fagsak.eksternId, fagsak.fagsystem).filter { klage ->
            klage.resultat ==
                BehandlingResultat.IKKE_MEDHOLD_FORMKRAV_AVVIST
        }
    }
}
