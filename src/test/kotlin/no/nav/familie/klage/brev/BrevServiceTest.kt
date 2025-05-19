package no.nav.familie.klage.brev

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.brev.domain.Brev
import no.nav.familie.klage.brevmottaker.BrevmottakerUtleder
import no.nav.familie.klage.distribusjon.JournalførBrevTask
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.formkrav.FormService
import no.nav.familie.klage.henlegg.HenlagtDto
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import no.nav.familie.klage.personopplysninger.dto.FullmaktDto
import no.nav.familie.klage.personopplysninger.dto.Kjønn
import no.nav.familie.klage.personopplysninger.dto.PersonopplysningerDto
import no.nav.familie.klage.personopplysninger.dto.VergemålDto
import no.nav.familie.klage.testutil.BrukerContextUtil
import no.nav.familie.klage.vurdering.VurderingService
import no.nav.familie.kontrakter.felles.klage.HenlagtÅrsak
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate
import java.util.UUID.randomUUID

class BrevServiceTest {
    private val brevClient = mockk<BrevClient>(relaxed = true)
    private val brevRepository = mockk<BrevRepository>()
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val familieDokumentClient = mockk<FamilieDokumentClient>(relaxed = true)
    private val brevsignaturService = mockk<BrevsignaturService>(relaxed = true)
    private val fagsakService = mockk<FagsakService>(relaxed = true)
    private val formService = mockk<FormService>(relaxed = true)
    private val vurderingService = mockk<VurderingService>(relaxed = true)
    private val personopplysningerService = mockk<PersonopplysningerService>(relaxed = true)
    private val brevInnholdUtleder = mockk<BrevInnholdUtleder>(relaxed = true)
    private val taskService = mockk<TaskService>(relaxed = true)
    private val brevmottakerUtleder = mockk<BrevmottakerUtleder>(relaxed = true)

    private val brevService =
        BrevService(
            brevClient = brevClient,
            brevRepository = brevRepository,
            behandlingService = behandlingService,
            familieDokumentClient = familieDokumentClient,
            brevsignaturService = brevsignaturService,
            fagsakService = fagsakService,
            formService = formService,
            vurderingService = vurderingService,
            personopplysningerService = personopplysningerService,
            brevInnholdUtleder = brevInnholdUtleder,
            taskService = taskService,
            brevmottakerUtleder = brevmottakerUtleder,
        )

    @AfterEach
    internal fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    internal fun `Skal kaste feil hvis feilregistrert og send brev er true`() {
        val exception =
            assertThrows<Feil> {
                brevService.opprettJournalførHenleggelsesbrevTask(
                    randomUUID(),
                    HenlagtDto(HenlagtÅrsak.FEILREGISTRERT, true),
                )
            }
        assertThat(exception.message).isEqualTo("Skal ikke sende brev hvis type er ulik trukket tilbake")
    }

    @Test
    internal fun `Skal kaste feil hvis bruker har fullmakt`() {
        mocckHentPersonopplysningerMedFullmakt()
        val exception =
            assertThrows<Feil> {
                brevService.opprettJournalførHenleggelsesbrevTask(
                    randomUUID(),
                    HenlagtDto(HenlagtÅrsak.TRUKKET_TILBAKE, true),
                )
            }
        assertThat(exception.message).isEqualTo("Skal ikke sende brev hvis person er tilknyttet vergemål eller fullmakt")
    }

    @Test
    internal fun `Skal ikke kaste feil hvis bruker har fullmakt som har utgått`() {
        BrukerContextUtil.mockBrukerContext()
        mocckHentPersonopplysningerMedFullmaktEnDagSiden()

        val taskSlot = slot<no.nav.familie.prosessering.domene.Task>()
        val behandlingId = randomUUID()

        every { brevRepository.findByIdOrNull(any()) } returns
            Brev(
                behandlingId = behandlingId,
                saksbehandlerHtml = "someHtml",
                mottakere = null,
            )
        every { brevRepository.insert(any()) } answers { firstArg() }
        every { brevRepository.update(any()) } answers { firstArg() }
        every { taskService.save(capture(taskSlot)) } answers { firstArg() }

        brevService.opprettJournalførHenleggelsesbrevTask(
            behandlingId,
            HenlagtDto(årsak = HenlagtÅrsak.TRUKKET_TILBAKE, skalSendeHenleggelsesbrev = true),
        )

        assertThat(taskSlot.isCaptured).isTrue()
        assertThat(taskSlot.captured.type).isEqualTo(JournalførBrevTask.TYPE)
        assertThat(taskSlot.captured.payload).isEqualTo(behandlingId.toString())
    }

    @Test
    internal fun `Skal kaste feil hvis bruker har Verge`() {
        mockkHentPersonopplysningerMedVergemål()
        val exception =
            assertThrows<Feil> {
                brevService.opprettJournalførHenleggelsesbrevTask(
                    randomUUID(),
                    HenlagtDto(årsak = HenlagtÅrsak.TRUKKET_TILBAKE, skalSendeHenleggelsesbrev = true),
                )
            }
        assertThat(exception.message).isEqualTo("Skal ikke sende brev hvis person er tilknyttet vergemål eller fullmakt")
    }

    private fun mocckHentPersonopplysningerMedFullmaktEnDagSiden() {
        every { personopplysningerService.hentPersonopplysninger(any()) } returns
            dto(
                fullmakt =
                    listOf(
                        FullmaktDto(
                            gyldigFraOgMed = LocalDate.now().minusDays(2),
                            gyldigTilOgMed = LocalDate.now().minusDays(1),
                            navn = "123",
                            motpartsPersonident = "123",
                            områder = emptyList(),
                        ),
                    ),
            )
    }

    private fun mocckHentPersonopplysningerMedFullmakt() {
        every { personopplysningerService.hentPersonopplysninger(any()) } returns
            dto(
                fullmakt =
                    listOf(
                        FullmaktDto(
                            gyldigFraOgMed = LocalDate.now(),
                            gyldigTilOgMed = null,
                            navn = "123",
                            motpartsPersonident = "123",
                            områder = emptyList(),
                        ),
                    ),
            )
    }

    private fun mockkHentPersonopplysningerMedVergemål() {
        every { personopplysningerService.hentPersonopplysninger(any()) } returns
            dto(
                vergemål =
                    listOf(
                        VergemålDto(
                            embete = null,
                            type = null,
                            motpartsPersonident = null,
                            navn = null,
                            omfang = null,
                        ),
                    ),
            )
    }

    private fun dto(
        fullmakt: List<FullmaktDto> = emptyList(),
        vergemål: List<VergemålDto> = emptyList(),
    ) = PersonopplysningerDto(
        personIdent = "",
        navn = "",
        kjønn = Kjønn.MANN,
        adressebeskyttelse = null,
        folkeregisterpersonstatus = null,
        dødsdato = null,
        fullmakt = fullmakt,
        egenAnsatt = false,
        vergemål = vergemål,
    )
}
