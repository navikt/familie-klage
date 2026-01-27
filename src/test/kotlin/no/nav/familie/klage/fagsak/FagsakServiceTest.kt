package no.nav.familie.klage.fagsak

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.klage.fagsak.domain.FagsakDomain
import no.nav.familie.klage.fagsak.domain.FagsakPerson
import no.nav.familie.klage.fagsak.domain.PersonIdent
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.klage.infrastruktur.featuretoggle.Toggle
import no.nav.familie.klage.institusjon.Institusjon
import no.nav.familie.klage.institusjon.InstitusjonService
import no.nav.familie.klage.personopplysninger.pdl.PdlClient
import no.nav.familie.klage.personopplysninger.pdl.PdlIdent
import no.nav.familie.klage.personopplysninger.pdl.PdlIdenter
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import no.nav.familie.klage.testutil.DomainUtil.fagsakDomain
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDateTime
import java.util.UUID

class FagsakServiceTest {
    private val fagsakRepository = mockk<FagsakRepository>()
    private val fagsakPersonService = mockk<FagsakPersonService>()
    private val pdlClient = mockk<PdlClient>()
    private val institusjonService = mockk<InstitusjonService>()
    private val featureToggleService = mockk<FeatureToggleService>()

    private val fagsakService: FagsakService =
        FagsakService(
            fagsakRepository = fagsakRepository,
            fagsakPersonService = fagsakPersonService,
            pdlClient = pdlClient,
            institusjonService = institusjonService,
            featureToggleService = featureToggleService,
        )

    @BeforeEach
    fun setup() {
        every { featureToggleService.isEnabled(Toggle.SKAL_KUNNE_BEHANDLE_BA_INSTITUSJON_FAGSAKER) } returns true
    }

    @Nested
    inner class HentEllerOpprettFagsak {
        @Test
        fun `skal opprette fagsak med institusjon`() {
            // Arrange
            val ident = "12345678903"
            val orgNummer = "123456789"
            val stønadstype = Stønadstype.BARNETRYGD
            val fagsystem = Fagsystem.BA
            val eksternId = "123"

            val fagsakPerson =
                FagsakPerson(
                    id = UUID.randomUUID(),
                    identer = setOf(PersonIdent(ident)),
                    opprettetAv = "A",
                    opprettetTid = LocalDateTime.now(),
                )

            val institusjon =
                Institusjon(
                    orgNummer = orgNummer,
                    navn = "navn",
                )

            every {
                pdlClient.hentPersonidenter(ident, stønadstype, true)
            } returns PdlIdenter(listOf(PdlIdent(ident, false)))

            every {
                fagsakPersonService.hentEllerOpprettPerson(setOf(ident), ident)
            } returns fagsakPerson

            every {
                fagsakPersonService.oppdaterIdent(fagsakPerson, ident)
            } returns fagsakPerson

            every {
                institusjonService.hentEllerLagreInstitusjon(orgNummer)
            } returns institusjon

            every {
                fagsakRepository.findByEksternIdAndFagsystemAndStønadstype(eksternId, fagsystem, stønadstype)
            } returns null

            every {
                fagsakRepository.insert(any())
            } returnsArgument 0

            // Act
            val opprettetFagsak =
                fagsakService.hentEllerOpprettFagsak(
                    ident = ident,
                    orgNummer = orgNummer,
                    eksternId = eksternId,
                    fagsystem = fagsystem,
                    stønadstype = stønadstype,
                )

            // Assert
            assertThat(opprettetFagsak.id).isNotNull()
            assertThat(opprettetFagsak.fagsakPersonId).isEqualTo(fagsakPerson.id)
            assertThat(opprettetFagsak.institusjon).isEqualTo(institusjon)
            assertThat(opprettetFagsak.personIdenter).hasSize(1)
            assertThat(opprettetFagsak.personIdenter).anySatisfy { assertThat(it.ident).isEqualTo(ident) }
            assertThat(opprettetFagsak.eksternId).isEqualTo(eksternId)
            assertThat(opprettetFagsak.stønadstype).isEqualTo(stønadstype)
            assertThat(opprettetFagsak.fagsystem).isEqualTo(fagsystem)
            assertThat(opprettetFagsak.sporbar).isNotNull()
        }

        @Test
        fun `skal opprette fagsak uten institusjon`() {
            // Arrange
            val ident = "12345678903"
            val stønadstype = Stønadstype.BARNETRYGD
            val fagsystem = Fagsystem.BA
            val eksternId = "123"

            val fagsakPerson =
                FagsakPerson(
                    id = UUID.randomUUID(),
                    identer = setOf(PersonIdent(ident)),
                    opprettetAv = "A",
                    opprettetTid = LocalDateTime.now(),
                )

            val fagsakSlot = slot<FagsakDomain>()

            every {
                pdlClient.hentPersonidenter(ident, stønadstype, true)
            } returns
                PdlIdenter(
                    listOf(
                        PdlIdent(ident, false),
                    ),
                )

            every {
                fagsakPersonService.hentEllerOpprettPerson(setOf(ident), ident)
            } returns fagsakPerson

            every {
                fagsakPersonService.oppdaterIdent(fagsakPerson, ident)
            } returns fagsakPerson

            every {
                fagsakRepository.findByEksternIdAndFagsystemAndStønadstype(eksternId, fagsystem, stønadstype)
            } returns null

            every {
                fagsakRepository.insert(capture(fagsakSlot))
            } returnsArgument 0

            // Act
            val opprettetFagsak =
                fagsakService.hentEllerOpprettFagsak(
                    ident = ident,
                    orgNummer = null,
                    eksternId = eksternId,
                    fagsystem = fagsystem,
                    stønadstype = stønadstype,
                )

            // Assert
            assertThat(opprettetFagsak.id).isNotNull()
            assertThat(opprettetFagsak.fagsakPersonId).isEqualTo(fagsakPerson.id)
            assertThat(opprettetFagsak.institusjon).isNull()
            assertThat(opprettetFagsak.personIdenter).hasSize(1)
            assertThat(opprettetFagsak.personIdenter).anySatisfy { assertThat(it.ident).isEqualTo(ident) }
            assertThat(opprettetFagsak.eksternId).isEqualTo(eksternId)
            assertThat(opprettetFagsak.stønadstype).isEqualTo(stønadstype)
            assertThat(opprettetFagsak.fagsystem).isEqualTo(fagsystem)
            assertThat(opprettetFagsak.sporbar).isNotNull()
        }
    }

    @Nested
    inner class HentFagsakForEksternIdOgFagsystem {
        @ParameterizedTest
        @EnumSource(Fagsystem::class, names = ["BA", "KS"], mode = EnumSource.Mode.EXCLUDE)
        fun `skal kaste feil dersom fagsystem ikke er BA eller KS og støndstype er null`(fagsystem: Fagsystem) {
            // Act & Assert
            val feil =
                assertThrows<Feil> {
                    fagsakService.hentFagsakForEksternIdOgFagsystem(
                        eksternId = "123",
                        fagsystem = fagsystem,
                        stønadstype = null,
                    )
                }
            assertThat(feil.message).isEqualTo("Stønadstype må spesifiseres for fagsystem $fagsystem")
        }

        @ParameterizedTest
        @EnumSource(Stønadstype::class, names = ["BARNETRYGD", "KONTANTSTØTTE"])
        fun `skal returnere null dersom fagsystem er BA eller KS og ekstern fagsak ikke finnes`(stønadstype: Stønadstype) {
            // Arrange
            val fagsak = fagsak(stønadstype = stønadstype)
            val fagsystem = if (stønadstype == Stønadstype.BARNETRYGD) Fagsystem.BA else Fagsystem.KS

            every { fagsakRepository.findByEksternIdAndFagsystemAndStønadstype(fagsak.eksternId, fagsystem, stønadstype) } returns null

            // Act & Assert
            val fagsakForEksternIdOgFagsystem =
                fagsakService.hentFagsakForEksternIdOgFagsystem(
                    eksternId = fagsak.eksternId,
                    fagsystem = fagsystem,
                    stønadstype = null,
                )

            assertThat(fagsakForEksternIdOgFagsystem).isNull()
        }

        @ParameterizedTest
        @EnumSource(Stønadstype::class)
        fun `skal returnere fagsak dersom fagsak med eksternId finnes`(stønadstype: Stønadstype) {
            // Arrange
            val fagsystem =
                when (stønadstype) {
                    Stønadstype.BARNETRYGD -> Fagsystem.BA
                    Stønadstype.KONTANTSTØTTE -> Fagsystem.KS
                    Stønadstype.BARNETILSYN, Stønadstype.SKOLEPENGER, Stønadstype.OVERGANGSSTØNAD -> Fagsystem.EF
                }
            val fagsak = fagsakDomain(fagsystem = fagsystem, stønadstype = stønadstype)

            every { fagsakRepository.findByEksternIdAndFagsystemAndStønadstype(fagsak.eksternId, fagsystem, stønadstype) } returns fagsak
            every { fagsakPersonService.hentIdenter(fagsak.fagsakPersonId) } returns emptySet()

            // Act
            val fagsakMedEksternId =
                fagsakService.hentFagsakForEksternIdOgFagsystem(
                    eksternId = fagsak.eksternId,
                    fagsystem = fagsystem,
                    stønadstype = stønadstype,
                )

            // Assert
            assertThat(fagsakMedEksternId).isNotNull
            assertThat(fagsakMedEksternId!!.id).isEqualTo(fagsak.id)
            assertThat(fagsakMedEksternId.eksternId).isEqualTo(fagsak.eksternId)
            assertThat(fagsakMedEksternId.fagsystem).isEqualTo(fagsak.fagsystem)
            assertThat(fagsakMedEksternId.stønadstype).isEqualTo(fagsak.stønadstype)
            assertThat(fagsakMedEksternId.fagsakPersonId).isEqualTo(fagsak.fagsakPersonId)
        }
    }
}
