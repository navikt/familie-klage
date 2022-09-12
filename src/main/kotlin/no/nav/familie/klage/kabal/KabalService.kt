package no.nav.familie.klage.kabal

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.vurdering.VurderingService
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class KabalService(
    private val kabalClient: KabalClient,
    private val fagsakService: FagsakService,
    private val vurderingService: VurderingService,
    private val behandlingService: BehandlingService
    ) {

    fun sendTilKabal(behandlingId: UUID, fagsakId: UUID) {
        val oversendtKlageAnkeV3 = lagOversendtKlageAnkeV3Mock(behandlingId, fagsakId)
        kabalClient.sendTilKabal(oversendtKlageAnkeV3)
    }

    fun lagKlageOversendelseV3(behandlingId: UUID, fagsakId: UUID): OversendtKlageAnkeV3 {

        val fagsak = fagsakService.hentFagsak(fagsakId)
        val behandling = behandlingService.hentBehandling(behandlingId)
        val vurdering = vurderingService.hentVurdering(behandlingId)

        val klagerIdent = fagsak.hentAktivIdent()


        return OversendtKlageAnkeV3(
            type = Type.KLAGE,
            klager = OversendtKlager(
                id = OversendtPartId(
                    type = OversendtPartIdType.PERSON,
                    verdi = klagerIdent
                )
            ),
            fagsak = OversendtSak(fagsakId = fagsak.eksternId, fagsystem = fagsak.fagsystem.tilKildeFagsystem()),
            kildeReferanse = behandling.eksternBehandlingId,
//            innsynUrl = null, TODO
            hjemler = listOf(vurdering?.hjemmel.tilKabalHjemmel()),
            forrigeBehandlendeEnhet = "",
            tilknyttedeJournalposter = listOf(),
            brukersHenvendelseMottattNavDato =,
            innsendtTilNav =,
            kilde =,
            ytelse =,
            kommentar = null
        )
    }


    fun lagOversendtKlageAnkeV3Mock(behandlingId: UUID, fagsakId: UUID): OversendtKlageAnkeV3 {

        val klager = lagKlagerMock(fagsakId)
        val fagsak = lagOversendtSakMock(behandlingId)
        val vurdering = vurderingService.hentEksisterendeVurdering(behandlingId)

        return OversendtKlageAnkeV3(
            type = Type.KLAGE,
            klager = klager,
            fagsak = fagsak,
            kildeReferanse = "kildereferansen kommer",
            dvhReferanse = "dvhReferanse",
            innsynUrl = "https://familie-klage.dev.intern.nav.no/behandling/$behandlingId",
            forrigeBehandlendeEnhet = "forrige behandlende enhet",
            brukersHenvendelseMottattNavDato = LocalDate.now(),
            innsendtTilNav = LocalDate.now(),
            kilde = KildeFagsystem.EF,
            ytelse = Ytelse.ENF,
            kommentar = vurdering.beskrivelse,
            hjemler = listOf(
                KabalHjemmel(
                    id = LovKilde.NAV_LOVEN.id,
                    lovKilde = LovKilde.NAV_LOVEN,
                    spesifikasjon = LovKilde.NAV_LOVEN.beskrivelse
                )
            )
        )
    }

    fun lagKlagerMock(fagsakId: UUID): OversendtKlager {

        val fnr = fagsakService.hentFagsak(fagsakId).hentAktivIdent()

        val oversendtPartIdType = OversendtPartIdType.PERSON
        val oversendtPartId = OversendtPartId(oversendtPartIdType, fnr)

        return OversendtKlager(oversendtPartId, null)
    }

    fun lagOversendtSakMock(fagsakId: UUID): OversendtSak {
        return OversendtSak(
            fagsakId = fagsakId.toString(),
            fagsystem = KildeFagsystem.EF
        )
    }
}
