package no.nav.familie.klage.søk

import no.nav.familie.klage.behandling.BehandlingRepository
import no.nav.familie.klage.fagsak.domain.PersonIdent
import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.søk.dto.PersonIdentDto
import no.nav.familie.klage.søk.dto.PersonTreffDto
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Ressurs.Status
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.util.UUID

class SøkControllerTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    private val fagsak = DomainUtil.fagsakDomain(eksternId = "1", stønadstype = Stønadstype.OVERGANGSSTØNAD)
        .tilFagsakMedPerson(setOf(PersonIdent("1")))
    val behandling = behandling(fagsak, vedtakDato = null, henlagtÅrsak = null)

    @BeforeEach
    internal fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
        behandlingRepository.insert(behandling)
        headers.setBearerAuth(onBehalfOfToken())
    }

    @Test
    internal fun `skal få feil hvis man søker etter personident med ugyldig behandlingId`() {
        val response = søkPerson(PersonIdentDto("12345678901", UUID.randomUUID()))

        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        val body = response.body!!
        assertThat(body.status).isEqualTo(Status.FEILET)
        assertThat(body.data).isNull()
    }

    @Test
    internal fun `skal finne person via søk med personident og behandlingid`() {
        val response = søkPerson(PersonIdentDto("12345678901", behandling.id))

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val body = response.body!!
        assertThat(body.status).isEqualTo(Status.SUKSESS)
        assertThat(body.data).isEqualTo(PersonTreffDto("12345678901", "Fornavn mellomnavn Etternavn"))
    }

    private fun søkPerson(personIdentDto: PersonIdentDto) =
        restTemplate.exchange<Ressurs<PersonTreffDto>>(
            localhost("/api/sok/person"),
            HttpMethod.POST,
            HttpEntity(personIdentDto, headers),
        )
}
