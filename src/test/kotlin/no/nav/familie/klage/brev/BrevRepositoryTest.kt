package no.nav.familie.klage.brev

import no.nav.familie.klage.brev.domain.Brev
import no.nav.familie.klage.brev.domain.BrevmottakerOrganisasjon
import no.nav.familie.klage.brev.domain.BrevmottakerPersonMedIdent
import no.nav.familie.klage.brev.domain.Brevmottakere
import no.nav.familie.klage.brev.domain.BrevmottakereJournalpost
import no.nav.familie.klage.brev.domain.BrevmottakereJournalposter
import no.nav.familie.klage.brev.domain.MottakerRolle
import no.nav.familie.klage.felles.domain.Fil
import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.repository.findByIdOrThrow
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class BrevRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    lateinit var brevRepository: BrevRepository

    private val fagsak = fagsak()
    private val behandling = behandling(fagsak)

    @BeforeEach
    internal fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagreBehandling(behandling)
    }

    @Test
    internal fun `lagring og henting av brev`() {
        val brev = brev()
        brevRepository.insert(brev)
        assertThat(brevRepository.findByIdOrThrow(behandling.id))
            .usingRecursiveComparison()
            .ignoringFields("sporbar")
            .isEqualTo(brev)
    }

    @Test
    internal fun `oppdaterMottakerJournalpost`() {
        val brev = brev()
        brevRepository.insert(brev)
        val oppdatertJournalposter = BrevmottakereJournalposter(journalposter = listOf(brevmottakereJournalpost("id")))

        brevRepository.oppdaterMottakerJournalpost(behandling.id, oppdatertJournalposter)

        val oppdatertBrev = brevRepository.findByIdOrThrow(behandling.id)
        assertThat(oppdatertBrev.mottakereJournalposter).isNotEqualTo(brev.mottakereJournalposter)
        assertThat(oppdatertBrev.mottakereJournalposter).isEqualTo(oppdatertJournalposter)
    }

    private fun brev() = Brev(
        behandlingId = behandling.id,
        saksbehandlerHtml = "html",
        pdf = Fil("123".toByteArray()),
        mottakere = Brevmottakere(
            personer = listOf(
                BrevmottakerPersonMedIdent("ident", MottakerRolle.BRUKER, "navn"),
            ),
            organisasjoner = listOf(BrevmottakerOrganisasjon("orgnr", "navn", "mottaker")),
        ),
        mottakereJournalposter = BrevmottakereJournalposter(listOf(brevmottakereJournalpost("distId"))),
    )

    private fun brevmottakereJournalpost(distribusjonId: String? = null) = BrevmottakereJournalpost(
        "ident",
        "journalpostId",
        distribusjonId,
    )
}
