package no.nav.familie.klage.klageinfo

import no.nav.familie.klage.klageinfo.domain.Klage
import no.nav.familie.klage.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class KlageinfoService(
    private val klageRepository: KlageRepository
) {

    fun lagKlage(klage: Klage): Klage = klageRepository.insert(klage)

    fun hentInfo(behandlingId: UUID): Klage {
        if (!sjekkOmKlageEksisterer(behandlingId)) {
            lagKlage(klageMock(behandlingId))
        }
        return klageRepository.findByIdOrThrow(behandlingId)
    }

    fun sjekkOmKlageEksisterer(behandlingId: UUID): Boolean {
        return klageRepository.findById(behandlingId).isPresent
    }
}
