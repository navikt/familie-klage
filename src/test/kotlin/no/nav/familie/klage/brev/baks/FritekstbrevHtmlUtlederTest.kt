package no.nav.familie.klage.brev.baks

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.klage.brev.BrevClient
import no.nav.familie.klage.brev.BrevsignaturService
import no.nav.familie.klage.brev.FritekstBrevRequestDto
import no.nav.familie.klage.brev.SignaturDto
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FritekstbrevHtmlUtlederTest {
    private val brevClient: BrevClient = mockk()
    private val brevsignaturService: BrevsignaturService = mockk()
    private val fagsakService: FagsakService = mockk()
    private val personopplysningerService: PersonopplysningerService = mockk()
    private val fritekstBrevRequestDtoUtleder: FritekstBrevRequestDtoUtleder = mockk()
    private val fritekstbrevHtmlUtleder: FritekstbrevHtmlUtleder = FritekstbrevHtmlUtleder(
        brevClient = brevClient,
        brevsignaturService = brevsignaturService,
        fagsakService = fagsakService,
        personopplysningerService = personopplysningerService,
        fritekstBrevRequestDtoUtleder = fritekstBrevRequestDtoUtleder,
    )

    @Nested
    inner class UtledFritekstbrevHtmlTest {
        @Test
        fun `skal utlede html for fritekstbrev`() {
            // Arrange
            val fagsak = DomainUtil.fagsak(stønadstype = Stønadstype.BARNETRYGD)
            val behandling = DomainUtil.behandling(fagsak)
            val personopplysningerDto = DomainUtil.personopplysningerDto()
            val fritekstBrevRequestDto = FritekstBrevRequestDto(
                "overskrift",
                emptyList(),
                "personIdent",
                "navn",
            )
            val signaturDto = SignaturDto("navn", "enhet")

            every {
                fagsakService.hentFagsak(behandling.fagsakId)
            } returns fagsak

            every {
                personopplysningerService.hentPersonopplysninger(behandling.id)
            } returns personopplysningerDto

            every {
                fritekstBrevRequestDtoUtleder.utled(
                    fagsak,
                    behandling,
                    personopplysningerDto.navn,
                )
            } returns fritekstBrevRequestDto

            every {
                brevsignaturService.lagSignatur(
                    personopplysningerDto, fagsak.fagsystem,
                )
            } returns signaturDto

            every {
                brevClient.genererHtmlFritekstbrev(
                    fritekstBrevRequestDto,
                    signaturDto.navn,
                    signaturDto.enhet,
                )
            } returns "<html />"

            // Act
            val html = fritekstbrevHtmlUtleder.utledFritekstbrevHtml(behandling)

            // Assert
            assertThat(html).isEqualTo("<html />")
        }
    }
}
