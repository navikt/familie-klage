package no.nav.familie.klage.infrastruktur.config

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.familie.klage.personopplysninger.pdl.Adressebeskyttelse
import no.nav.familie.klage.personopplysninger.pdl.AdressebeskyttelseGradering
import no.nav.familie.klage.personopplysninger.pdl.KjønnType
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
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-pdl")
class PdlClientMock {
    @Bean
    @Primary
    fun pdlClient(): PdlClient {
        val pdlClient: PdlClient = mockk()

        every { pdlClient.ping() } just runs

        every { pdlClient.hentNavnBolk(any(), any()) } answers { firstArg<List<String>>().associateWith { pdlNavn(listOf(lagNavn())) } }

        every { pdlClient.hentPerson(any(), any()) } returns opprettPdlSøker()

        every {
            pdlClient.hentPersonidenter(
                any(),
                any<Stønadstype>(),
                eq(true),
            )
        } answers {
            PdlIdenter(
                listOf(
                    PdlIdent(
                        firstArg(),
                        false,
                    ),
                    PdlIdent(
                        "98765432109",
                        true,
                    ),
                ),
            )
        }

        return pdlClient
    }

    companion object {
        private const val ANNEN_FORELDER_FNR = "17097926735"

        fun opprettPdlSøker() =
            pdlSøker(
                adressebeskyttelse =
                    listOf(
                        Adressebeskyttelse(
                            gradering = AdressebeskyttelseGradering.UGRADERT,
                            metadata = metadataGjeldende,
                        ),
                    ),
                dødsfall = listOf(),
                kjønn = lagKjønn(KjønnType.KVINNE),
                navn = listOf(lagNavn()),
                vergemaalEllerFremtidsfullmakt = vergemaalEllerFremtidsfullmakt(),
            )

        private fun vergemaalEllerFremtidsfullmakt(): List<VergemaalEllerFremtidsfullmakt> =
            listOf(
                VergemaalEllerFremtidsfullmakt(
                    embete = null,
                    folkeregistermetadata = null,
                    type = "voksen",
                    vergeEllerFullmektig =
                        VergeEllerFullmektig(
                            motpartsPersonident = ANNEN_FORELDER_FNR,
                            navn = null,
                            omfang = "personligeOgOekonomiskeInteresser",
                            omfangetErInnenPersonligOmraade = false,
                        ),
                ),
                VergemaalEllerFremtidsfullmakt(
                    embete = null,
                    folkeregistermetadata = null,
                    type = "stadfestetFremtidsfullmakt",
                    vergeEllerFullmektig =
                        VergeEllerFullmektig(
                            motpartsPersonident = ANNEN_FORELDER_FNR,
                            navn = null,
                            omfang = "personligeOgOekonomiskeInteresser",
                            omfangetErInnenPersonligOmraade = false,
                        ),
                ),
            )
    }
}
