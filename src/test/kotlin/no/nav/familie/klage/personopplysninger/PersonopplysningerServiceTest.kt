package no.nav.familie.klage.personopplysninger

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.personopplysninger.dto.Adressebeskyttelse
import no.nav.familie.klage.personopplysninger.dto.Folkeregisterpersonstatus
import no.nav.familie.klage.personopplysninger.dto.Kjønn
import no.nav.familie.klage.personopplysninger.pdl.Dødsfall
import no.nav.familie.klage.personopplysninger.pdl.Fullmakt
import no.nav.familie.klage.personopplysninger.pdl.KjønnType
import no.nav.familie.klage.personopplysninger.pdl.MotpartsRolle
import no.nav.familie.klage.personopplysninger.pdl.Navn
import no.nav.familie.klage.personopplysninger.pdl.PdlClient
import no.nav.familie.klage.personopplysninger.pdl.PdlNavn
import no.nav.familie.klage.personopplysninger.pdl.Personnavn
import no.nav.familie.klage.personopplysninger.pdl.VergeEllerFullmektig
import no.nav.familie.klage.personopplysninger.pdl.VergemaalEllerFremtidsfullmakt
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.defaultIdenter
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import no.nav.familie.klage.testutil.PdlTestdataHelper.lagNavn
import no.nav.familie.klage.testutil.PdlTestdataHelper.metadataGjeldende
import no.nav.familie.klage.testutil.PdlTestdataHelper.pdlSøker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import no.nav.familie.klage.personopplysninger.pdl.Adressebeskyttelse as PdlAdressebeskyttelse
import no.nav.familie.klage.personopplysninger.pdl.AdressebeskyttelseGradering as PdlAdressebeskyttelseGradering1
import no.nav.familie.klage.personopplysninger.pdl.Folkeregisterpersonstatus as PdlFolkeregisterpersonstatus1
import no.nav.familie.klage.personopplysninger.pdl.Kjønn as PdlKjønn

internal class PersonopplysningerServiceTest {

    private val behandlingService = mockk<BehandlingService>()
    private val fagsakService = mockk<FagsakService>()
    private val pdlClient = mockk<PdlClient>()
    private val integrasjonerClient = mockk<PersonopplysningerIntegrasjonerClient>()

    private val personopplysningerService =
        PersonopplysningerService(behandlingService, fagsakService, pdlClient, integrasjonerClient)

    private val fagsak = fagsak()
    private val behandling = behandling(fagsak)

    @BeforeEach
    internal fun setUp() {
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { fagsakService.hentFagsak(fagsak.id) } returns fagsak
        every { pdlClient.hentPerson(any()) } returns lagPdlSøker()
        every { pdlClient.hentNavnBolk(any()) } returns navnBolkResponse()
        every { integrasjonerClient.egenAnsatt(any()) } returns true
    }

    @Test
    internal fun `skal mappe til dto`() {
        val personopplysninger = personopplysningerService.hentPersonopplysninger(behandling.id)

        assertThat(personopplysninger.personIdent).isEqualTo(defaultIdenter.single().ident)
        assertThat(personopplysninger.navn).isEqualTo("Fornavn mellomnavn Etternavn")
        assertThat(personopplysninger.kjønn).isEqualTo(Kjønn.KVINNE)
        assertThat(personopplysninger.folkeregisterpersonstatus).isEqualTo(Folkeregisterpersonstatus.DØD)
        assertThat(personopplysninger.adressebeskyttelse).isEqualTo(Adressebeskyttelse.FORTROLIG)
        assertThat(personopplysninger.dødsdato).isEqualTo(LocalDate.now())
        assertThat(personopplysninger.fullmakt).hasSize(1)
        assertThat(personopplysninger.egenAnsatt).isTrue
        assertThat(personopplysninger.vergemål).hasSize(1)

        verify(exactly = 1) { pdlClient.hentNavnBolk(eq(listOf("fullmaktIdent"))) }
    }

    @Test
    internal fun `skal hente navn til fullmakt`() {
        val personopplysninger = personopplysningerService.hentPersonopplysninger(behandling.id)

        assertThat(personopplysninger.fullmakt.single().navn).isEqualTo("fullmakt etternavn")

        verify(exactly = 1) { pdlClient.hentNavnBolk(eq(listOf("fullmaktIdent"))) }
    }

    private fun navnBolkResponse() = mapOf(
        "fullmaktIdent" to PdlNavn(listOf(Navn("fullmakt", null, "etternavn", metadataGjeldende))),
    )

    private fun lagPdlSøker() = pdlSøker(
        listOf(PdlAdressebeskyttelse(PdlAdressebeskyttelseGradering1.FORTROLIG, metadataGjeldende)),
        listOf(Dødsfall(LocalDate.now())),
        listOf(PdlFolkeregisterpersonstatus1("doed", "d", metadataGjeldende)),
        listOf(Fullmakt(LocalDate.now(), LocalDate.now(), "fullmaktIdent", MotpartsRolle.FULLMEKTIG, listOf("o"))),
        PdlKjønn(KjønnType.KVINNE),
        listOf(lagNavn()),
        listOf(
            VergemaalEllerFremtidsfullmakt(
                "embete",
                null,
                "type",
                VergeEllerFullmektig("vergeIdent", Personnavn("", "", null), "omfang", true),
            ),
        ),
    )
}
