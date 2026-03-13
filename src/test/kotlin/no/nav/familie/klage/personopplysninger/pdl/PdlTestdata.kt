package no.nav.familie.klage.personopplysninger.pdl

import java.time.LocalDate
import java.time.LocalDateTime

object PdlTestdata {
    private val metadataGjeldende = Metadata(false)

    private const val IDENT = "2"

    private val folkeregistermetadata = Folkeregistermetadata(LocalDateTime.now(), LocalDateTime.now())

    private val navn = listOf(Navn("", "", "", metadataGjeldende))

    private val adressebeskyttelse =
        listOf(Adressebeskyttelse(AdressebeskyttelseGradering.FORTROLIG, metadataGjeldende))

    private val dødsfall = listOf(Dødsfall(LocalDate.now()))

    private val fødselsdato = LocalDate.now().minusYears(40).let { Fødselsdato(it, it.year) }

    val pdlNavnBolk =
        PersonBolk(
            personBolk =
                listOf(
                    PersonDataBolk(
                        ident = IDENT,
                        code = "ok",
                        person =
                            PdlNavn(
                                navn = navn,
                            ),
                    ),
                ),
        )

    val pdlPersonData =
        PdlPersonData(
            PdlPerson(
                adressebeskyttelse,
                fødselsdato,
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
