package no.nav.familie.klage.distribusjon

import no.nav.familie.klage.fagsak.domain.PersonIdent
import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.testutil.DomainUtil
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDateTime

internal class DistribusjonResultatServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var distribusjonResultatService: DistribusjonResultatService

    @Autowired
    private lateinit var distribusjonResultatRepository: DistribusjonResultatRepository

    val fagsak = DomainUtil.fagsakDomain().tilFagsakMedPerson(setOf(PersonIdent("1")))
    val behandling = DomainUtil.behandling(fagsak = fagsak)

    @BeforeEach
    fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagreBehandling(behandling)
    }

    @Test
    fun `skal opprette distribusjonResultat hvis det ikke finnes fra før`() {
        Assertions.assertThat(distribusjonResultatRepository.findAll()).isEmpty()
        distribusjonResultatService.hentEllerOpprettDistribusjonResultat(behandling.id)
        Assertions.assertThat(distribusjonResultatRepository.findAll()).isNotEmpty()
        Assertions.assertThat(distribusjonResultatRepository.findById(behandling.id)).isNotNull
    }

    @Test
    fun `skal oppdatere journalpostId på distribusjonResultat`() {
        val journalpostId = "1234"
        distribusjonResultatService.hentEllerOpprettDistribusjonResultat(behandling.id)
        distribusjonResultatService.oppdaterJournalpostId(journalpostId = journalpostId, behandlingId = behandling.id)
        Assertions.assertThat(distribusjonResultatRepository.findByIdOrNull(behandling.id)?.journalpostId).isEqualTo(journalpostId)
    }

    @Test
    fun `oppdatering av journalpostId skal feile hvis det ikke foreligger et distribusjonResultat`() {
        val journalpostId = "1234"
        assertThrows<Exception> {
            distribusjonResultatService.oppdaterJournalpostId(journalpostId = journalpostId, behandlingId = behandling.id)
        }
    }

    @Test
    fun `skal oppdatere brevDistribusjonId på distribusjonResultat`() {
        val brevDistribusjonId = "12345"
        distribusjonResultatService.hentEllerOpprettDistribusjonResultat(behandling.id)
        distribusjonResultatService.oppdaterBrevDistribusjonId(brevDistribusjonId = brevDistribusjonId, behandlingId = behandling.id)
        Assertions.assertThat(distribusjonResultatRepository.findByIdOrNull(behandling.id)?.brevDistribusjonId).isEqualTo(brevDistribusjonId)
    }

    @Test
    fun `skal oppdatere oversendtTilKabalTidspunkt på distribusjonResultat`() {
        val tidspunkt = LocalDateTime.now()
        distribusjonResultatService.hentEllerOpprettDistribusjonResultat(behandling.id)
        distribusjonResultatService.oppdaterSendtTilKabalTid(oversendtTilKabalTidspunkt = tidspunkt, behandlingId = behandling.id)
        Assertions.assertThat(distribusjonResultatRepository.findByIdOrNull(behandling.id)?.oversendtTilKabalTidspunkt)
            .isEqualToIgnoringNanos(tidspunkt)
    }
}
