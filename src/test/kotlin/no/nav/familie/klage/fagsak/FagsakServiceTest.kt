package no.nav.familie.klage.fagsak

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.personopplysninger.pdl.PdlClient
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import no.nav.familie.klage.testutil.DomainUtil.fagsakDomain
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class FagsakServiceTest {
    private val fagsakRepository = mockk<FagsakRepository>()
    private val fagsakPersonService = mockk<FagsakPersonService>()
    private val pdlClient = mockk<PdlClient>()

    private val fagsakService: FagsakService =
        FagsakService(
            fagsakRepository = fagsakRepository,
            fagsakPersonService = fagsakPersonService,
            pdlClient = pdlClient,
        )

    @Nested
    inner class HentFagsakForEksternIdOgFagsystem {
        @ParameterizedTest
        @EnumSource(Fagsystem::class, names = ["BA", "KS"], mode = EnumSource.Mode.EXCLUDE)
        fun `skal kaste feil dersom fagsystem ikke er BA eller KS og støndstype er null`(fagsystem: Fagsystem) {
            // Arrange

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
        fun `skal kaste feil dersom fagsystem er BA eller KS og ekstern fagsak ikke finnes`(stønadstype: Stønadstype) {
            // Arrange
            val fagsak = fagsak(stønadstype = stønadstype)
            val fagsystem = if (stønadstype == Stønadstype.BARNETRYGD) Fagsystem.BA else Fagsystem.KS

            every { fagsakRepository.findByEksternIdAndFagsystemAndStønadstype(fagsak.eksternId, fagsystem, stønadstype) } returns null

            // Act & Assert
            val feil =
                assertThrows<Feil> {
                    fagsakService.hentFagsakForEksternIdOgFagsystem(
                        eksternId = fagsak.eksternId,
                        fagsystem = fagsystem,
                        stønadstype = null,
                    )
                }
            assertThat(feil.message).isEqualTo("Finner ikke fagsak for eksternId=${fagsak.eksternId}, fagsystem=$fagsystem og stønadstype=$stønadstype")
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
            assertThat(fagsakMedEksternId.id).isEqualTo(fagsak.id)
            assertThat(fagsakMedEksternId.eksternId).isEqualTo(fagsak.eksternId)
            assertThat(fagsakMedEksternId.fagsystem).isEqualTo(fagsak.fagsystem)
            assertThat(fagsakMedEksternId.stønadstype).isEqualTo(fagsak.stønadstype)
            assertThat(fagsakMedEksternId.fagsakPersonId).isEqualTo(fagsak.fagsakPersonId)
        }
    }
}
