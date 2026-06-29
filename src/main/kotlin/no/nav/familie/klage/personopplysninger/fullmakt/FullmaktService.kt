package no.nav.familie.klage.personopplysninger.fullmakt

import no.nav.familie.klage.personopplysninger.pdl.Fullmakt
import no.nav.familie.klage.personopplysninger.pdl.MotpartsRolle
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException

@Service
class FullmaktService(
    val fullmaktClient: FullmaktClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun hentFullmakt(ident: String): FullmaktResultat =
        try {
            val fullmaktResponse = fullmaktClient.hentFullmakt(ident)
            FullmaktResultat(
                fullmakt =
                    fullmaktResponse.map {
                        Fullmakt(
                            gyldigFraOgMed = it.gyldigFraOgMed,
                            gyldigTilOgMed = it.gyldigTilOgMed,
                            motpartsPersonident = it.fullmektig,
                            fullmektigsNavn = it.fullmektigsNavn,
                            motpartsRolle = MotpartsRolle.FULLMEKTIG,
                            it.omraade.map { it.tema },
                        )
                    },
                harTilgang = true,
            )
        } catch (e: HttpClientErrorException.Forbidden) {
            logger.warn("Saksbehandler mangler tilgang til fullmakt i repr-api (403)")
            FullmaktResultat(fullmakt = emptyList(), harTilgang = false)
        }
}

data class FullmaktResultat(
    val fullmakt: List<Fullmakt>,
    val harTilgang: Boolean,
)
