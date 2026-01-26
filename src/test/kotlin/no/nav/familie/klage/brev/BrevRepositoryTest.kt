package no.nav.familie.klage.brev

import no.nav.familie.klage.brev.domain.Brev
import no.nav.familie.klage.brev.domain.BrevmottakereJournalposter
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerOrganisasjon
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonMedIdent
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonUtenIdent
import no.nav.familie.klage.brevmottaker.domain.Brevmottakere
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle
import no.nav.familie.klage.distribusjon.domain.BrevmottakerJournalpostMedIdent
import no.nav.familie.klage.felles.domain.Fil
import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.repository.findByIdOrThrow
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

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
    internal fun oppdaterMottakerJournalpost() {
        val brev = brev()
        brevRepository.insert(brev)
        val oppdatertJournalposter = BrevmottakereJournalposter(journalposter = listOf(brevmottakereJournalpost("id")))

        brevRepository.oppdaterMottakerJournalpost(behandling.id, oppdatertJournalposter)

        val oppdatertBrev = brevRepository.findByIdOrThrow(behandling.id)
        assertThat(oppdatertBrev.mottakereJournalposter).isNotEqualTo(brev.mottakereJournalposter)
        assertThat(oppdatertBrev.mottakereJournalposter).isEqualTo(oppdatertJournalposter)
    }

    private fun brev() =
        Brev(
            behandlingId = behandling.id,
            saksbehandlerHtml = "html",
            pdf = Fil("123".toByteArray()),
            mottakere =
                Brevmottakere(
                    personer =
                        listOf(
                            BrevmottakerPersonMedIdent("ident", "navn", MottakerRolle.BRUKER),
                            BrevmottakerPersonUtenIdent(
                                id = UUID.randomUUID(),
                                mottakerRolle = MottakerRolle.BRUKER,
                                navn = "navn",
                                adresselinje1 = "Adresselinje 1",
                                adresselinje2 = "Adresselinje 2",
                                postnummer = "1234",
                                poststed = "Oslo",
                                landkode = "NO",
                            ),
                        ),
                    organisasjoner = listOf(BrevmottakerOrganisasjon("orgnr", "navn", "mottaker")),
                ),
            mottakereJournalposter = BrevmottakereJournalposter(listOf(brevmottakereJournalpost("distId"))),
        )

    private fun brevmottakereJournalpost(distribusjonId: String? = null) =
        BrevmottakerJournalpostMedIdent(
            "ident",
            "journalpostId",
            distribusjonId,
        )
}
