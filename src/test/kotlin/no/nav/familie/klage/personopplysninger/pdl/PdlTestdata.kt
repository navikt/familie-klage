package no.nav.familie.klage.personopplysninger.pdl

import java.time.LocalDate
import java.time.LocalDateTime

object PdlTestdata {

    private val metadataGjeldende = Metadata(false)

    val vegadresse = Vegadresse(
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        Koordinater(
            1.0f,
            1.0f,
            1.0f,
            1
        ),
        1L
    )

    private val matrikkeladresse = Matrikkeladresse(1L, "", "", "")
    private val utenlandskAdresse = UtenlandskAdresse("", "", "", "", "", "", "")

    private val folkeregistermetadata = Folkeregistermetadata(LocalDateTime.now(), LocalDateTime.now())

    private val navn = listOf(Navn("", "", "", metadataGjeldende))

    private val adressebeskyttelse =
        listOf(Adressebeskyttelse(AdressebeskyttelseGradering.FORTROLIG, metadataGjeldende))

    private val bostedsadresse = listOf(
        Bostedsadresse(
            LocalDate.now().minusDays(10),
            LocalDate.now(),
            LocalDate.now(),
            "",
            utenlandskAdresse,
            vegadresse,
            UkjentBosted(""),
            matrikkeladresse,
            metadataGjeldende
        )
    )

    private val dødsfall = listOf(Dødsfall(LocalDate.now()))

    private val familierelasjon =
        listOf(ForelderBarnRelasjon("", Familierelasjonsrolle.BARN, Familierelasjonsrolle.FAR))

    private val fødsel = listOf(Fødsel(1, LocalDate.now(), "", "", "", metadataGjeldende))

    private val opphold = listOf(Opphold(Oppholdstillatelse.MIDLERTIDIG, LocalDate.now(), LocalDate.now()))

    private val oppholdsadresse = listOf(
        Oppholdsadresse(
            LocalDate.now(),
            null,
            "",
            utenlandskAdresse,
            vegadresse,
            "",
            metadataGjeldende
        )
    )

    private val statsborgerskap = listOf(Statsborgerskap("", LocalDate.now(), LocalDate.now()))

    private val innflyttingTilNorge = listOf(InnflyttingTilNorge("", "", folkeregistermetadata))

    private val utflyttingFraNorge = listOf(UtflyttingFraNorge("", "", LocalDate.now(), folkeregistermetadata))

    val søkerIdentifikator = "1"

    val pdlSøkerData =
        PdlSøkerData(
            PdlSøker(
                adressebeskyttelse,
                bostedsadresse,
                dødsfall,
                familierelasjon,
                fødsel,
                listOf(Folkeregisterpersonstatus("", "", metadataGjeldende)),
                listOf(
                    Fullmakt(
                        LocalDate.now(),
                        LocalDate.now(),
                        "",
                        MotpartsRolle.FULLMAKTSGIVER,
                        listOf("")
                    )
                ),
                listOf(Kjønn(KjønnType.KVINNE)),
                listOf(
                    Kontaktadresse(
                        "",
                        LocalDate.now(),
                        LocalDate.now(),
                        PostadresseIFrittFormat("", "", "", ""),
                        Postboksadresse("", "", ""),
                        KontaktadresseType.INNLAND,
                        utenlandskAdresse,
                        UtenlandskAdresseIFrittFormat("", "", "", "", "", ""),
                        vegadresse
                    )
                ),
                navn,
                opphold,
                oppholdsadresse,
                listOf(
                    Sivilstand(
                        Sivilstandstype.GIFT,
                        LocalDate.now(),
                        "",
                        LocalDate.now(),
                        metadataGjeldende
                    )
                ),
                statsborgerskap,
                listOf(Telefonnummer("", "", 1)),
                listOf(TilrettelagtKommunikasjon(Tolk(""), Tolk(""))),
                innflyttingTilNorge,
                utflyttingFraNorge,
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
