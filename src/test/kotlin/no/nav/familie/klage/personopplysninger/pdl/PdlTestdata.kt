package no.nav.familie.klage.personopplysninger.pdl

import java.time.LocalDate
import java.time.LocalDateTime

object PdlTestdata {

    private val metadataGjeldende = Metadata(false)

    private val folkeregistermetadata = Folkeregistermetadata(LocalDateTime.now(), LocalDateTime.now())

    private val navn = listOf(Navn("", "", "", metadataGjeldende))

    private val adressebeskyttelse =
        listOf(Adressebeskyttelse(AdressebeskyttelseGradering.FORTROLIG, metadataGjeldende))

    private val dødsfall = listOf(Dødsfall(LocalDate.now()))

   val pdlSøkerData =
        PdlSøkerData(
            PdlSøker(
                adressebeskyttelse,
                dødsfall,
                listOf(Kjønn(KjønnType.KVINNE)),
                listOf(
                    Fullmakt(
                        LocalDate.now(),
                        LocalDate.now(),
                        "",
                        MotpartsRolle.FULLMAKTSGIVER,
                        listOf("")
                    )
                ),
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
                            true
                        )
                    )
                )
            )
        )

    val ennenForelderIdentifikator = "2"

}
