package no.nav.familie.klage.integrasjoner

import no.nav.familie.klage.behandling.domain.StønadsType
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
        stønadstype: StønadsType,
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
            eksternReferanseId = "$behandlingId-blankett"
        )
    }
}