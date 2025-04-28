package no.nav.familie.klage.vurdering

import no.nav.familie.klage.behandling.StegService
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.brev.BrevRepository
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.vurdering.VurderingValidator.validerVurdering
import no.nav.familie.klage.vurdering.domain.Vedtak
import no.nav.familie.klage.vurdering.domain.Vurdering
import no.nav.familie.klage.vurdering.dto.VurderingDto
import no.nav.familie.klage.vurdering.dto.tilDto
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class VurderingService(
    private val vurderingRepository: VurderingRepository,
    private val stegService: StegService,
    private val brevRepository: BrevRepository,
    private val fagsakService: FagsakService,
) {
    fun hentVurdering(behandlingId: UUID): Vurdering? = vurderingRepository.findByIdOrNull(behandlingId)

    fun hentVurderingDto(behandlingId: UUID): VurderingDto? = hentVurdering(behandlingId)?.tilDto()

    @Transactional
    fun lagreVurderingOgOppdaterSteg(vurdering: VurderingDto): VurderingDto {
        val fagsystem = fagsakService.hentFagsakForBehandling(vurdering.behandlingId).fagsystem
        validerVurdering(vurdering, fagsystem)
        if (vurdering.vedtak === Vedtak.OMGJØR_VEDTAK) {
            brevRepository.deleteById(vurdering.behandlingId)
        }

        stegService.oppdaterSteg(vurdering.behandlingId, StegType.VURDERING, StegType.BREV)

        val eksisterendeVurdering = vurderingRepository.findByIdOrNull(vurdering.behandlingId)
        return if (eksisterendeVurdering != null) {
            oppdaterVurdering(vurdering, eksisterendeVurdering).tilDto()
        } else {
            opprettNyVurdering(vurdering).tilDto()
        }
    }

    @Transactional
    fun lagreVurdering(vurdering: VurderingDto): VurderingDto {
        val eksisterendeVurdering = vurderingRepository.findByIdOrNull(vurdering.behandlingId)
        return if (eksisterendeVurdering != null) {
            oppdaterVurdering(vurdering, eksisterendeVurdering).tilDto()
        } else {
            opprettNyVurdering(vurdering).tilDto()
        }
    }

    fun slettVurderingForBehandling(behandlingId: UUID) {
        vurderingRepository.deleteById(behandlingId)
    }

    private fun opprettNyVurdering(vurdering: VurderingDto) =
        vurderingRepository.insert(
            Vurdering(
                behandlingId = vurdering.behandlingId,
                vedtak = vurdering.vedtak,
                årsak = vurdering.årsak,
                begrunnelseOmgjøring = vurdering.begrunnelseOmgjøring,
                hjemmel = vurdering.hjemmel,
                innstillingKlageinstans = vurdering.innstillingKlageinstans,
                dokumentasjonOgUtredning = vurdering.dokumentasjonOgUtredning,
                spørsmåletISaken = vurdering.spørsmåletISaken,
                aktuelleRettskilder = vurdering.aktuelleRettskilder,
                klagersAnførsler = vurdering.klagersAnførsler,
                vurderingAvKlagen = vurdering.vurderingAvKlagen,
                interntNotat = vurdering.interntNotat,
            ),
        )

    private fun oppdaterVurdering(
        vurdering: VurderingDto,
        eksisterendeVurdering: Vurdering,
    ): Vurdering =
        vurderingRepository.update(
            eksisterendeVurdering.copy(
                vedtak = vurdering.vedtak,
                innstillingKlageinstans = vurdering.innstillingKlageinstans,
                årsak = vurdering.årsak,
                begrunnelseOmgjøring = vurdering.begrunnelseOmgjøring,
                hjemmel = vurdering.hjemmel,
                dokumentasjonOgUtredning = vurdering.dokumentasjonOgUtredning,
                spørsmåletISaken = vurdering.spørsmåletISaken,
                aktuelleRettskilder = vurdering.aktuelleRettskilder,
                klagersAnførsler = vurdering.klagersAnførsler,
                vurderingAvKlagen = vurdering.vurderingAvKlagen,
                interntNotat = vurdering.interntNotat,
            ),
        )
}
