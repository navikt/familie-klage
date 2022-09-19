package no.nav.familie.klage.ekstern

import no.nav.familie.klage.behandling.BehandlingRepository
import no.nav.familie.klage.fagsak.domain.PersonIdent
import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Ressurs.Status
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.KlagebehandlingDto
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

internal class EksternBehandlingControllerTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    private val fagsak = DomainUtil.fagsakDomain(eksternId = "1", stønadstype = Stønadstype.OVERGANGSSTØNAD)
        .tilFagsakMedPerson(setOf(PersonIdent("1")))

    @BeforeEach
    internal fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
        headers.setBearerAuth(onBehalfOfToken())
    }

    @Nested
    inner class finnKlagebehandlingsresultat {

        private val hentBehandlingUrl: String = localhost("/api/ekstern/behandling/${Fagsystem.EF}")

        @Test
        internal fun `skal returnere tomt svar når det ikke finnes noen behandlinger på fagsaken`() {
            val externFagsakId = "200"
            val url = "$hentBehandlingUrl?eksternFagsakId=$externFagsakId"
            val response = hentBehandlinger(url)

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val body = response.body!!
            assertThat(body.status).isEqualTo(Status.SUKSESS)
            assertThat(body.data).isEqualTo(mapOf(externFagsakId to emptyList<KlagebehandlingDto>()))
        }

        @Test
        internal fun `skal returnere behandling når man spør etter eksternFagsakId`() {
            val behandling = behandlingRepository.insert(behandling(fagsak, eksternFagsystemBehandlingId = "123"))

            val url = "$hentBehandlingUrl?eksternFagsakId=${fagsak.eksternId}"
            val response = hentBehandlinger(url)

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val body = response.body!!
            assertThat(body.status).isEqualTo(Status.SUKSESS)
            val data = body.data
            assertThat(data!!).hasSize(1)
            val entry = data.entries.single()
            assertThat(entry.key).isEqualTo(fagsak.eksternId)
            assertThat(entry.value).hasSize(1)

            val klagebehandling = entry.value.single()
            assertThat(klagebehandling.id).isEqualTo(behandling.id)
            assertThat(klagebehandling.fagsakId).isEqualTo(behandling.fagsakId)
            assertThat(klagebehandling.status).isEqualTo(behandling.status)
            assertThat(klagebehandling.mottattDato).isEqualTo(behandling.klageMottatt)
            assertThat(klagebehandling.opprettet).isEqualTo(behandling.sporbar.opprettetTid)
            assertThat(klagebehandling.resultat).isEqualTo(BehandlingResultat.IKKE_SATT)
            assertThat(klagebehandling.årsak).isNull()
            assertThat(klagebehandling.vedtaksdato).isNull()
        }

        @Test
        internal fun `skal returnere behandlinger til flere eksternFagsakId for samme person`() {
            val fagsak3EksternId = "999"
            val fagsak2 = DomainUtil.fagsakDomain(
                personId = fagsak.fagsakPersonId,
                eksternId = "2",
                stønadstype = Stønadstype.BARNETILSYN
            ).tilFagsakMedPerson(fagsak.personIdenter)

            testoppsettService.lagreFagsak(fagsak2)
            behandlingRepository.insert(behandling(fagsak, eksternFagsystemBehandlingId = "11"))
            behandlingRepository.insert(behandling(fagsak2, eksternFagsystemBehandlingId = "22"))

            val url = "$hentBehandlingUrl?eksternFagsakId=${fagsak.eksternId},${fagsak2.eksternId},$fagsak3EksternId"
            val response = hentBehandlinger(url)

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val body = response.body!!
            assertThat(body.status).isEqualTo(Status.SUKSESS)
            val data = body.data
            assertThat(data!!).hasSize(3)
            assertThat(data.entries.map { it.key }).containsExactlyInAnyOrder(fagsak.eksternId, fagsak2.eksternId, fagsak3EksternId)
            assertThat(data.getValue(fagsak.eksternId)).hasSize(1)
            assertThat(data.getValue(fagsak2.eksternId)).hasSize(1)
            assertThat(data.getValue(fagsak3EksternId)).isEmpty()
        }

        @Disabled
        @Test
        internal fun `skal feile når man spør etter fagsakIder till ulike personer`() {
            val fagsakAnnenPerson = DomainUtil.fagsakDomain(
                personId = fagsak.fagsakPersonId,
                eksternId = "2",
                stønadstype = Stønadstype.BARNETILSYN
            ).tilFagsakMedPerson(setOf(PersonIdent("2")))

            testoppsettService.lagreFagsak(fagsakAnnenPerson)

            val url = "$hentBehandlingUrl?eksternFagsakId=${fagsak.eksternId},${fagsakAnnenPerson.eksternId}"
            val response = hentBehandlinger(url)
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.body!!.status).isEqualTo(Status.FEILET)
        }

        private fun hentBehandlinger(url: String) =
            restTemplate.exchange<Ressurs<Map<String, List<KlagebehandlingDto>>>>(url, HttpMethod.GET, HttpEntity(null, headers))
    }
}
