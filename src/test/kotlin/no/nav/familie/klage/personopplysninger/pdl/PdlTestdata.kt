package no.nav.familie.klage.personopplysninger.pdl

import java.time.LocalDate
import java.time.LocalDateTime

object PdlTestdata {

    private val metadataGjeldende = Metadata(false)

    val ident = "2"

    private val folkeregistermetadata = Folkeregistermetadata(LocalDateTime.now(), LocalDateTime.now())

    private val navn = listOf(Navn("", "", "", metadataGjeldende))

    private val adressebeskyttelse =
        listOf(Adressebeskyttelse(AdressebeskyttelseGradering.FORTROLIG, metadataGjeldende))

    private val dødsfall = listOf(Dødsfall(LocalDate.now()))

    val pdlNavnBolk = PersonBolk(
        personBolk = listOf(
            PersonDataBolk(
                ident = ident,
                code = "ok",
                person = PdlNavn(
                    navn = navn,
                ),
            ),
        ),
    )

    val pdlSøkerData =
        PdlSøkerData(
            PdlSøker(
                adressebeskyttelse,
                dødsfall,
                listOf(Kjønn(KjønnType.KVINNE)),
                listOf(Folkeregisterpersonstatus("", "", metadataGjeldende)),
                navn,
                listOf(
                    VergemaalEllerFremtidsfullmakt(
                        "",
                        folkeregistermetadata,
                        "",
                        VergeEllerFullmektig(
                            "",
                            Personnavn("", "", ""),
                            "",
                            true,
                        ),
                    ),
                ),
            ),
        )
}
