package no.nav.familie.klage.infrastruktur.sikkerhet

import no.nav.familie.klage.felles.domain.AuditLogger
import no.nav.familie.klage.felles.domain.AuditLoggerEvent
import no.nav.familie.klage.felles.domain.BehandlerRolle
import no.nav.familie.klage.felles.domain.Sporingsdata
import no.nav.familie.klage.felles.dto.Tilgang
import no.nav.familie.klage.infrastruktur.config.RolleConfig
import no.nav.familie.klage.infrastruktur.exception.ManglerTilgang
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext.hentGrupperFraToken
import no.nav.familie.klage.personopplysninger.PersonopplysningerIntegrasjonerClient
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service

@Service
class TilgangService(
        private val personopplysningerIntegrasjonerClient: PersonopplysningerIntegrasjonerClient,
        private val rolleConfig: RolleConfig,
        private val cacheManager: CacheManager,
        private val auditLogger: AuditLogger
) {

    /**
     * Kun ved tilgangskontroll for enskild person, ellers bruk [validerTilgangTilPersonMedBarn]
     */
    fun validerTilgangTilPerson(personIdent: String, event: AuditLoggerEvent) {
        val tilgang = personopplysningerIntegrasjonerClient.sjekkTilgangTilPerson(personIdent)
        auditLogger.log(Sporingsdata(event, personIdent, tilgang))
        if (!tilgang.harTilgang) {
            throw ManglerTilgang(
                    melding = "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                              "har ikke tilgang til $personIdent",
                    frontendFeilmelding = "Mangler tilgang til opplysningene. ${tilgang.utledÅrsakstekst()}"
            )
        }
    }

    fun validerTilgangTilPersonMedBarn(personIdent: String, event: AuditLoggerEvent) {
        val tilgang = harTilgangTilPersonMedRelasjoner(personIdent)
        auditLogger.log(Sporingsdata(event, personIdent, tilgang))
        if (!tilgang.harTilgang) {
            throw ManglerTilgang(
                    melding = "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                              "har ikke tilgang til $personIdent eller dets barn",
                    frontendFeilmelding = "Mangler tilgang til opplysningene. ${tilgang.utledÅrsakstekst()}"
            )
        }
    }

//    fun validerTilgangTilBehandling(behandlingId: UUID, event: AuditLoggerEvent) {
//        val personIdent = cacheManager.getValue("behandlingPersonIdent", behandlingId) {
//            behandlingService.hentAktivIdent(behandlingId)
//        }
//        val tilgang = harTilgangTilPersonMedRelasjoner(personIdent)
//        auditLogger.log(
//                Sporingsdata(
//                        event, personIdent, tilgang,
//                        custom1 = CustomKeyValue("behandling", behandlingId.toString())
//                )
//        )
//        if (!tilgang.harTilgang) {
//            throw ManglerTilgang(
//                    melding = "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
//                              "har ikke tilgang til behandling=$behandlingId",
//                    frontendFeilmelding = "Mangler tilgang til opplysningene. ${tilgang.utledÅrsakstekst()}"
//            )
//        }
//    }

    private fun harTilgangTilPersonMedRelasjoner(personIdent: String): Tilgang {
        return harSaksbehandlerTilgang("validerTilgangTilPersonMedBarn", personIdent) {
            personopplysningerIntegrasjonerClient.sjekkTilgangTilPersonMedRelasjoner(personIdent)
        }
    }

    fun validerHarSaksbehandlerrolle() {
        validerTilgangTilRolle(BehandlerRolle.SAKSBEHANDLER)
    }

    fun validerHarBeslutterrolle() {
        validerTilgangTilRolle(BehandlerRolle.BESLUTTER)
    }

    fun validerTilgangTilRolle(minimumsrolle: BehandlerRolle) {
        if (!harTilgangTilRolle(minimumsrolle)) {
            throw ManglerTilgang(
                    melding = "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} har ikke tilgang " +
                              "til å utføre denne operasjonen som krever minimumsrolle $minimumsrolle",
                    frontendFeilmelding = "Mangler nødvendig saksbehandlerrolle for å utføre handlingen"
            )
        }
    }

    fun harTilgangTilRolle(minimumsrolle: BehandlerRolle): Boolean {
        return SikkerhetContext.harTilgangTilGittRolle(rolleConfig, minimumsrolle)
    }

//    /**
//     * Filtrerer data basert på om man har tilgang til den eller ikke
//     * Filtrer ikke på egen ansatt
//     */
//    fun <T> filtrerUtFortroligDataForRolle(values: List<T>, fn: (T) -> Adressebeskyttelse?): List<T> {
//        val grupper = hentGrupperFraToken()
//        val kode6gruppe = grupper.contains(rolleConfig.kode6)
//        val kode7Gruppe = grupper.contains(rolleConfig.kode7)
//        return values.filter {
//            when (fn(it)?.gradering) {
//                FORTROLIG -> kode7Gruppe
//                STRENGT_FORTROLIG, STRENGT_FORTROLIG_UTLAND -> kode6gruppe
//                else -> (!kode6gruppe)
//            }
//        }
//    }

    /**
     * Sjekker cache om tilgangen finnes siden tidligere, hvis ikke hentes verdiet med [hentVerdi]
     * Resultatet caches sammen med identen for saksbehandleren på gitt [cacheName]
     * @param cacheName navnet på cachen
     * @param verdi verdiet som man ønsket å hente cache for, eks behandlingId, eller personIdent
     */
    private fun <T> harSaksbehandlerTilgang(cacheName: String, verdi: T, hentVerdi: () -> Tilgang): Tilgang {
        val cache = cacheManager.getCache(cacheName) ?: error("Finner ikke cache=$cacheName")
        return cache.get(Pair(verdi, SikkerhetContext.hentSaksbehandler(true))) {
            hentVerdi()
        } ?: error("Finner ikke verdi fra cache=$cacheName")
    }

    fun validerSaksbehandler(saksbehandler: String): Boolean {
        return SikkerhetContext.hentSaksbehandler() == saksbehandler
    }
}
