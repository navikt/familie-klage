package no.nav.familie.klage.kabal

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

){
    fun sendTilKabal(behandlingId: UUID, fagsakId: UUID){
        val oversendtKlageAnkeV3 = lagOversendtKlageAnkeV3Mock(behandlingId, fagsakId)
        kabalClient.sendTilKabal(oversendtKlageAnkeV3)
    }
    fun lagOversendtKlageAnkeV3Mock(behandlingId: UUID, fagsakId: UUID): OversendtKlageAnkeV3 {

        val klager = lagKlagerMock(fagsakId)
        val fagsak = lagOversendtSakMock(behandlingId)
        val vurdering = vurderingService.hentVurdering(behandlingId)

        return OversendtKlageAnkeV3(
            type = Type.KLAGE,
            klager = klager,
            fagsak = fagsak,
            kildeReferanse = "kildereferansen kommer",
            dvhReferanse = "dvhReferanse",
            innsynUrl = "https://familie-klage.dev.intern.nav.no/behandling/${behandlingId}",
            forrigeBehandlendeEnhet = "forrige behandlende enhet",
            brukersHenvendelseMottattNavDato = LocalDate.now(),
            innsendtTilNav = LocalDate.now(),
            kilde = KildeFagsystem.EF,
            ytelse = Ytelse.ENF,
            kommentar = vurdering.beskrivelse,
            hjemler = listOf(KabalHjemmel(
                id = LovKilde.NAV_LOVEN.id,
                lovKilde = LovKilde.NAV_LOVEN,
                spesifikasjon = LovKilde.NAV_LOVEN.beskrivelse))
        )
    }

    fun lagKlagerMock(fagsakId: UUID): OversendtKlager{

        val fnr = fagsakService.hentFagsak(fagsakId).person_id

        val oversendtPartIdType = OversendtPartIdType.PERSON
        val oversendtPartId = OversendtPartId(oversendtPartIdType, fnr)

        return OversendtKlager(oversendtPartId, null)
    }

    fun lagOversendtSakMock(fagsakId: UUID): OversendtSak{
        return OversendtSak(
            fagsakId = fagsakId.toString(),
            fagsystem = KildeFagsystem.EF)
    }
}