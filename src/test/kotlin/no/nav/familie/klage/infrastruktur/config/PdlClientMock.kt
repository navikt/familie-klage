package no.nav.familie.klage.infrastruktur.config

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.familie.klage.personopplysninger.pdl.Adressebeskyttelse
import no.nav.familie.klage.personopplysninger.pdl.AdressebeskyttelseGradering
import no.nav.familie.klage.personopplysninger.pdl.Fullmakt
import no.nav.familie.klage.personopplysninger.pdl.KjønnType
import no.nav.familie.klage.personopplysninger.pdl.MotpartsRolle
import no.nav.familie.klage.personopplysninger.pdl.PdlClient
import no.nav.familie.klage.personopplysninger.pdl.PdlIdent
import no.nav.familie.klage.personopplysninger.pdl.PdlIdenter
import no.nav.familie.klage.personopplysninger.pdl.VergeEllerFullmektig
import no.nav.familie.klage.personopplysninger.pdl.VergemaalEllerFremtidsfullmakt
import no.nav.familie.klage.testutil.PdlTestdataHelper.lagKjønn
import no.nav.familie.klage.testutil.PdlTestdataHelper.lagNavn
import no.nav.familie.klage.testutil.PdlTestdataHelper.metadataGjeldende
import no.nav.familie.klage.testutil.PdlTestdataHelper.pdlNavn
import no.nav.familie.klage.testutil.PdlTestdataHelper.pdlSøker
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.LocalDate

@Configuration
@Profile("mock-pdl")
class PdlClientMock {

    @Bean
    @Primary
    fun pdlClient(): PdlClient {
        val pdlClient: PdlClient = mockk()

        every { pdlClient.ping() } just runs

        every { pdlClient.hentNavnBolk(any()) } answers { firstArg<List<String>>().associateWith { pdlNavn(listOf(lagNavn())) } }

        every { pdlClient.hentPerson(any()) } returns opprettPdlSøker()

        every { pdlClient.hentPersonidenter(any(), eq(true)) } answers
            { PdlIdenter(listOf(PdlIdent(firstArg(), false), PdlIdent("98765432109", true))) }

        return pdlClient
    }

    companion object {

        private val startdato = LocalDate.of(2020, 1, 1)
        private val sluttdato = LocalDate.of(2021, 1, 1)
        private const val annenForelderFnr = "17097926735"

        fun opprettPdlSøker() =
            pdlSøker(
                adressebeskyttelse = listOf(
                    Adressebeskyttelse(
                        gradering = AdressebeskyttelseGradering.UGRADERT,
                        metadata = metadataGjeldende
                    )
                ),
                dødsfall = listOf(),
                fullmakt = fullmakter(),
                kjønn = lagKjønn(KjønnType.KVINNE),
                navn = listOf(lagNavn()),
                vergemaalEllerFremtidsfullmakt = vergemaalEllerFremtidsfullmakt()
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
