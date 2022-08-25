package no.nav.familie.klage.integrasjoner

import no.nav.familie.klage.fagsak.domain.Stønadstype
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class IntegrasjonerService {
    fun lagArkiverDokumentRequest(
        personIdent: String,
        pdf: ByteArray,
        fagsakId: String?,
        behandlingId: UUID,
        enhet: String,
        stønadstype: Stønadstype,
        dokumenttype: Dokumenttype
    ): ArkiverDokumentRequest {
        val dokument = no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument(
            pdf,
            Filtype.PDFA,
            null,
            "Brev for ${stønadstype.name.lowercase()}",
            dokumenttype
        )
        return ArkiverDokumentRequest(
            fnr = personIdent,
            forsøkFerdigstill = true,
            hoveddokumentvarianter = listOf(dokument),
            vedleggsdokumenter = listOf(),
            fagsakId = fagsakId,
            journalførendeEnhet = enhet,
            eksternReferanseId = "$behandlingId-klage"
        )
    }
}
