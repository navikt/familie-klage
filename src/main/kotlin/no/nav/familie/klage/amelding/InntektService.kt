package no.nav.familie.ef.sak.amelding

import no.nav.familie.ef.sak.amelding.ekstern.AMeldingInntektClient
import no.nav.familie.klage.fagsak.FagsakService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class InntektService(
    private val aMeldingInntektClient: AMeldingInntektClient,
    private val fagsakService: FagsakService
) {

    fun genererAInntektUrlFagsak(fagsakId: UUID): String {
        val fagsak = fagsakService.hentFagsak(fagsakId)
        return aMeldingInntektClient.genererAInntektUrl(fagsak.hentAktivIdent())
    }
}
