package no.nav.familie.ef.klage.infrastruktur.config

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.familie.ef.klage.testutil.PdlTestdataHelper.fødsel
import no.nav.familie.ef.klage.testutil.PdlTestdataHelper.lagKjønn
import no.nav.familie.ef.klage.testutil.PdlTestdataHelper.lagNavn
import no.nav.familie.ef.klage.testutil.PdlTestdataHelper.metadataGjeldende
import no.nav.familie.ef.klage.testutil.PdlTestdataHelper.pdlBarn
import no.nav.familie.ef.klage.testutil.PdlTestdataHelper.pdlSøker
import no.nav.familie.klage.personopplysninger.pdl.Adressebeskyttelse
import no.nav.familie.klage.personopplysninger.pdl.AdressebeskyttelseGradering
import no.nav.familie.klage.personopplysninger.pdl.Bostedsadresse
import no.nav.familie.klage.personopplysninger.pdl.Dødsfall
import no.nav.familie.klage.personopplysninger.pdl.Familierelasjonsrolle
import no.nav.familie.klage.personopplysninger.pdl.Folkeregistermetadata
import no.nav.familie.klage.personopplysninger.pdl.Folkeregisterpersonstatus
import no.nav.familie.klage.personopplysninger.pdl.ForelderBarnRelasjon
import no.nav.familie.klage.personopplysninger.pdl.Fullmakt
import no.nav.familie.klage.personopplysninger.pdl.InnflyttingTilNorge
import no.nav.familie.klage.personopplysninger.pdl.KjønnType
import no.nav.familie.klage.personopplysninger.pdl.Kontaktadresse
import no.nav.familie.klage.personopplysninger.pdl.KontaktadresseType
import no.nav.familie.klage.personopplysninger.pdl.MotpartsRolle
import no.nav.familie.klage.personopplysninger.pdl.Navn
import no.nav.familie.klage.personopplysninger.pdl.Opphold
import no.nav.familie.klage.personopplysninger.pdl.Oppholdstillatelse
import no.nav.familie.klage.personopplysninger.pdl.PdlAnnenForelder
import no.nav.familie.klage.personopplysninger.pdl.PdlBarn
import no.nav.familie.klage.personopplysninger.pdl.PdlClient
import no.nav.familie.klage.personopplysninger.pdl.PdlIdent
import no.nav.familie.klage.personopplysninger.pdl.PdlIdenter
import no.nav.familie.klage.personopplysninger.pdl.PdlPersonKort
import no.nav.familie.klage.personopplysninger.pdl.Sivilstand
import no.nav.familie.klage.personopplysninger.pdl.Sivilstandstype
import no.nav.familie.klage.personopplysninger.pdl.Statsborgerskap
import no.nav.familie.klage.personopplysninger.pdl.Telefonnummer
import no.nav.familie.klage.personopplysninger.pdl.UtflyttingFraNorge
import no.nav.familie.klage.personopplysninger.pdl.Vegadresse
import no.nav.familie.klage.personopplysninger.pdl.VergeEllerFullmektig
import no.nav.familie.klage.personopplysninger.pdl.VergemaalEllerFremtidsfullmakt
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month

@Configuration
@Profile("mock-pdl")
class PdlClientMock {

    @Bean
    @Primary
    fun pdlClient(): PdlClient {
        val pdlClient: PdlClient = mockk()

        every { pdlClient.ping() } just runs

        every { pdlClient.hentPersonKortBolk(any()) } answers { firstArg<List<String>>().associate { it to lagPersonKort(it) } }

        every { pdlClient.hentSøker(any()) } returns opprettPdlSøker()

        every { pdlClient.hentBarn(any()) } returns barn()

        every { pdlClient.hentAndreForeldre(any()) } returns mapOf(annenForelderFnr to annenForelder())

        every { pdlClient.hentAktørIder(any()) } returns PdlIdenter(listOf(PdlIdent("12345678901232", false)))

        every { pdlClient.hentPersonidenter(any(), eq(true)) } answers
            { PdlIdenter(listOf(PdlIdent(firstArg(), false), PdlIdent("98765432109", true))) }

        every { pdlClient.hentIdenterBolk(listOf("123", "456")) }
            .returns(
                mapOf(
                    "123" to PdlIdent("ny123", false),
                    "456" to PdlIdent("ny456", false)
                )
            )

        every { pdlClient.hentIdenterBolk(listOf("456", "123")) }
            .returns(
                mapOf(
                    "123" to PdlIdent("ny123", false),
                    "456" to PdlIdent("ny456", false)
                )
            )

        every { pdlClient.hentIdenterBolk(listOf("111", "222")) }
            .returns(
                mapOf(
                    "111" to PdlIdent("111", false),
                    "222" to PdlIdent("222", false)
                )
            )

        return pdlClient
    }

    companion object {

        private val startdato = LocalDate.of(2020, 1, 1)
        private val sluttdato = LocalDate.of(2021, 1, 1)
        private const val barnFnr = "01012067050"
        private const val barn2Fnr = "14041385481"
        private const val søkerFnr = "01010172272"
        private const val annenForelderFnr = "17097926735"
        private const val fnrPåAdresseSøk = "01012067050"

        fun lagPersonKort(it: String) =
            PdlPersonKort(
                listOf(
                    Adressebeskyttelse(
                        gradering = AdressebeskyttelseGradering.UGRADERT,
                        metadata = metadataGjeldende
                    )
                ),
                listOf(lagNavn(fornavn = it)),
                emptyList()
            )

        fun opprettPdlSøker() =
            pdlSøker(
                adressebeskyttelse = listOf(
                    Adressebeskyttelse(
                        gradering = AdressebeskyttelseGradering.UGRADERT,
                        metadata = metadataGjeldende
                    )
                ),
                bostedsadresse = bostedsadresse(),
                dødsfall = listOf(),
                forelderBarnRelasjon = forelderBarnRelasjoner(),
                fødsel = listOf(fødsel()),
                folkeregisterpersonstatus = listOf(
                    Folkeregisterpersonstatus(
                        "bosatt",
                        "bosattEtterFolkeregisterloven",
                        metadataGjeldende
                    )
                ),
                fullmakt = fullmakter(),
                kjønn = lagKjønn(KjønnType.KVINNE),
                kontaktadresse = kontaktadresse(),
                navn = listOf(lagNavn()),
                opphold = listOf(Opphold(Oppholdstillatelse.PERMANENT, startdato, null)),
                oppholdsadresse = listOf(),
                sivilstand = sivilstand(),
                statsborgerskap = statsborgerskap(),
                telefonnummer = listOf(Telefonnummer(landskode = "+47", nummer = "98999923", prioritet = 1)),
                tilrettelagtKommunikasjon = listOf(),
                innflyttingTilNorge = listOf(InnflyttingTilNorge("SWE", "Stockholm", folkeregistermetadata)),
                utflyttingFraNorge = listOf(
                    UtflyttingFraNorge(
                        tilflyttingsland = "SWE",
                        tilflyttingsstedIUtlandet = "Stockholm",
                        utflyttingsdato = LocalDate.of(2021, 1, 1),
                        folkeregistermetadata = folkeregistermetadata
                    )
                ),
                vergemaalEllerFremtidsfullmakt = vergemaalEllerFremtidsfullmakt()
            )

        private val folkeregistermetadata = Folkeregistermetadata(
            LocalDateTime.of(2010, Month.AUGUST, 30, 10, 10),
            LocalDateTime.of(2018, Month.JANUARY, 15, 12, 55)
        )

        private fun barn(): Map<String, PdlBarn> =
            mapOf(
                barnFnr to pdlBarn(
                    bostedsadresse = bostedsadresse(),
                    forelderBarnRelasjon = familierelasjonerBarn(),
                    fødsel = fødsel(),
                    navn = lagNavn("Barn", null, "Barnesen")
                ),
                barn2Fnr to pdlBarn(
                    bostedsadresse = bostedsadresse(),
                    forelderBarnRelasjon = familierelasjonerBarn(),
                    fødsel = fødsel(),
                    navn = lagNavn("Barn2", null, "Barnesen")
                )
            )

        private fun annenForelder(): PdlAnnenForelder =
            PdlAnnenForelder(
                adressebeskyttelse = emptyList(),
                bostedsadresse = bostedsadresse(),
                dødsfall = listOf(Dødsfall(LocalDate.of(2021, 9, 22))),
                fødsel = listOf(fødsel(1994, 11, 1)),
                navn = listOf(Navn("Bob", "", "Burger", metadataGjeldende)),
            )

        private fun forelderBarnRelasjoner(): List<ForelderBarnRelasjon> =
            listOf(
                ForelderBarnRelasjon(
                    relatertPersonsIdent = barnFnr,
                    relatertPersonsRolle = Familierelasjonsrolle.BARN,
                    minRolleForPerson = Familierelasjonsrolle.MOR
                ),
                ForelderBarnRelasjon(
                    relatertPersonsIdent = barn2Fnr,
                    relatertPersonsRolle = Familierelasjonsrolle.BARN,
                    minRolleForPerson = Familierelasjonsrolle.MOR
                )
            )

        private fun familierelasjonerBarn(): List<ForelderBarnRelasjon> =
            listOf(
                ForelderBarnRelasjon(
                    relatertPersonsIdent = søkerFnr,
                    relatertPersonsRolle = Familierelasjonsrolle.MOR,
                    minRolleForPerson = Familierelasjonsrolle.BARN
                ),
                ForelderBarnRelasjon(
                    relatertPersonsIdent = annenForelderFnr,
                    relatertPersonsRolle = Familierelasjonsrolle.FAR,
                    minRolleForPerson = Familierelasjonsrolle.BARN
                )
            )

        private fun kontaktadresse(): List<Kontaktadresse> =
            listOf(
                Kontaktadresse(
                    coAdressenavn = "co",
                    gyldigFraOgMed = startdato,
                    gyldigTilOgMed = sluttdato,
                    postadresseIFrittFormat = null,
                    postboksadresse = null,
                    type = KontaktadresseType.INNLAND,
                    utenlandskAdresse = null,
                    utenlandskAdresseIFrittFormat = null,
                    vegadresse = vegadresse()
                )
            )

        private fun statsborgerskap(): List<Statsborgerskap> =
            listOf(
                Statsborgerskap(
                    land = "NOR",
                    gyldigFraOgMed = startdato,
                    gyldigTilOgMed = null
                ),
                Statsborgerskap(
                    land = "SWE",
                    gyldigFraOgMed = startdato.minusYears(3),
                    gyldigTilOgMed = startdato
                )
            )

        private fun sivilstand(): List<Sivilstand> =
            listOf(
                Sivilstand(
                    type = Sivilstandstype.GIFT,
                    gyldigFraOgMed = startdato,
                    relatertVedSivilstand = "11111122222",
                    bekreftelsesdato = LocalDate.of(2020, 1, 1),
                    metadata = metadataGjeldende
                )
            )

        private fun fullmakter(): List<Fullmakt> =
            listOf(
                Fullmakt(
                    gyldigTilOgMed = startdato,
                    gyldigFraOgMed = sluttdato,
                    motpartsPersonident = "11111133333",
                    motpartsRolle = MotpartsRolle.FULLMEKTIG,
                    omraader = listOf()
                )
            )

        private fun bostedsadresse(): List<Bostedsadresse> =
            listOf(
                Bostedsadresse(
                    angittFlyttedato = startdato.plusDays(1),
                    gyldigFraOgMed = startdato,
                    gyldigTilOgMed = LocalDate.of(2199, 1, 1),
                    utenlandskAdresse = null,
                    coAdressenavn = "CONAVN",
                    vegadresse = vegadresse(),
                    ukjentBosted = null,
                    matrikkeladresse = null,
                    metadata = metadataGjeldende
                )
            )

        private fun vegadresse(): Vegadresse =
            Vegadresse(
                husnummer = "13",
                husbokstav = "b",
                adressenavn = "Charlies vei",
                kommunenummer = "0301",
                postnummer = "0575",
                bruksenhetsnummer = "",
                tilleggsnavn = null,
                koordinater = null,
                matrikkelId = 0
            )

        private fun vergemaalEllerFremtidsfullmakt(): List<VergemaalEllerFremtidsfullmakt> {
            return listOf(
                VergemaalEllerFremtidsfullmakt(
                    embete = null,
                    folkeregistermetadata = null,
                    type = "voksen",
                    vergeEllerFullmektig =
                    VergeEllerFullmektig(
                        motpartsPersonident = annenForelderFnr,
                        navn = null,
                        omfang = "personligeOgOekonomiskeInteresser",
                        omfangetErInnenPersonligOmraade = false
                    )
                ),
                VergemaalEllerFremtidsfullmakt(
                    embete = null,
                    folkeregistermetadata = null,
                    type = "stadfestetFremtidsfullmakt",
                    vergeEllerFullmektig =
                    VergeEllerFullmektig(
                        motpartsPersonident = annenForelderFnr,
                        navn = null,
                        omfang = "personligeOgOekonomiskeInteresser",
                        omfangetErInnenPersonligOmraade = false
                    )
                )
            )
        }
    }
}
