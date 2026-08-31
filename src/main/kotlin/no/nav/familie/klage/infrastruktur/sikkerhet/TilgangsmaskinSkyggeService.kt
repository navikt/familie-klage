package no.nav.familie.klage.infrastruktur.sikkerhet

import io.micrometer.core.instrument.Metrics
import no.nav.familie.klage.felles.dto.Tilgang
import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.klage.infrastruktur.featuretoggle.Toggle
import no.nav.familie.tilgangsmaskin.Avvisningskode
import no.nav.familie.tilgangsmaskin.Regeltype
import no.nav.familie.tilgangsmaskin.TilgangsmaskinException
import no.nav.familie.tilgangsmaskin.TilgangsmaskinKlient
import no.nav.familie.tilgangsmaskin.TilgangsmaskinResultat
import org.slf4j.LoggerFactory
import org.springframework.core.NestedExceptionUtils
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

/**
 * Skyggekjøring av Tilgangsmaskinen (NAV-30363). Vi spør Tilgangsmaskinen i tillegg til dagens kall mot
 * familie-integrasjoner, sammenligner svarene og logger avvik. Tilgangsbeslutningen påvirkes aldri.
 *
 * Merk forskjellen fra ba-sak/ks-sak: der skygger vi et kall som allerede svarer per ident, så
 * sammenligningen er 1:1. Her skygger vi `POST /api/tilgang/person-med-relasjoner`, som gir **ett
 * aggregert svar** for søkeren og relasjonene hens – familie-integrasjoner slår opp relasjonene selv, så
 * klage kjenner ikke identene deres. Tilgangsmaskinen vurderer én person av gangen og har ingen
 * tilsvarende relasjonsmodus, derfor sammenligner vi kun på søkerens egen ident. Det gjør de to
 * retningene ulikt mye verdt:
 *
 * Dette er klages eneste direkte tilgangskall mot familie-integrasjoner (brukes for EF, søk og
 * vedlegg). Tilgang for BA-/KS-behandlinger delegeres til familie-ba-sak/familie-ks-sak, som har
 * egen skyggekjøring - metrikkene her dekker altså kun klages egne sjekker.
 *
 *  - `gammel=true, ny=false` er konklusivt: ga integrasjoner tilgang til søker *og* relasjoner, må
 *    søkeren selv ha vært tillatt, og et avslag fra Tilgangsmaskinen er et reelt avvik.
 *  - `gammel=false, ny=true` er **ikke** konklusivt: avslaget kan like gjerne ha kommet av en relasjon
 *    vi ikke spurte om. Det telles derfor separat, ikke som avvik.
 */
@Service
class TilgangsmaskinSkyggeService(
    private val tilgangsmaskinKlient: TilgangsmaskinKlient,
    private val featureToggleService: FeatureToggleService,
) {
    private val sammenlignetTeller = Metrics.counter("tilgangsmaskin.skygge.sammenlignet")
    private val manglendeSvarTeller = Metrics.counter("tilgangsmaskin.skygge.manglende.svar")

    fun skyggeSjekkTilgangTilPersonMedRelasjoner(
        personIdent: String,
        tilgangFraIntegrasjoner: Tilgang,
    ) {
        try {
            if (SikkerhetContext.erSystembruker()) return
            if (!featureToggleService.isEnabled(Toggle.SKAL_SKYGGEKJØRE_TILGANGSMASKINEN)) return

            val resultat =
                tilgangsmaskinKlient
                    .sjekkTilgangTilPersoner(setOf(personIdent), Regeltype.KJERNE_REGELTYPE)
                    .singleOrNull { it.personIdent == personIdent }

            if (resultat == null || resultat.erManglendeSvar()) {
                manglendeSvarTeller.increment()
                logger.warn("Tilgangsmaskin-skygge: fikk ikke svar for identen, sammenlignes ikke.")
                return
            }
            sammenlignetTeller.increment()

            when {
                tilgangFraIntegrasjoner.harTilgang == resultat.harTilgang -> {
                    logger.info("Tilgangsmaskin-skygge: sammenlignet 1 ident, ingen avvik.")
                }

                // Integrasjoner ga tilgang til søker og relasjoner, men Tilgangsmaskinen avviser søkeren selv.
                tilgangFraIntegrasjoner.harTilgang -> {
                    loggAvvik(tilgangFraIntegrasjoner, resultat)
                }

                // Integrasjoner avviste, men vi vet ikke om det var søkeren eller en relasjon som utløste det.
                else -> {
                    loggUavklart(tilgangFraIntegrasjoner, resultat)
                }
            }
        } catch (exception: Exception) {
            // Skyggingen skal aldri påvirke den gjeldende tilgangskontrollen.
            val rotårsak = NestedExceptionUtils.getMostSpecificCause(exception)
            val httpStatus = (exception as? TilgangsmaskinException)?.httpStatus
            Metrics
                .counter(
                    "tilgangsmaskin.skygge.feilet",
                    "feiltype",
                    rotårsak.javaClass.simpleName,
                    "httpStatus",
                    httpStatus?.toString() ?: "INGEN",
                ).increment()
            logger.warn("Tilgangsmaskin-skygge feilet: ${rotårsak.javaClass.simpleName}${httpStatus?.let { " (HTTP $it)" } ?: ""}")
            secureLogger.warn("Tilgangsmaskin-skygge feilet", exception)
        }
    }

    private fun TilgangsmaskinResultat.erManglendeSvar(): Boolean =
        !harTilgang &&
            httpStatus == HttpStatus.INTERNAL_SERVER_ERROR.value() &&
            avvisningskode == Avvisningskode.UKJENT

    private fun loggAvvik(
        tilgangFraIntegrasjoner: Tilgang,
        resultat: TilgangsmaskinResultat,
    ) {
        Metrics
            .counter(
                "tilgangsmaskin.skygge.avvik",
                "retning",
                Avviksretning.NY_STRENGERE.tag,
                "avvisningskode",
                resultat.avvisningskode?.name ?: "INGEN",
            ).increment()
        logger.warn(
            "Tilgangsmaskin-skygge: avvik (${Avviksretning.NY_STRENGERE.tag}). " +
                "Avvisningskode=${resultat.avvisningskode}, traceId=${resultat.traceId}. Se securelogs for detaljer.",
        )
        secureLogger.warn(
            "Tilgangsmaskin-skygge avvik (${Avviksretning.NY_STRENGERE.tag}) for ident ${resultat.personIdent}: " +
                "integrasjoner harTilgang=${tilgangFraIntegrasjoner.harTilgang} " +
                "(begrunnelse=${tilgangFraIntegrasjoner.begrunnelse}), " +
                "tilgangsmaskinen harTilgang=${resultat.harTilgang} (avvisningskode=${resultat.avvisningskode}, " +
                "begrunnelse=${resultat.begrunnelse}, kanOverstyres=${resultat.kanOverstyres}, " +
                "httpStatus=${resultat.httpStatus}, traceId=${resultat.traceId})",
        )
    }

    private fun loggUavklart(
        tilgangFraIntegrasjoner: Tilgang,
        resultat: TilgangsmaskinResultat,
    ) {
        Metrics.counter("tilgangsmaskin.skygge.uavklart", "retning", Avviksretning.NY_MILDERE.tag).increment()
        logger.info(
            "Tilgangsmaskin-skygge: integrasjoner avviste, Tilgangsmaskinen ga tilgang til søkeren. " +
                "Kan skyldes en relasjon vi ikke spør om, telles derfor ikke som avvik.",
        )
        secureLogger.info(
            "Tilgangsmaskin-skygge uavklart (${Avviksretning.NY_MILDERE.tag}) for ident ${resultat.personIdent}: " +
                "integrasjoner harTilgang=false (begrunnelse=${tilgangFraIntegrasjoner.begrunnelse}), " +
                "tilgangsmaskinen ga tilgang til søkeren (traceId=${resultat.traceId})",
        )
    }

    private enum class Avviksretning(
        val tag: String,
    ) {
        NY_MILDERE("ny-mildere"),
        NY_STRENGERE("ny-strengere"),
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TilgangsmaskinSkyggeService::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
