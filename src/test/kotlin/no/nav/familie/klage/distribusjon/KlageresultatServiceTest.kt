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

internal class KlageresultatServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var klageresultatService: KlageresultatService

    @Autowired
    private lateinit var klageresultatRepository: KlageresultatRepository

    val fagsak = DomainUtil.fagsakDomain().tilFagsakMedPerson(setOf(PersonIdent("1")))
    val behandling = DomainUtil.behandling(fagsak = fagsak)

    @BeforeEach
    fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagreBehandling(behandling)
    }

    @Test
    fun `skal opprette klageresultat hvis det ikke finnes fra før`() {
        Assertions.assertThat(klageresultatRepository.findAll()).isEmpty()
        klageresultatService.hentEllerOpprettKlageresultat(behandling.id)
        Assertions.assertThat(klageresultatRepository.findAll()).isNotEmpty()
        Assertions.assertThat(klageresultatRepository.findById(behandling.id)).isNotNull
    }

    @Test
    fun `skal oppdatere journalpostId på klageresultat`() {
        val journalpostId = "1234"
        klageresultatService.hentEllerOpprettKlageresultat(behandling.id)
        klageresultatService.oppdaterJournalpostId(journalpostId = journalpostId, behandlingId = behandling.id)
        Assertions.assertThat(klageresultatRepository.findByIdOrNull(behandling.id)?.journalpostId).isEqualTo(journalpostId)
    }

    @Test
    fun `oppdatering av journalpostId skal feile hvis det ikke foreligger et klageresultat`() {
        val journalpostId = "1234"
        assertThrows<Exception> {
            klageresultatService.oppdaterJournalpostId(journalpostId = journalpostId, behandlingId = behandling.id)
        }
    }

    @Test
    fun `skal oppdatere distribusjonId på klageresultat`() {
        val distribusjonId = "12345"
        klageresultatService.hentEllerOpprettKlageresultat(behandling.id)
        klageresultatService.oppdaterDistribusjonId(distribusjonId = distribusjonId, behandlingId = behandling.id)
        Assertions.assertThat(klageresultatRepository.findByIdOrNull(behandling.id)?.distribusjonId).isEqualTo(distribusjonId)
    }

    @Test
    fun `skal oppdatere oversendtTilKabalTidspunkt på klageresultat`() {
        val tidspunkt = LocalDateTime.now()
        klageresultatService.hentEllerOpprettKlageresultat(behandling.id)
        klageresultatService.oppdaterSendtTilKabalTid(oversendtTilKabalTidspunkt = tidspunkt, behandlingId = behandling.id)
        Assertions.assertThat(klageresultatRepository.findByIdOrNull(behandling.id)?.oversendtTilKabalTidspunkt)
            .isEqualToIgnoringNanos(tidspunkt)
    }
}
