package no.nav.familie.klage.ekstern

import no.nav.familie.klage.behandling.BehandlingRepository
import no.nav.familie.klage.fagsak.domain.PersonIdent
import no.nav.familie.klage.felles.domain.SporbarUtils
import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.kabal.KlageresultatRepository
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.klageresultat
import no.nav.familie.klage.testutil.DomainUtil.vurdering
import no.nav.familie.klage.testutil.DtoTestUtil.lagOpprettKlagebehandlingRequest
import no.nav.familie.klage.vurdering.VurderingRepository
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Ressurs.Status
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.HenlagtÅrsak
import no.nav.familie.kontrakter.felles.klage.KlagebehandlingDto
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import no.nav.familie.kontrakter.felles.klage.Årsak
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
import java.util.UUID

internal class EksternBehandlingControllerTest : OppslagSpringRunnerTest() {
    private val baseUrl = "/api/ekstern/behandling"

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var vurderingRepository: VurderingRepository

    @Autowired
    private lateinit var klageresultatRepository: KlageresultatRepository

    private val fagsak =
        DomainUtil
            .fagsakDomain(eksternId = "1", stønadstype = Stønadstype.OVERGANGSSTØNAD)
            .tilFagsakMedPersonOgInstitusjon(setOf(PersonIdent("1")))

    @BeforeEach
    internal fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
        headers.setBearerAuth(onBehalfOfToken())
    }

    @Nested
    inner class FinnKlagebehandlingsresultat {
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
            val vedtakDato = SporbarUtils.now()
            val henlagtÅrsak = HenlagtÅrsak.TRUKKET_TILBAKE
            val behandling =
                behandlingRepository.insert(
                    behandling(fagsak, vedtakDato = vedtakDato, henlagtÅrsak = henlagtÅrsak),
                )
            vurderingRepository.insert(vurdering(behandling.id, årsak = Årsak.FEIL_PROSESSUELL))
            val klageresultat = klageresultatRepository.insert(klageresultat(behandlingId = behandling.id))

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
            assertThat(klagebehandling.årsak).isEqualTo(Årsak.FEIL_PROSESSUELL)
            assertThat(klagebehandling.vedtaksdato).isEqualTo(vedtakDato)
            assertThat(klagebehandling.henlagtÅrsak).isEqualTo(henlagtÅrsak)

            val klageinstansResultat = klagebehandling.klageinstansResultat
            assertThat(klageinstansResultat).hasSize(1)
            assertThat(klageinstansResultat[0].type).isEqualTo(klageresultat.type)
            assertThat(klageinstansResultat[0].utfall).isEqualTo(klageresultat.utfall)
            assertThat(klageinstansResultat[0].mottattEllerAvsluttetTidspunkt)
                .isEqualTo(klageresultat.mottattEllerAvsluttetTidspunkt)
            assertThat(klageinstansResultat[0].journalpostReferanser)
                .containsExactlyInAnyOrderElementsOf(klageresultat.journalpostReferanser.verdier)
        }

        @Test
        internal fun `skal returnere behandlinger til flere eksternFagsakId for samme person`() {
            val fagsak3EksternId = "999"
            val fagsak2 =
                DomainUtil
                    .fagsakDomain(
                        personId = fagsak.fagsakPersonId,
                        eksternId = "2",
                        stønadstype = Stønadstype.BARNETILSYN,
                    ).tilFagsakMedPersonOgInstitusjon(fagsak.personIdenter)

            testoppsettService.lagreFagsak(fagsak2)
            behandlingRepository.insert(behandling(fagsak))
            behandlingRepository.insert(behandling(fagsak2))

            val url = "$hentBehandlingUrl?eksternFagsakId=${fagsak.eksternId},${fagsak2.eksternId},$fagsak3EksternId"
            val response = hentBehandlinger(url)

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val body = response.body!!
            assertThat(body.status).isEqualTo(Status.SUKSESS)
            val data = body.data
            assertThat(data!!).hasSize(3)
            assertThat(data.entries.map { it.key }).containsExactlyInAnyOrder(
                fagsak.eksternId,
                fagsak2.eksternId,
                fagsak3EksternId,
            )
            assertThat(data.getValue(fagsak.eksternId)).hasSize(1)
            assertThat(data.getValue(fagsak2.eksternId)).hasSize(1)
            assertThat(data.getValue(fagsak3EksternId)).isEmpty()
        }

        @Disabled
        @Test
        internal fun `skal feile når man spør etter fagsakIder till ulike personer`() {
            val fagsakAnnenPerson =
                DomainUtil
                    .fagsakDomain(
                        personId = fagsak.fagsakPersonId,
                        eksternId = "2",
                        stønadstype = Stønadstype.BARNETILSYN,
                    ).tilFagsakMedPersonOgInstitusjon(setOf(PersonIdent("2")))

            testoppsettService.lagreFagsak(fagsakAnnenPerson)

            val url = "$hentBehandlingUrl?eksternFagsakId=${fagsak.eksternId},${fagsakAnnenPerson.eksternId}"
            val response = hentBehandlinger(url)
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.body!!.status).isEqualTo(Status.FEILET)
        }

        private fun hentBehandlinger(url: String) =
            restTemplate.exchange<Ressurs<Map<String, List<KlagebehandlingDto>>>>(
                url,
                HttpMethod.GET,
                HttpEntity(null, headers),
            )
    }

    @Nested
    inner class OpprettBehandling {
        @Test
        fun `skal opprette behandling og returnere UUID`() {
            // Arrange
            val opprettKlagebehandlingRequest = lagOpprettKlagebehandlingRequest()

            // Act
            val response =
                restTemplate.exchange<Ressurs<UUID>>(
                    localhost("$baseUrl/v2/opprett"),
                    HttpMethod.POST,
                    HttpEntity<OpprettKlagebehandlingRequest>(
                        opprettKlagebehandlingRequest,
                        headers,
                    ),
                )

            // Arrange
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body?.status).isEqualTo(Status.SUKSESS)
            assertThat(response.body?.data).isNotNull()
        }
    }
}
