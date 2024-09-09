package no.nav.familie.klage.personopplysninger.fullmakt

import no.nav.familie.klage.personopplysninger.pdl.Fullmakt
import no.nav.familie.klage.personopplysninger.pdl.MotpartsRolle
import org.springframework.stereotype.Service

@Service
class FullmaktService(
    val fullmaktClient: FullmaktClient,
) {

    fun hentFullmakt(ident: String): List<Fullmakt> {
        val fullmaktResponse = fullmaktClient.hentFullmakt(ident)
        return fullmaktResponse.map {
            Fullmakt(
                gyldigFraOgMed = it.gyldigFraOgMed,
                gyldigTilOgMed = it.gyldigTilOgMed,
                motpartsPersonident = it.fullmektig,
                fullmektigsNavn = it.fullmektigsNavn,
                motpartsRolle = MotpartsRolle.FULLMEKTIG,
                it.omraade.map { it.tema },
            )
        }
    }
}
