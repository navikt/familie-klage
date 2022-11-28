package no.nav.familie.klage.kabal

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.PåklagetVedtak
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.fagsak.domain.tilYtelse
import no.nav.familie.klage.infrastruktur.config.LenkeConfig
import no.nav.familie.klage.vurdering.domain.Vurdering
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.FagsystemType
import org.springframework.stereotype.Service

@Service
class KabalService(
    private val kabalClient: KabalClient,
    private val lenkeConfig: LenkeConfig
) {

    fun sendTilKabal(fagsak: Fagsak, behandling: Behandling, vurdering: Vurdering) {
        val oversendtKlageAnkeV3 = lagKlageOversendelseV3(fagsak, behandling, vurdering)
        kabalClient.sendTilKabal(oversendtKlageAnkeV3)
    }

    private fun lagKlageOversendelseV3(fagsak: Fagsak, behandling: Behandling, vurdering: Vurdering): OversendtKlageAnkeV3 {
        return OversendtKlageAnkeV3(
            type = Type.KLAGE,
            klager = OversendtKlager(
                id = OversendtPartId(
                    type = OversendtPartIdType.PERSON,
                    verdi = fagsak.hentAktivIdent()
                )
            ),
            fagsak = OversendtSak(fagsakId = fagsak.eksternId, fagsystem = fagsak.fagsystem),
            kildeReferanse = behandling.eksternBehandlingId.toString(),
            innsynUrl = lagInnsynUrl(fagsak, behandling.påklagetVedtak),
            hjemler = vurdering.hjemmel?.let { listOf(it.kabalHjemmel) } ?: emptyList(),
            forrigeBehandlendeEnhet = behandling.behandlendeEnhet,
            tilknyttedeJournalposter = listOf(), // TODO: klagebrev kan puttes på automatisk, vedtaksbrev fra EF-sak må hentes fra iverksett, klage må velges ved ferdigstilling eller ved journalføring av klage
            brukersHenvendelseMottattNavDato = behandling.klageMottatt,
            innsendtTilNav = behandling.klageMottatt,
            kilde = fagsak.fagsystem,
            ytelse = fagsak.stønadstype.tilYtelse()
        )
    }

    private fun lagInnsynUrl(fagsak: Fagsak, påklagetVedtak: PåklagetVedtak): String {
        val fagsystemUrl = when (fagsak.fagsystem) {
            Fagsystem.EF -> lenkeConfig.efSakLenke
            Fagsystem.BA -> lenkeConfig.baSakLenke
            Fagsystem.KS -> error("Ikke implementert støtte for KS")
        }
        val påklagetVedtakDetaljer = påklagetVedtak.påklagetVedtakDetaljer
        return if (påklagetVedtakDetaljer != null) {
            "$fagsystemUrl/fagsak/${fagsak.eksternId}/${påklagetVedtakDetaljer.eksternFagsystemBehandlingId}"
        } else {
                "$fagsystemUrl/fagsak/${fagsak.eksternId}/saksoversikt"
        }
    }
}
