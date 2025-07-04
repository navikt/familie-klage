package no.nav.familie.klage.infrastruktur.sikkerhet

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.felles.domain.AuditLogger
import no.nav.familie.klage.felles.domain.AuditLoggerEvent
import no.nav.familie.klage.felles.domain.BehandlerRolle
import no.nav.familie.klage.felles.domain.CustomKeyValue
import no.nav.familie.klage.felles.domain.Sporingsdata
import no.nav.familie.klage.felles.dto.Tilgang
import no.nav.familie.klage.infrastruktur.config.FagsystemRolleConfig
import no.nav.familie.klage.infrastruktur.config.RolleConfig
import no.nav.familie.klage.infrastruktur.config.getValue
import no.nav.familie.klage.infrastruktur.exception.ManglerTilgang
import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.klage.infrastruktur.featuretoggle.Toggle
import no.nav.familie.klage.integrasjoner.FamilieBASakClient
import no.nav.familie.klage.integrasjoner.FamilieKSSakClient
import no.nav.familie.klage.personopplysninger.PersonopplysningerIntegrasjonerClient
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TilgangService(
    private val personopplysningerIntegrasjonerClient: PersonopplysningerIntegrasjonerClient,
    private val rolleConfig: RolleConfig,
    private val cacheManager: CacheManager,
    private val auditLogger: AuditLogger,
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val familieBASakClient: FamilieBASakClient,
    private val familieKSSakClient: FamilieKSSakClient,
    private val featureToggleService: FeatureToggleService,
) {
    fun validerTilgangTilPersonMedRelasjoner(
        personIdent: String,
        event: AuditLoggerEvent,
    ) {
        val tilgang = harTilgangTilPersonMedRelasjoner(personIdent)
        auditLogger.log(Sporingsdata(event, personIdent, tilgang))
        if (!tilgang.harTilgang) {
            throw ManglerTilgang(
                melding =
                    "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                        "har ikke tilgang til $personIdent eller dets barn",
                frontendFeilmelding = "Mangler tilgang til opplysningene. ${tilgang.utledÅrsakstekst()}",
            )
        }
    }

    fun validerTilgangTilFagsak(
        fagsakId: UUID,
        event: AuditLoggerEvent,
    ) {
        val fagsak = hentFagsak(fagsakId)
        val personIdent = fagsak.hentAktivIdent()

        val tilgang =
            when (fagsak.fagsystem) {
                Fagsystem.BA, Fagsystem.KS ->
                    harTilgangTilEksternFagsak(
                        eksternFagsakId = fagsak.eksternId,
                        personIdent = personIdent,
                        fagsystem = fagsak.fagsystem,
                    )
                Fagsystem.EF -> harTilgangTilPersonMedRelasjoner(personIdent)
            }

        auditLogger.log(Sporingsdata(event, personIdent, tilgang, custom1 = CustomKeyValue("fagsak", fagsakId.toString())))

        if (!tilgang.harTilgang) {
            throw ManglerTilgang(
                melding =
                    "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                        "har ikke tilgang til fagsak=$fagsakId",
                frontendFeilmelding = "Mangler tilgang til opplysningene. ${tilgang.utledÅrsakstekst()}",
            )
        }
    }

    fun validerTilgangTilEksternFagsak(
        eksternFagsakId: String,
        fagsystem: Fagsystem,
        event: AuditLoggerEvent,
    ) {
        val fagsak = fagsakService.hentFagsakForEksternIdOgFagsystem(eksternId = eksternFagsakId, fagsystem = fagsystem)
        val personIdent = fagsak.hentAktivIdent()

        val tilgang =
            when (fagsak.fagsystem) {
                Fagsystem.BA, Fagsystem.KS ->
                    harTilgangTilEksternFagsak(
                        eksternFagsakId = fagsak.eksternId,
                        personIdent = personIdent,
                        fagsystem = fagsak.fagsystem,
                    )
                Fagsystem.EF -> harTilgangTilPersonMedRelasjoner(personIdent)
            }

        auditLogger.log(Sporingsdata(event, personIdent, tilgang, custom1 = CustomKeyValue("fagsak", fagsak.id.toString())))

        if (!tilgang.harTilgang) {
            throw ManglerTilgang(
                melding =
                    "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                        "har ikke tilgang til fagsak=${fagsak.id}",
                frontendFeilmelding = "Mangler tilgang til opplysningene. ${tilgang.utledÅrsakstekst()}",
            )
        }
    }

    fun validerTilgangTilBehandling(
        behandlingId: UUID,
        event: AuditLoggerEvent,
    ) {
        val fagsak = hentFagsakForBehandling(behandlingId)
        val personIdent = fagsak.hentAktivIdent()

        val tilgang =
            when (fagsak.fagsystem) {
                Fagsystem.BA, Fagsystem.KS ->
                    harTilgangTilEksternFagsak(
                        eksternFagsakId = fagsak.eksternId,
                        personIdent = personIdent,
                        fagsystem = fagsak.fagsystem,
                    )
                Fagsystem.EF -> harTilgangTilPersonMedRelasjoner(personIdent)
            }

        auditLogger.log(
            Sporingsdata(
                event,
                personIdent,
                tilgang,
                custom1 = CustomKeyValue("behandling", behandlingId.toString()),
            ),
        )

        if (!tilgang.harTilgang) {
            throw ManglerTilgang(
                melding =
                    "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                        "har ikke tilgang til behandling=$behandlingId",
                frontendFeilmelding = "Mangler tilgang til opplysningene. ${tilgang.utledÅrsakstekst()}",
            )
        }
    }

    fun validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId: UUID) {
        validerHarRolleForBehandling(behandlingId, BehandlerRolle.SAKSBEHANDLER)
    }

    fun validerHarVeilederrolleTilStønadForBehandling(behandlingId: UUID) {
        validerHarRolleForBehandling(behandlingId, BehandlerRolle.VEILEDER)
    }

    fun validerHarVeilederrolleTilStønadForFagsak(fagsakId: UUID) {
        harTilgangTilFagsakGittRolle(fagsakId, BehandlerRolle.VEILEDER)
    }

    fun harEgenAnsattRolle(): Boolean = SikkerhetContext.harRolle(rolleConfig.egenAnsatt)

    fun harMinimumRolleTversFagsystem(minimumsrolle: BehandlerRolle): Boolean =
        harTilgangTilGittRolleForFagsystem(rolleConfig.ba, minimumsrolle) ||
            harTilgangTilGittRolleForFagsystem(rolleConfig.ef, minimumsrolle) ||
            harTilgangTilGittRolleForFagsystem(rolleConfig.ks, minimumsrolle)

    fun harTilgangTilBehandlingGittRolle(
        behandlingId: UUID,
        minimumsrolle: BehandlerRolle,
    ): Boolean = harTilgangTilFagsakGittRolle(behandlingService.hentBehandling(behandlingId).fagsakId, minimumsrolle)

    private fun harTilgangTilEksternFagsak(
        eksternFagsakId: String,
        personIdent: String,
        fagsystem: Fagsystem,
    ): Tilgang =
        if (featureToggleService.isEnabled(Toggle.BRUK_NY_TILGANG_KONTROLL_BAKS)) {
            when (fagsystem) {
                Fagsystem.BA -> familieBASakClient.harTilgangTilFagsak(eksternFagsakId)
                Fagsystem.KS -> familieKSSakClient.harTilgangTilFagsak(eksternFagsakId)
                else -> throw IllegalArgumentException("Ugyldig fagsystem: $eksternFagsakId. Validering av tilgang til ekstern fagsag støttes kun for BA og KS.")
            }
        } else {
            harTilgangTilPersonMedRelasjoner(personIdent)
        }

    private fun harTilgangTilPersonMedRelasjoner(personIdent: String): Tilgang =
        harSaksbehandlerTilgang("validerTilgangTilPersonMedBarn", personIdent) {
            personopplysningerIntegrasjonerClient.sjekkTilgangTilPersonMedRelasjoner(personIdent)
        }

    /**
     * Sjekker cache om tilgangen finnes siden tidligere, hvis ikke hentes verdiet med [hentVerdi]
     * Resultatet caches sammen med identen for saksbehandleren på gitt [cacheName]
     * @param cacheName navnet på cachen
     * @param verdi verdiet som man ønsket å hente cache for, eks behandlingId, eller personIdent
     */
    private fun <T> harSaksbehandlerTilgang(
        cacheName: String,
        verdi: T,
        hentVerdi: () -> Tilgang,
    ): Tilgang {
        val cache = cacheManager.getCache(cacheName) ?: error("Finner ikke cache=$cacheName")
        return cache.get(Pair(verdi, SikkerhetContext.hentSaksbehandler(true))) {
            hentVerdi()
        } ?: error("Finner ikke verdi fra cache=$cacheName")
    }

    private fun hentFagsak(fagsakId: UUID): Fagsak =
        cacheManager.getValue("fagsak", fagsakId) {
            fagsakService.hentFagsak(fagsakId)
        }

    private fun hentFagsakForBehandling(behandlingId: UUID): Fagsak =
        cacheManager.getValue("fagsakForBehandling", behandlingId) {
            fagsakService.hentFagsakForBehandling(behandlingId)
        }

    private fun validerHarRolleForBehandling(
        behandlingId: UUID,
        minumumRolle: BehandlerRolle,
    ) {
        if (!harTilgangTilBehandlingGittRolle(behandlingId, minumumRolle)) {
            throw ManglerTilgang(
                melding =
                    "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} har ikke tilgang " +
                        "til å utføre denne operasjonen som krever minimumsrolle $minumumRolle",
                frontendFeilmelding = "Mangler nødvendig saksbehandlerrolle for å utføre handlingen",
            )
        }
    }

    private fun harTilgangTilFagsakGittRolle(
        fagsakId: UUID,
        minimumsrolle: BehandlerRolle,
    ): Boolean {
        val stønadstype = hentFagsak(fagsakId).stønadstype
        return harTilgangTilGittRolle(stønadstype, minimumsrolle)
    }

    private fun harTilgangTilGittRolle(
        stønadstype: Stønadstype,
        minimumsrolle: BehandlerRolle,
    ): Boolean {
        val rolleForFagsystem =
            when (stønadstype) {
                Stønadstype.BARNETRYGD -> rolleConfig.ba
                Stønadstype.OVERGANGSSTØNAD, Stønadstype.BARNETILSYN, Stønadstype.SKOLEPENGER -> rolleConfig.ef
                Stønadstype.KONTANTSTØTTE -> rolleConfig.ks
            }
        return harTilgangTilGittRolleForFagsystem(rolleForFagsystem, minimumsrolle)
    }

    private fun harTilgangTilGittRolleForFagsystem(
        fagsystemRolleConfig: FagsystemRolleConfig,
        minimumsrolle: BehandlerRolle,
    ): Boolean {
        val rollerFraToken = SikkerhetContext.hentGrupperFraToken()
        val rollerForBruker =
            when {
                SikkerhetContext.hentSaksbehandler() == SikkerhetContext.SYSTEM_FORKORTELSE ->
                    listOf(
                        BehandlerRolle.SYSTEM,
                        BehandlerRolle.BESLUTTER,
                        BehandlerRolle.SAKSBEHANDLER,
                        BehandlerRolle.VEILEDER,
                    )
                rollerFraToken.contains(fagsystemRolleConfig.beslutter) ->
                    listOf(
                        BehandlerRolle.BESLUTTER,
                        BehandlerRolle.SAKSBEHANDLER,
                        BehandlerRolle.VEILEDER,
                    )
                rollerFraToken.contains(fagsystemRolleConfig.saksbehandler) ->
                    listOf(
                        BehandlerRolle.SAKSBEHANDLER,
                        BehandlerRolle.VEILEDER,
                    )
                rollerFraToken.contains(fagsystemRolleConfig.veileder) -> listOf(BehandlerRolle.VEILEDER)
                else -> listOf(BehandlerRolle.UKJENT)
            }

        return rollerForBruker.contains(minimumsrolle)
    }
}
