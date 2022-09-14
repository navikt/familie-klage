package no.nav.familie.klage.distribusjon

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.kabal.KabalService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FerdigstillBehandlingService(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val distribusjonService: DistribusjonService,
    private val kabalService: KabalService,
    private val klageresultatRepository: KlageresultatRepository
) {

    /**
     * Skal ikke være @transactional fordi det er mulig å komme delvis igjennom løypa
     */
    fun ferdigstillKlagebehandling(behandlingId: UUID) {
        val persistertResultat = klageresultatRepository.findByIdOrNull(behandlingId)
//        val (jounrnalpostId, distribusjonsId, datoElle) = klageresultatRepository.findByIdOrNull(behandlingId)

        val persistertJournalpostId = if (persistertResultat?.journalpostId == null) {
            val journalpostId = distribusjonService.journalførBrev(behandlingId)
            klageresultatRepository.update(Klageresultat(behandlingId = behandlingId, journalpostId = journalpostId))
            journalpostId
        } else {
            persistertResultat.journalpostId
        }

        if (persistertResultat?.distribusjonsId == null) {
            val distribusjonsId = distribusjonService.distribuerBrev(persistertJournalpostId)
        }
    }
}
