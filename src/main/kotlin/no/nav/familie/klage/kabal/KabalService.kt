package no.nav.familie.klage.kabal

import VurderingDto
import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.infrastruktur.config.LenkeConfig
import no.nav.familie.kontrakter.felles.Fagsystem
import org.springframework.stereotype.Service

@Service
class KabalService(
    private val kabalClient: KabalClient,
    private val lenkeConfig: LenkeConfig
) {

    fun sendTilKabal(fagsak: Fagsak, behandling: Behandling, vurdering: VurderingDto?) {
        val oversendtKlageAnkeV3 = lagKlageOversendelseV3(fagsak, behandling, vurdering)
        kabalClient.sendTilKabal(oversendtKlageAnkeV3)
    }

    private fun lagKlageOversendelseV3(fagsak: Fagsak, behandling: Behandling, vurdering: VurderingDto?): OversendtKlageAnkeV3 {

        return OversendtKlageAnkeV3(
            type = Type.KLAGE,
            klager = OversendtKlager(
                id = OversendtPartId(
                    type = OversendtPartIdType.PERSON,
                    verdi = fagsak.hentAktivIdent()
                )
            ),
            fagsak = OversendtSak(fagsakId = fagsak.eksternId, fagsystem = fagsak.fagsystem.tilKildeFagsystem()),
            kildeReferanse = behandling.eksternBehandlingId,
            innsynUrl = lagInnsynUrl(fagsak, behandling.eksternBehandlingId),
            hjemler = vurdering?.hjemmel?.let { listOf(it.kabalHjemmel) } ?: emptyList(),
            forrigeBehandlendeEnhet = behandling.behandlendeEnhet,
            tilknyttedeJournalposter = listOf(), // TODO: klagebrev kan puttes på automatisk, vedtaksbrev fra EF-sak må hentes fra iverksett, klage må velges ved ferdigstilling eller ved journalføring av klage
            brukersHenvendelseMottattNavDato = behandling.klageMottatt,
            innsendtTilNav = behandling.klageMottatt,
            kilde = fagsak.fagsystem.tilKildeFagsystem(),
            ytelse = fagsak.stønadstype.tilYtelse()
        )
    }

    private fun lagInnsynUrl(fagsak: Fagsak, eksternBehandlingId: String?): String {

        val fagsystemUrl = when (fagsak.fagsystem) {
            Fagsystem.EF -> lenkeConfig.efSakLenke
            Fagsystem.BA -> lenkeConfig.baSakLenke
            Fagsystem.KS -> error("Ikke implementert støtte for KS")
            Fagsystem.IT01 -> error("Skal ikke ha infotrygd som fagsystem for klager")
        }
        return eksternBehandlingId?.let { "$fagsystemUrl/fagsak/${fagsak.eksternId}/$eksternBehandlingId" }
            ?: "$fagsystemUrl/fagsak/${fagsak.eksternId}/saksoversikt"
    }
}
