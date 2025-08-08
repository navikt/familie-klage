package no.nav.familie.klage.brev

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.brev.domain.Brev
import no.nav.familie.klage.brevmottaker.BrevmottakerUtleder
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonMedIdent
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonUtenIdent
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle
import no.nav.familie.klage.distribusjon.JournalførBrevTask
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.felles.domain.Fil
import no.nav.familie.klage.formkrav.FormService
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.klage.infrastruktur.featuretoggle.Toggle
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import no.nav.familie.klage.testutil.BrukerContextUtil
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.klage.testutil.DtoTestUtil
import no.nav.familie.klage.vurdering.VurderingService
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate

class BrevServiceTest {
    private val brevClient = mockk<BrevClient>()
    private val brevRepository = mockk<BrevRepository>()
    private val behandlingService = mockk<BehandlingService>()
    private val familieDokumentClient = mockk<FamilieDokumentClient>()
    private val brevsignaturService = mockk<BrevsignaturService>()
    private val fagsakService = mockk<FagsakService>()
    private val formService = mockk<FormService>()
    private val vurderingService = mockk<VurderingService>()
    private val personopplysningerService = mockk<PersonopplysningerService>()
    private val brevInnholdUtleder = mockk<BrevInnholdUtleder>()
    private val taskService = mockk<TaskService>()
    private val brevmottakerUtleder = mockk<BrevmottakerUtleder>()
    private val featureToggleService = mockk<FeatureToggleService>()

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
            featureToggleService = featureToggleService,
        )

    @BeforeEach
    fun setup() {
        BrukerContextUtil.mockBrukerContext()
        every { featureToggleService.isEnabled(any()) } returns true
    }

    @AfterEach
    fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Nested
    inner class LagHenleggelsesbrevOgOpprettJournalføringstask {
        @Test
        fun `skal kaste feil hvis verge for fagsystem EF`() {
            // Arrange
            val fagsak = DomainUtil.fagsak(stønadstype = Stønadstype.SKOLEPENGER)
            val behandling = DomainUtil.behandling(fagsak = fagsak)

            val personopplysningerDtoMedVergemål =
                DtoTestUtil.lagPersonopplysningerDto(
                    vergemål =
                        listOf(
                            DtoTestUtil.lagVergemålDto(
                                navn = "Vergenavn",
                            ),
                        ),
                )

            every { behandlingService.hentBehandling(behandling.id) } returns behandling
            every { fagsakService.hentFagsak(behandling.fagsakId) } returns fagsak
            every { personopplysningerService.hentPersonopplysninger(any()) } returns personopplysningerDtoMedVergemål

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    brevService.lagHenleggelsesbrevOgOpprettJournalføringstask(
                        behandlingId = behandling.id,
                        nyeBrevmottakere = emptyList(),
                    )
                }
            assertThat(exception.message).isEqualTo("Skal ikke sende brev hvis person er tilknyttet vergemål eller fullmakt")
        }

        @Test
        fun `skal kaste feil hvis fullmakt for fagsystem EF`() {
            // Arrange
            val fagsak = DomainUtil.fagsak(stønadstype = Stønadstype.SKOLEPENGER)
            val behandling = DomainUtil.behandling(fagsak = fagsak)

            val fullmaktDtoSomFortsattLøper =
                DtoTestUtil.lagFullmaktDto(
                    gyldigFraOgMed = LocalDate.now().minusDays(1),
                    gyldigTilOgMed = null,
                )
            val personopplysningerDto = DtoTestUtil.lagPersonopplysningerDto(fullmakt = listOf(fullmaktDtoSomFortsattLøper))

            every { behandlingService.hentBehandling(behandling.id) } returns behandling
            every { fagsakService.hentFagsak(behandling.fagsakId) } returns fagsak
            every { personopplysningerService.hentPersonopplysninger(any()) } returns personopplysningerDto

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    brevService.lagHenleggelsesbrevOgOpprettJournalføringstask(
                        behandlingId = behandling.id,
                        nyeBrevmottakere = emptyList(),
                    )
                }
            assertThat(exception.message).isEqualTo("Skal ikke sende brev hvis person er tilknyttet vergemål eller fullmakt")
        }

        @Test
        fun `skal opprette henleggelsesbrev og opprette journalføringstask når toggle er skrudd av for fagsystem EF`() {
            // Arrange
            val fagsak = DomainUtil.fagsak(stønadstype = Stønadstype.SKOLEPENGER)
            val behandling = DomainUtil.behandling(fagsak = fagsak)

            val personopplysningerDto =
                DtoTestUtil.lagPersonopplysningerDto(
                    fullmakt =
                        listOf(
                            DtoTestUtil.lagFullmaktDto(
                                navn = "Navn Navnesen",
                                gyldigFraOgMed = LocalDate.now().minusDays(2),
                                gyldigTilOgMed = LocalDate.now().minusDays(1),
                            ),
                        ),
                )

            val bruker = DomainUtil.lagNyBrevmottakerPersonMedIdent(mottakerRolle = MottakerRolle.BRUKER)
            val nyeBrevmottakere = listOf(bruker)

            val initielleBrevmottakere =
                DomainUtil.lagBrevmottakere(
                    personer =
                        listOf(
                            DomainUtil.lagBrevmottakerPersonMedIdent(
                                personIdent = bruker.personIdent,
                                mottakerRolle = bruker.mottakerRolle,
                                navn = bruker.navn,
                            ),
                        ),
                    organisasjoner = emptyList(),
                )

            val pdf = ByteArray(0)

            val taskSlot = slot<Task>()
            val brevSlot = slot<Brev>()

            every { behandlingService.hentBehandling(behandling.id) } returns behandling
            every { behandlingService.hentBehandlingDto(behandling.id) } returns DtoTestUtil.lagBehandlingDto(fagsak = fagsak, behandling = behandling)
            every { fagsakService.hentFagsak(behandling.fagsakId) } returns fagsak
            every { personopplysningerService.hentPersonopplysninger(any()) } returns personopplysningerDto
            every { brevsignaturService.lagSignatur(personopplysningerDto, fagsak.fagsystem) } returns DtoTestUtil.lagSignaturDto()
            every { brevClient.genererHtml(any(), any(), any(), any(), any(), any()) } returns "<html />"
            every { familieDokumentClient.genererPdfFraHtml(any()) } returns pdf
            every { brevmottakerUtleder.utledInitielleBrevmottakere(behandling.id) } returns initielleBrevmottakere
            every { brevRepository.findByIdOrNull(any()) } returns null
            every { brevRepository.insert(capture(brevSlot)) } answers { firstArg() }
            every { taskService.save(capture(taskSlot)) } answers { firstArg() }

            // Act
            brevService.lagHenleggelsesbrevOgOpprettJournalføringstask(
                behandlingId = behandling.id,
                nyeBrevmottakere = nyeBrevmottakere,
            )

            // Assert
            assertThat(brevSlot.isCaptured).isTrue()
            assertThat(brevSlot.captured.behandlingId).isEqualTo(behandling.id)
            assertThat(brevSlot.captured.saksbehandlerHtml).isEqualTo("<html />")
            assertThat(brevSlot.captured.pdf).isEqualTo(Fil(pdf))
            assertThat(brevSlot.captured.mottakere?.personer).hasSize(1)
            assertThat(
                brevSlot.captured.mottakere
                    ?.personer
                    ?.get(0),
            ).isInstanceOfSatisfying(BrevmottakerPersonMedIdent::class.java) {
                assertThat(it.personIdent).isEqualTo(bruker.personIdent)
                assertThat(it.mottakerRolle).isEqualTo(bruker.mottakerRolle)
                assertThat(it.navn).isEqualTo(bruker.navn)
            }
            assertThat(brevSlot.captured.mottakere?.organisasjoner).isEmpty()
            assertThat(brevSlot.captured.mottakereJournalposter).isNull()
            assertThat(brevSlot.captured.sporbar).isNotNull()
            assertThat(taskSlot.isCaptured).isTrue()
            assertThat(taskSlot.captured.type).isEqualTo(JournalførBrevTask.TYPE)
            assertThat(taskSlot.captured.payload).isEqualTo(behandling.id.toString())
        }

        @Test
        fun `skal oppdatere eksisterende brev til henleggelsesbrev og opprette journalføringstask når toggle er skrudd av for fagsystem EF`() {
            // Arrange
            val fagsak = DomainUtil.fagsak()
            val behandling = DomainUtil.behandling(fagsak = fagsak)

            val personopplysningerDto =
                DtoTestUtil.lagPersonopplysningerDto(
                    fullmakt =
                        listOf(
                            DtoTestUtil.lagFullmaktDto(
                                navn = "Navn Navnesen",
                                gyldigFraOgMed = LocalDate.now().minusDays(2),
                                gyldigTilOgMed = LocalDate.now().minusDays(1),
                            ),
                        ),
                )

            val bruker = DomainUtil.lagNyBrevmottakerPersonMedIdent(mottakerRolle = MottakerRolle.BRUKER)
            val nyeBrevmottakere = listOf(bruker)

            val initielleBrevmottakere =
                DomainUtil.lagBrevmottakere(
                    personer =
                        listOf(
                            DomainUtil.lagBrevmottakerPersonMedIdent(
                                personIdent = bruker.personIdent,
                                mottakerRolle = bruker.mottakerRolle,
                                navn = bruker.navn,
                            ),
                        ),
                    organisasjoner = emptyList(),
                )
            val brev = DomainUtil.lagBrev(behandlingId = behandling.id, mottakere = initielleBrevmottakere)

            val pdf = ByteArray(0)

            val taskSlot = slot<Task>()
            val brevSlot = slot<Brev>()

            every { behandlingService.hentBehandling(behandling.id) } returns behandling
            every { behandlingService.hentBehandlingDto(behandling.id) } returns DtoTestUtil.lagBehandlingDto(fagsak = fagsak, behandling = behandling)
            every { fagsakService.hentFagsak(behandling.fagsakId) } returns fagsak
            every { personopplysningerService.hentPersonopplysninger(any()) } returns personopplysningerDto
            every { brevsignaturService.lagSignatur(personopplysningerDto, fagsak.fagsystem) } returns DtoTestUtil.lagSignaturDto()
            every { brevClient.genererHtml(any(), any(), any(), any(), any(), any()) } returns "<html />"
            every { familieDokumentClient.genererPdfFraHtml(any()) } returns pdf
            every { brevmottakerUtleder.utledInitielleBrevmottakere(behandling.id) } returns initielleBrevmottakere
            every { brevRepository.findByIdOrNull(any()) } returns brev
            every { brevRepository.update(capture(brevSlot)) } answers { firstArg() }
            every { taskService.save(capture(taskSlot)) } answers { firstArg() }

            // Act
            brevService.lagHenleggelsesbrevOgOpprettJournalføringstask(
                behandlingId = behandling.id,
                nyeBrevmottakere = nyeBrevmottakere,
            )

            // Assert
            assertThat(brevSlot.isCaptured).isTrue()
            assertThat(brevSlot.captured.behandlingId).isEqualTo(behandling.id)
            assertThat(brevSlot.captured.saksbehandlerHtml).isEqualTo("<html />")
            assertThat(brevSlot.captured.pdf).isEqualTo(Fil(pdf))
            assertThat(brevSlot.captured.mottakere?.personer).hasSize(1)
            assertThat(
                brevSlot.captured.mottakere
                    ?.personer
                    ?.get(0),
            ).isInstanceOfSatisfying(BrevmottakerPersonMedIdent::class.java) {
                assertThat(it.personIdent).isEqualTo(bruker.personIdent)
                assertThat(it.mottakerRolle).isEqualTo(bruker.mottakerRolle)
                assertThat(it.navn).isEqualTo(bruker.navn)
            }
            assertThat(brevSlot.captured.mottakere?.organisasjoner).isEmpty()
            assertThat(brevSlot.captured.mottakereJournalposter).isNull()
            assertThat(brevSlot.captured.sporbar).isNotNull()
            assertThat(taskSlot.isCaptured).isTrue()
            assertThat(taskSlot.captured.type).isEqualTo(JournalførBrevTask.TYPE)
            assertThat(taskSlot.captured.payload).isEqualTo(behandling.id.toString())
        }

        @Test
        fun `skal opprette henleggelsesbrev og opprette journalføringstask for fagsystem BA`() {
            // Arrange
            val fagsak = DomainUtil.fagsak(stønadstype = Stønadstype.BARNETRYGD)
            val behandling = DomainUtil.behandling(fagsak = fagsak)

            val personopplysningerDto =
                DtoTestUtil.lagPersonopplysningerDto(
                    fullmakt =
                        listOf(
                            DtoTestUtil.lagFullmaktDto(
                                navn = "Navn Navnesen",
                                gyldigFraOgMed = LocalDate.now().minusDays(2),
                                gyldigTilOgMed = LocalDate.now().minusDays(1),
                            ),
                        ),
                )

            val bruker = DomainUtil.lagNyBrevmottakerPersonMedIdent(mottakerRolle = MottakerRolle.BRUKER)
            val verge = DomainUtil.lagNyBrevmottakerPersonUtenIdent(mottakerRolle = MottakerRolle.VERGE)
            val nyeBrevmottakere = listOf(bruker, verge)

            val initielleBrevmottakere =
                DomainUtil.lagBrevmottakere(
                    personer =
                        listOf(
                            DomainUtil.lagBrevmottakerPersonMedIdent(
                                personIdent = bruker.personIdent,
                                mottakerRolle = bruker.mottakerRolle,
                                navn = bruker.navn,
                            ),
                        ),
                    organisasjoner = emptyList(),
                )

            val lagFritekstBrevRequestDto =
                DtoTestUtil.lagFritekstBrevRequestDto(
                    personIdent = bruker.personIdent,
                    navn = bruker.navn,
                )

            val pdf = ByteArray(0)

            val taskSlot = slot<Task>()
            val brevSlot = slot<Brev>()

            every { behandlingService.hentBehandling(behandling.id) } returns behandling
            every { behandlingService.hentBehandlingDto(behandling.id) } returns DtoTestUtil.lagBehandlingDto(fagsak = fagsak, behandling = behandling)
            every { fagsakService.hentFagsak(behandling.fagsakId) } returns fagsak
            every { personopplysningerService.hentPersonopplysninger(any()) } returns personopplysningerDto
            every { brevsignaturService.lagSignatur(personopplysningerDto, fagsak.fagsystem) } returns DtoTestUtil.lagSignaturDto()
            every { brevClient.genererHtmlFritekstbrev(any(), any(), any(), any()) } returns "<html />"
            every { familieDokumentClient.genererPdfFraHtml(any()) } returns pdf
            every { brevmottakerUtleder.utledInitielleBrevmottakere(behandling.id) } returns initielleBrevmottakere
            every { brevRepository.findByIdOrNull(any()) } returns null
            every { brevRepository.insert(capture(brevSlot)) } answers { firstArg() }
            every { taskService.save(capture(taskSlot)) } answers { firstArg() }
            every { brevInnholdUtleder.lagHenleggelsesbrevBaksInnhold(any(), any(), any()) } returns lagFritekstBrevRequestDto

            // Act
            brevService.lagHenleggelsesbrevOgOpprettJournalføringstask(
                behandlingId = behandling.id,
                nyeBrevmottakere = nyeBrevmottakere,
            )

            // Assert
            assertThat(brevSlot.isCaptured).isTrue()
            assertThat(brevSlot.captured.behandlingId).isEqualTo(behandling.id)
            assertThat(brevSlot.captured.saksbehandlerHtml).isEqualTo("<html />")
            assertThat(brevSlot.captured.pdf).isEqualTo(Fil(pdf))
            assertThat(brevSlot.captured.mottakere?.personer).hasSize(2)
            assertThat(
                brevSlot.captured.mottakere
                    ?.personer
                    ?.get(0),
            ).isInstanceOfSatisfying(BrevmottakerPersonMedIdent::class.java) {
                assertThat(it.personIdent).isEqualTo(bruker.personIdent)
                assertThat(it.mottakerRolle).isEqualTo(bruker.mottakerRolle)
                assertThat(it.navn).isEqualTo(bruker.navn)
            }
            assertThat(
                brevSlot.captured.mottakere
                    ?.personer
                    ?.get(1),
            ).isInstanceOfSatisfying(BrevmottakerPersonUtenIdent::class.java) {
                assertThat(it.id).isNotNull()
                assertThat(it.mottakerRolle).isEqualTo(verge.mottakerRolle)
                assertThat(it.navn).isEqualTo(verge.navn)
                assertThat(it.adresselinje1).isEqualTo(verge.adresselinje1)
                assertThat(it.adresselinje2).isEqualTo(verge.adresselinje2)
                assertThat(it.poststed).isEqualTo(verge.poststed)
                assertThat(it.postnummer).isEqualTo(verge.postnummer)
                assertThat(it.landkode).isEqualTo(verge.landkode)
            }
            assertThat(brevSlot.captured.mottakere?.organisasjoner).isEmpty()
            assertThat(brevSlot.captured.mottakereJournalposter).isNull()
            assertThat(brevSlot.captured.sporbar).isNotNull()
            assertThat(taskSlot.isCaptured).isTrue()
            assertThat(taskSlot.captured.type).isEqualTo(JournalførBrevTask.TYPE)
            assertThat(taskSlot.captured.payload).isEqualTo(behandling.id.toString())
        }

        @Test
        fun `skal oppdatere eksisterende brev til henleggelsesbrev og opprette journalføringstask for fagsystem BA`() {
            // Arrange
            val fagsak = DomainUtil.fagsak(stønadstype = Stønadstype.BARNETRYGD)
            val behandling = DomainUtil.behandling(fagsak = fagsak)

            val personopplysningerDto =
                DtoTestUtil.lagPersonopplysningerDto(
                    fullmakt =
                        listOf(
                            DtoTestUtil.lagFullmaktDto(
                                navn = "Navn Navnesen",
                                gyldigFraOgMed = LocalDate.now().minusDays(2),
                                gyldigTilOgMed = LocalDate.now().minusDays(1),
                            ),
                        ),
                )

            val bruker = DomainUtil.lagNyBrevmottakerPersonMedIdent(mottakerRolle = MottakerRolle.BRUKER)
            val verge = DomainUtil.lagNyBrevmottakerPersonUtenIdent(mottakerRolle = MottakerRolle.VERGE)
            val nyeBrevmottakere = listOf(bruker, verge)

            val initielleBrevmottakere =
                DomainUtil.lagBrevmottakere(
                    personer =
                        listOf(
                            DomainUtil.lagBrevmottakerPersonMedIdent(
                                personIdent = bruker.personIdent,
                                mottakerRolle = bruker.mottakerRolle,
                                navn = bruker.navn,
                            ),
                        ),
                    organisasjoner = emptyList(),
                )

            val brev = DomainUtil.lagBrev(behandlingId = behandling.id, mottakere = initielleBrevmottakere)

            val lagFritekstBrevRequestDto =
                DtoTestUtil.lagFritekstBrevRequestDto(
                    personIdent = bruker.personIdent,
                    navn = bruker.navn,
                )

            val pdf = ByteArray(0)

            val taskSlot = slot<Task>()
            val brevSlot = slot<Brev>()

            every { behandlingService.hentBehandling(behandling.id) } returns behandling
            every { behandlingService.hentBehandlingDto(behandling.id) } returns DtoTestUtil.lagBehandlingDto(fagsak = fagsak, behandling = behandling)
            every { fagsakService.hentFagsak(behandling.fagsakId) } returns fagsak
            every { personopplysningerService.hentPersonopplysninger(any()) } returns personopplysningerDto
            every { brevsignaturService.lagSignatur(personopplysningerDto, fagsak.fagsystem) } returns DtoTestUtil.lagSignaturDto()
            every { brevClient.genererHtmlFritekstbrev(any(), any(), any(), any()) } returns "<html />"
            every { familieDokumentClient.genererPdfFraHtml(any()) } returns pdf
            every { brevmottakerUtleder.utledInitielleBrevmottakere(behandling.id) } returns initielleBrevmottakere
            every { brevRepository.findByIdOrNull(any()) } returns brev
            every { brevRepository.update(capture(brevSlot)) } answers { firstArg() }
            every { taskService.save(capture(taskSlot)) } answers { firstArg() }
            every { brevInnholdUtleder.lagHenleggelsesbrevBaksInnhold(any(), any(), any()) } returns lagFritekstBrevRequestDto

            // Act
            brevService.lagHenleggelsesbrevOgOpprettJournalføringstask(
                behandlingId = behandling.id,
                nyeBrevmottakere = nyeBrevmottakere,
            )

            // Assert
            assertThat(brevSlot.isCaptured).isTrue()
            assertThat(brevSlot.captured.behandlingId).isEqualTo(behandling.id)
            assertThat(brevSlot.captured.saksbehandlerHtml).isEqualTo("<html />")
            assertThat(brevSlot.captured.pdf).isEqualTo(Fil(pdf))
            assertThat(brevSlot.captured.mottakere?.personer).hasSize(2)
            assertThat(
                brevSlot.captured.mottakere
                    ?.personer
                    ?.get(0),
            ).isInstanceOfSatisfying(BrevmottakerPersonMedIdent::class.java) {
                assertThat(it.personIdent).isEqualTo(bruker.personIdent)
                assertThat(it.mottakerRolle).isEqualTo(bruker.mottakerRolle)
                assertThat(it.navn).isEqualTo(bruker.navn)
            }
            assertThat(
                brevSlot.captured.mottakere
                    ?.personer
                    ?.get(1),
            ).isInstanceOfSatisfying(BrevmottakerPersonUtenIdent::class.java) {
                assertThat(it.id).isNotNull()
                assertThat(it.mottakerRolle).isEqualTo(verge.mottakerRolle)
                assertThat(it.navn).isEqualTo(verge.navn)
                assertThat(it.adresselinje1).isEqualTo(verge.adresselinje1)
                assertThat(it.adresselinje2).isEqualTo(verge.adresselinje2)
                assertThat(it.poststed).isEqualTo(verge.poststed)
                assertThat(it.postnummer).isEqualTo(verge.postnummer)
                assertThat(it.landkode).isEqualTo(verge.landkode)
            }
            assertThat(brevSlot.captured.mottakere?.organisasjoner).isEmpty()
            assertThat(brevSlot.captured.mottakereJournalposter).isNull()
            assertThat(brevSlot.captured.sporbar).isNotNull()
            assertThat(taskSlot.isCaptured).isTrue()
            assertThat(taskSlot.captured.type).isEqualTo(JournalførBrevTask.TYPE)
            assertThat(taskSlot.captured.payload).isEqualTo(behandling.id.toString())
        }

        @Test
        fun `skal opprette henleggelsesbrev og opprette journalføringstask for fagsystem EF`() {
            // Arrange
            val fagsak = DomainUtil.fagsak(stønadstype = Stønadstype.SKOLEPENGER)
            val behandling = DomainUtil.behandling(fagsak = fagsak)

            val personopplysningerDto =
                DtoTestUtil.lagPersonopplysningerDto(
                    fullmakt =
                        listOf(
                            DtoTestUtil.lagFullmaktDto(
                                navn = "Navn Navnesen",
                                gyldigFraOgMed = LocalDate.now().minusDays(2),
                                gyldigTilOgMed = LocalDate.now().minusDays(1),
                            ),
                        ),
                )

            val bruker = DomainUtil.lagNyBrevmottakerPersonMedIdent(mottakerRolle = MottakerRolle.BRUKER)
            val nyeBrevmottakere = listOf(bruker)

            val pdf = ByteArray(0)

            val taskSlot = slot<Task>()
            val brevSlot = slot<Brev>()

            every { behandlingService.hentBehandling(behandling.id) } returns behandling
            every { behandlingService.hentBehandlingDto(behandling.id) } returns DtoTestUtil.lagBehandlingDto(fagsak = fagsak, behandling = behandling)
            every { fagsakService.hentFagsak(behandling.fagsakId) } returns fagsak
            every { personopplysningerService.hentPersonopplysninger(any()) } returns personopplysningerDto
            every { brevsignaturService.lagSignatur(personopplysningerDto, fagsak.fagsystem) } returns DtoTestUtil.lagSignaturDto()
            every { brevClient.genererHtml(any(), any(), any(), any(), any(), any()) } returns "<html />"
            every { familieDokumentClient.genererPdfFraHtml(any()) } returns pdf
            every { brevRepository.findByIdOrNull(any()) } returns null
            every { brevRepository.insert(capture(brevSlot)) } answers { firstArg() }
            every { taskService.save(capture(taskSlot)) } answers { firstArg() }

            // Act
            brevService.lagHenleggelsesbrevOgOpprettJournalføringstask(
                behandlingId = behandling.id,
                nyeBrevmottakere = nyeBrevmottakere,
            )

            // Assert
            assertThat(brevSlot.isCaptured).isTrue()
            assertThat(brevSlot.captured.behandlingId).isEqualTo(behandling.id)
            assertThat(brevSlot.captured.saksbehandlerHtml).isEqualTo("<html />")
            assertThat(brevSlot.captured.pdf).isEqualTo(Fil(pdf))
            assertThat(brevSlot.captured.mottakere?.personer).hasSize(1)
            assertThat(
                brevSlot.captured.mottakere
                    ?.personer
                    ?.get(0),
            ).isInstanceOfSatisfying(BrevmottakerPersonMedIdent::class.java) {
                assertThat(it.personIdent).isEqualTo(bruker.personIdent)
                assertThat(it.mottakerRolle).isEqualTo(bruker.mottakerRolle)
                assertThat(it.navn).isEqualTo(bruker.navn)
            }
            assertThat(brevSlot.captured.mottakere?.organisasjoner).isEmpty()
            assertThat(brevSlot.captured.mottakereJournalposter).isNull()
            assertThat(brevSlot.captured.sporbar).isNotNull()
            assertThat(taskSlot.isCaptured).isTrue()
            assertThat(taskSlot.captured.type).isEqualTo(JournalførBrevTask.TYPE)
            assertThat(taskSlot.captured.payload).isEqualTo(behandling.id.toString())
        }

        @Test
        fun `skal oppdatere eksisterende brev til henleggelsesbrev og opprette journalføringstask for fagsystem EF`() {
            // Arrange
            val fagsak = DomainUtil.fagsak()
            val behandling = DomainUtil.behandling(fagsak = fagsak)

            val personopplysningerDto =
                DtoTestUtil.lagPersonopplysningerDto(
                    fullmakt =
                        listOf(
                            DtoTestUtil.lagFullmaktDto(
                                navn = "Navn Navnesen",
                                gyldigFraOgMed = LocalDate.now().minusDays(2),
                                gyldigTilOgMed = LocalDate.now().minusDays(1),
                            ),
                        ),
                )

            val bruker = DomainUtil.lagNyBrevmottakerPersonMedIdent(mottakerRolle = MottakerRolle.BRUKER)
            val nyeBrevmottakere = listOf(bruker)

            val initielleBrevmottakere =
                DomainUtil.lagBrevmottakere(
                    personer =
                        listOf(
                            DomainUtil.lagBrevmottakerPersonMedIdent(
                                personIdent = bruker.personIdent,
                                mottakerRolle = bruker.mottakerRolle,
                                navn = bruker.navn,
                            ),
                        ),
                    organisasjoner = emptyList(),
                )
            val brev = DomainUtil.lagBrev(behandlingId = behandling.id, mottakere = initielleBrevmottakere)

            val pdf = ByteArray(0)

            val taskSlot = slot<Task>()
            val brevSlot = slot<Brev>()

            every { behandlingService.hentBehandling(behandling.id) } returns behandling
            every { behandlingService.hentBehandlingDto(behandling.id) } returns DtoTestUtil.lagBehandlingDto(fagsak = fagsak, behandling = behandling)
            every { fagsakService.hentFagsak(behandling.fagsakId) } returns fagsak
            every { personopplysningerService.hentPersonopplysninger(any()) } returns personopplysningerDto
            every { brevsignaturService.lagSignatur(personopplysningerDto, fagsak.fagsystem) } returns DtoTestUtil.lagSignaturDto()
            every { brevClient.genererHtml(any(), any(), any(), any(), any(), any()) } returns "<html />"
            every { familieDokumentClient.genererPdfFraHtml(any()) } returns pdf
            every { brevRepository.findByIdOrNull(any()) } returns brev
            every { brevRepository.update(capture(brevSlot)) } answers { firstArg() }
            every { taskService.save(capture(taskSlot)) } answers { firstArg() }

            // Act
            brevService.lagHenleggelsesbrevOgOpprettJournalføringstask(
                behandlingId = behandling.id,
                nyeBrevmottakere = nyeBrevmottakere,
            )

            // Assert
            assertThat(brevSlot.isCaptured).isTrue()
            assertThat(brevSlot.captured.behandlingId).isEqualTo(behandling.id)
            assertThat(brevSlot.captured.saksbehandlerHtml).isEqualTo("<html />")
            assertThat(brevSlot.captured.pdf).isEqualTo(Fil(pdf))
            assertThat(brevSlot.captured.mottakere?.personer).hasSize(1)
            assertThat(
                brevSlot.captured.mottakere
                    ?.personer
                    ?.get(0),
            ).isInstanceOfSatisfying(BrevmottakerPersonMedIdent::class.java) {
                assertThat(it.personIdent).isEqualTo(bruker.personIdent)
                assertThat(it.mottakerRolle).isEqualTo(bruker.mottakerRolle)
                assertThat(it.navn).isEqualTo(bruker.navn)
            }
            assertThat(brevSlot.captured.mottakere?.organisasjoner).isEmpty()
            assertThat(brevSlot.captured.mottakereJournalposter).isNull()
            assertThat(brevSlot.captured.sporbar).isNotNull()
            assertThat(taskSlot.isCaptured).isTrue()
            assertThat(taskSlot.captured.type).isEqualTo(JournalførBrevTask.TYPE)
            assertThat(taskSlot.captured.payload).isEqualTo(behandling.id.toString())
        }
    }
}
