package no.nav.familie.klage.distribusjon

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class DistribusjonResultatService(private val distribusjonResultatRepository: DistribusjonResultatRepository) {

    fun hentEllerOpprettDistribusjonResultat(behandlingId: UUID): DistribusjonResultat {
        val distribusjonResultat = distribusjonResultatRepository.findByIdOrNull(behandlingId)
        return distribusjonResultat ?: distribusjonResultatRepository.insert(DistribusjonResultat(behandlingId = behandlingId))
    }

    fun oppdaterJournalpostId(journalpostId: String, behandlingId: UUID) {
        val distribusjonResultat = hentDistribusjonResultat(behandlingId)
        distribusjonResultatRepository.update(distribusjonResultat.copy(journalpostId = journalpostId))
    }

    fun oppdaterBrevDistribusjonId(brevDistribusjonId: String, behandlingId: UUID) {
        val distribusjonResultat = hentDistribusjonResultat(behandlingId)
        distribusjonResultatRepository.update(distribusjonResultat.copy(brevDistribusjonId = brevDistribusjonId))
    }

    fun oppdaterSendtTilKabalTid(oversendtTilKabalTidspunkt: LocalDateTime, behandlingId: UUID) {
        val distribusjonResultat = hentDistribusjonResultat(behandlingId)
        distribusjonResultatRepository.update(distribusjonResultat.copy(oversendtTilKabalTidspunkt = oversendtTilKabalTidspunkt))
    }

    private fun hentDistribusjonResultat(behandlingId: UUID): DistribusjonResultat {
        val distribusjonResultat = distribusjonResultatRepository.findByIdOrNull(behandlingId)
            ?: error("Kan ikke oppdatere distribusjonResultat som ikke eksisterer")
        return distribusjonResultat
    }
}
