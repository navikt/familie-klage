package no.nav.familie.klage.personopplysninger.fullmakt

import no.nav.familie.klage.personopplysninger.pdl.Fullmakt
import no.nav.familie.klage.personopplysninger.pdl.MotpartsRolle
import no.nav.familie.klage.personopplysninger.pdl.PdlClient
import no.nav.familie.klage.personopplysninger.pdl.gjeldende
import no.nav.familie.klage.personopplysninger.pdl.visningsnavn
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException

@Service
class FullmaktService(
    val fullmaktClient: FullmaktClient,
    private val pdlClient: PdlClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun hentFullmakt(
        ident: String,
        stønadstype: Stønadstype,
    ): FullmaktResultat =
        try {
            val fullmaktResponse = fullmaktClient.hentFullmakt(ident)
            val fullmektigesNavn = hentNavn(fullmaktResponse.map { it.fullmektig }, stønadstype)

            return FullmaktResultat(
                fullmakt =
                    fullmaktResponse.map {
                        Fullmakt(
                            gyldigFraOgMed = it.gyldigFraOgMed,
                            gyldigTilOgMed = it.gyldigTilOgMed,
                            motpartsPersonident = it.fullmektig,
                            fullmektigsNavn = fullmektigesNavn[it.fullmektig],
                            motpartsRolle = MotpartsRolle.FULLMEKTIG,
                            omraader = it.leserettigheter.plus(it.skriverettigheter).distinct(),
                        )
                    },
                harTilgang = true,
            )
        } catch (e: HttpClientErrorException.Forbidden) {
            logger.warn("Saksbehandler mangler tilgang til fullmakt i repr-api (403)")
            FullmaktResultat(fullmakt = emptyList(), harTilgang = false)
        }

    private fun hentNavn(
        identer: List<String>,
        stønadstype: Stønadstype,
    ): Map<String, String> =
        identer
            .distinct()
            .takeIf { it.isNotEmpty() }
            ?.let { pdlClient.hentNavnBolk(it, stønadstype) }
            ?.mapValues {
                it.value.navn
                    .gjeldende()
                    .visningsnavn()
            }
            ?: emptyMap()
}

data class FullmaktResultat(
    val fullmakt: List<Fullmakt>,
    val harTilgang: Boolean,
)
