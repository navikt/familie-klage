package no.nav.familie.klage.kabal

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.PåklagetVedtak
import no.nav.familie.klage.brev.domain.BrevmottakerPersonUtenIdent
import no.nav.familie.klage.brev.domain.Brevmottakere
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.infrastruktur.config.LenkeConfig
import no.nav.familie.klage.integrasjoner.FamilieIntegrasjonerClient
import no.nav.familie.klage.kabal.domain.OversendtKlageAnke
import no.nav.familie.klage.kabal.domain.OversendtKlageAnkeV3
import no.nav.familie.klage.kabal.domain.OversendtKlageAnkeV4
import no.nav.familie.klage.vurdering.domain.Vurdering
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.FagsystemType
import org.springframework.stereotype.Service

@Service
class KabalService(
    private val kabalClient: KabalClient,
    private val integrasjonerClient: FamilieIntegrasjonerClient,
    private val lenkeConfig: LenkeConfig,
) {
    fun sendTilKabal(
        fagsak: Fagsak,
        behandling: Behandling,
        vurdering: Vurdering,
        saksbehandlerIdent: String,
        brevMottakere: Brevmottakere,
    ) {
        val saksbehandler = integrasjonerClient.hentSaksbehandlerInfo(saksbehandlerIdent)
        val oversendtKlageAnke =
            lagKlageOversendelse(fagsak, behandling, vurdering, saksbehandler.enhet, brevMottakere)
        kabalClient.sendTilKabal(oversendtKlageAnke)
    }

    private fun lagKlageOversendelse(
        fagsak: Fagsak,
        behandling: Behandling,
        vurdering: Vurdering,
        saksbehandlersEnhet: String,
        brevMottakere: Brevmottakere,
    ): OversendtKlageAnke =
        if (behandlingInneholderBrevmottakerUtenIdent(brevMottakere)) {
            OversendtKlageAnkeV4.lagKlageOversendelse(
                fagsak = fagsak,
                behandling = behandling,
                vurdering = vurdering,
                saksbehandlersEnhet = saksbehandlersEnhet,
                brevMottakere = brevMottakere,
            )
        } else {
            OversendtKlageAnkeV3.lagKlageOversendelse(
                fagsak = fagsak,
                behandling = behandling,
                vurdering = vurdering,
                saksbehandlersEnhet = saksbehandlersEnhet,
                brevMottakere = brevMottakere,
                innsynUrl = lagInnsynUrl(fagsak, behandling.påklagetVedtak),
            )
        }

    private fun behandlingInneholderBrevmottakerUtenIdent(brevMottakere: Brevmottakere): Boolean =
        brevMottakere.personer.any { it is BrevmottakerPersonUtenIdent }

    private fun lagInnsynUrl(
        fagsak: Fagsak,
        påklagetVedtak: PåklagetVedtak,
    ): String {
        val fagsystemUrl =
            when (fagsak.fagsystem) {
                Fagsystem.EF -> lenkeConfig.efSakLenke
                Fagsystem.BA -> lenkeConfig.baSakLenke
                Fagsystem.KS -> lenkeConfig.ksSakLenke
            }
        val påklagetVedtakDetaljer = påklagetVedtak.påklagetVedtakDetaljer
        return if (påklagetVedtakDetaljer != null &&
            påklagetVedtakDetaljer.fagsystemType == FagsystemType.ORDNIÆR &&
            påklagetVedtakDetaljer.eksternFagsystemBehandlingId != null
        ) {
            "$fagsystemUrl/fagsak/${fagsak.eksternId}/${påklagetVedtakDetaljer.eksternFagsystemBehandlingId}"
        } else {
            "$fagsystemUrl/fagsak/${fagsak.eksternId}/saksoversikt"
        }
    }
}
