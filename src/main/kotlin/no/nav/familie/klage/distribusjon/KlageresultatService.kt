package no.nav.familie.klage.distribusjon

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class KlageresultatService(private val klageresultatRepository: KlageresultatRepository) {

    fun hentEllerOpprettKlageresultat(behandlingId: UUID): Klageresultat {
        val klageResultat = klageresultatRepository.findByIdOrNull(behandlingId)
        return klageResultat ?: klageresultatRepository.insert(Klageresultat(behandlingId = behandlingId))
    }

    fun oppdaterJournalpostId(journalpostId: String, behandlingId: UUID) {
        val klageresultat = hentKlageResultat(behandlingId)
        klageresultatRepository.update(klageresultat.copy(journalpostId = journalpostId))
    }

    fun oppdaterDistribusjonId(distribusjonId: String, behandlingId: UUID) {
        val klageresultat = hentKlageResultat(behandlingId)
        klageresultatRepository.update(klageresultat.copy(distribusjonId = distribusjonId))
    }

    fun oppdaterSendtTilKabalTid(oversendtTilKabalTidspunkt: LocalDateTime, behandlingId: UUID) {
        val klageresultat = hentKlageResultat(behandlingId)
        klageresultatRepository.update(klageresultat.copy(oversendtTilKabalTidspunkt = oversendtTilKabalTidspunkt))
    }

    private fun hentKlageResultat(behandlingId: UUID): Klageresultat {
        val klageresultat = klageresultatRepository.findByIdOrNull(behandlingId)
            ?: error("Kan ikke oppdatere klageresultat som ikke eksisterer")
        return klageresultat
    }
}
