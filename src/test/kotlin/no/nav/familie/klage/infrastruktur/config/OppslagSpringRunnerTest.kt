package no.nav.familie.klage.infrastruktur.config

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.familie.klage.ApplicationLocal
import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.familie.klage.brev.domain.Avsnitt
import no.nav.familie.klage.brev.domain.Brev
import no.nav.familie.klage.brevmottaker.domain.Brevmottaker
import no.nav.familie.klage.fagsak.domain.FagsakDomain
import no.nav.familie.klage.fagsak.domain.FagsakPerson
import no.nav.familie.klage.fagsak.domain.PersonIdent
import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.infrastruktur.db.DbContainerInitializer
import no.nav.familie.klage.kabal.domain.KlageinstansResultat
import no.nav.familie.klage.oppgave.BehandleSakOppgave
import no.nav.familie.klage.testutil.TestoppsettService
import no.nav.familie.klage.testutil.TokenUtil
import no.nav.familie.klage.vurdering.domain.Vurdering
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskLogg
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.cache.CacheManager
import org.springframework.context.ApplicationContext
import org.springframework.data.jdbc.core.JdbcAggregateOperations
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@SpringBootTest(classes = [ApplicationLocal::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(
    "integrasjonstest",
    "mock-oauth",
    "mock-pdl",
    "mock-integrasjoner",
    "mock-brev",
    "mock-kabal",
    "mock-dokument",
    "mock-ef-sak",
    "mock-ks-sak",
    "mock-ba-sak",
    "mock-ereg",
    "mock-inntekt",
    "mock-fullmakt",
    "mock-featuretoggle",
)
@EnableMockOAuth2Server
abstract class OppslagSpringRunnerTest {
    protected val listAppender = initLoggingEventListAppender()
    protected var loggingEvents: MutableList<ILoggingEvent> = listAppender.list
    protected val restTemplate = TestRestTemplate()
    protected val headers = HttpHeaders()

    @Autowired
    private lateinit var jdbcAggregateOperations: JdbcAggregateOperations

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var cacheManager: CacheManager

    @Autowired
    private lateinit var rolleConfig: RolleConfig

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @Autowired
    lateinit var testoppsettService: TestoppsettService

    @LocalServerPort
    private var port: Int? = 0

    @AfterEach
    fun reset() {
        headers.clear()
        loggingEvents.clear()
        resetDatabase()
        clearCaches()
        resetWiremockServers()
    }

    private fun resetWiremockServers() {
        applicationContext.getBeansOfType(WireMockServer::class.java).values.forEach(WireMockServer::resetRequests)
    }

    private fun clearCaches() {
        listOf(cacheManager).forEach {
            it.cacheNames
                .mapNotNull { cacheName -> it.getCache(cacheName) }
                .forEach { cache -> cache.clear() }
        }
    }

    private fun resetDatabase() {
        listOf(
            Brevmottaker::class,
            BehandleSakOppgave::class,
            Behandlingshistorikk::class,
            Avsnitt::class,
            Brev::class,
            Vurdering::class,
            Form::class,
            KlageinstansResultat::class,
            Behandling::class,
            FagsakDomain::class,
            FagsakPerson::class,
            PersonIdent::class,
            TaskLogg::class,
            Task::class,
        ).forEach { jdbcAggregateOperations.deleteAll(it.java) }
    }

    protected fun getPort(): String = port.toString()

    protected fun localhost(uri: String): String = LOCALHOST + getPort() + uri

    protected fun url(
        baseUrl: String,
        uri: String,
    ): String = baseUrl + uri

    protected val lokalTestToken: String
        get() {
            return onBehalfOfToken(role = rolleConfig.ef.beslutter)
        }

    protected fun onBehalfOfToken(
        role: String = rolleConfig.ef.beslutter,
        saksbehandler: String = "julenissen",
    ): String = TokenUtil.onBehalfOfToken(mockOAuth2Server, role, saksbehandler)

    protected fun clientToken(
        clientId: String = "1",
        accessAsApplication: Boolean = true,
    ): String = TokenUtil.clientToken(mockOAuth2Server, clientId, accessAsApplication)

    companion object {
        private const val LOCALHOST = "http://localhost:"

        protected fun initLoggingEventListAppender(): ListAppender<ILoggingEvent> {
            val listAppender = ListAppender<ILoggingEvent>()
            listAppender.start()
            return listAppender
        }
    }
}
