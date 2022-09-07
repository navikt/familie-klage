package no.nav.familie.klage.fagsak

import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.fagsak.domain.FagsakDomain
import no.nav.familie.klage.fagsak.domain.FagsakPerson
import no.nav.familie.klage.fagsak.domain.Stønadstype
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.personopplysninger.pdl.PdlClient
import no.nav.familie.klage.repository.findByIdOrThrow
import no.nav.familie.kontrakter.felles.Fagsystem
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FagsakService(
    private val fagsakRepository: FagsakRepository,
    private val fagsakPersonService: FagsakPersonService,
    private val pdlClient: PdlClient,
) {

    @Transactional
    fun hentEllerOpprettFagsak(ident: String, eksternId: String, fagsystem: Fagsystem, stønadType: Stønadstype): Fagsak {
        val personIdenter = pdlClient.hentPersonidenter(ident, true)
        val gjeldendePersonIdent = personIdenter.gjeldende()
        val person = fagsakPersonService.hentEllerOpprettPerson(personIdenter.identer(), gjeldendePersonIdent.ident)
        val oppdatertPerson = fagsakPersonService.oppdaterIdent(person, gjeldendePersonIdent.ident)
        val fagsak = fagsakRepository.findByEksternIdAndFagsystemAndStønadstype(eksternId, fagsystem, stønadType)
            ?: opprettFagsak(stønadType, eksternId, fagsystem, oppdatertPerson)

        return fagsak.tilFagsakMedPerson(oppdatertPerson.identer)
    }

    fun hentFagsak(id: UUID): Fagsak {
        val fagsak = fagsakRepository.findByIdOrThrow(id)
        return fagsak.tilFagsakMedPerson(fagsakPersonService.hentIdenter(fagsak.fagsakPersonId))
    }

    fun hentFagsakForBehandling(behandlingId: UUID): Fagsak {
        val fagsak = fagsakRepository.finnFagsakForBehandling(behandlingId)
        return fagsak?.tilFagsakMedPerson(fagsakPersonService.hentIdenter(fagsak.fagsakPersonId))
            ?: throw Feil("Finner ikke fagsak til behandlingId=$behandlingId")
    }

    private fun opprettFagsak(
        stønadType: Stønadstype,
        eksternId: String,
        fagsystem: Fagsystem,
        fagsakPerson: FagsakPerson
    ): FagsakDomain {
        return fagsakRepository.insert(
            FagsakDomain(
                fagsakPersonId = fagsakPerson.id,
                stønadstype = stønadType,
                eksternId = eksternId,
                fagsystem = fagsystem
            )
        )
    }
}
