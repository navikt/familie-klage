package no.nav.familie.klage.brev.baks

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BaksBrevService(
    private val baksBrevHenter: BaksBrevHenter,
    private val baksBrevOppretter: BaksBrevOppretter,
    private val baksBrevOppdaterer: BaksBrevOppdaterer,
) {
    fun hentBrev(behandlingId: UUID): BaksBrev {
        return baksBrevHenter.hentBrev(behandlingId)
    }

    @Transactional
    fun opprettEllerOppdaterBrev(behandlingId: UUID): BaksBrev {
        val baksBrev = baksBrevHenter.hentBrevEllerNull(behandlingId)
        if (baksBrev != null) {
            return baksBrevOppdaterer.oppdaterBrev(baksBrev)
        }
        return baksBrevOppretter.opprettBrev(behandlingId)
    }
}
