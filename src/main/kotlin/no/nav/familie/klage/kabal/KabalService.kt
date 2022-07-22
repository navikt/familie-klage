package no.nav.familie.klage.kabal

import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class KabalService(
    private val kabalClient: KabalClient,

){
    fun sendTilKabal(){
        val oversendtKlageAnkeV3 = lagOversendtKlageAnkeV3()
        kabalClient.sendTilKabal(oversendtKlageAnkeV3)
    }
    fun lagOversendtKlageAnkeV3(): OversendtKlageAnkeV3 {

        val klager = lagKlager()
        val fagsak = lagOversendtSak()

        return OversendtKlageAnkeV3(
            type = Type.KLAGE,
            klager = klager,
            fagsak = fagsak,
            kildeReferanse = "kildereferansen kommer",
            dvhReferanse = "dvhReferanse",
            forrigeBehandlendeEnhet = "forrige behandlende enhet",
            brukersHenvendelseMottattNavDato = LocalDate.now(),
            innsendtTilNav = LocalDate.now(),
            kilde = KildeFagsystem.EF,
            ytelse = Ytelse.ENF,
        )
    }

    fun lagKlager(): OversendtKlager{
        val oversendtPartIdType = OversendtPartIdType.PERSON
        val oversendtPartId = OversendtPartId(oversendtPartIdType, "en verdi")
        return OversendtKlager(oversendtPartId, null)
    }

    fun lagOversendtSak(): OversendtSak{
        return OversendtSak(
            fagsakId = UUID.randomUUID().toString(),
            fagsystem = KildeFagsystem.EF)
    }

}