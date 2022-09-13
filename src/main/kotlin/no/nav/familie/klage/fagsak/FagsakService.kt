package no.nav.familie.klage.fagsak

import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.fagsak.domain.FagsakDomain
import no.nav.familie.klage.fagsak.domain.FagsakPerson
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.personopplysninger.pdl.PdlClient
import no.nav.familie.klage.repository.findByIdOrThrow
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.Stønadstype
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
    fun hentEllerOpprettFagsak(ident: String, eksternId: String, fagsystem: Fagsystem, stønadstype: Stønadstype): Fagsak {
        val personIdenter = pdlClient.hentPersonidenter(ident, true)
        val gjeldendePersonIdent = personIdenter.gjeldende()
        val person = fagsakPersonService.hentEllerOpprettPerson(personIdenter.identer(), gjeldendePersonIdent.ident)
        val oppdatertPerson = fagsakPersonService.oppdaterIdent(person, gjeldendePersonIdent.ident)
        val fagsak = fagsakRepository.findByEksternIdAndFagsystemAndStønadstype(eksternId, fagsystem, stønadstype)
            ?: opprettFagsak(stønadstype, eksternId, fagsystem, oppdatertPerson)

        return fagsak.tilFagsakMedPerson(oppdatertPerson.identer)
    }

    fun hentFagsak(id: UUID): Fagsak {
        val fagsak = fagsakRepository.findByIdOrThrow(id)
        return fagsak.tilFagsakMedPerson(fagsakPersonService.hentIdenter(fagsak.fagsakPersonId))
    }

    fun hentFagsakForBehandling(behandlingId: UUID): Fagsak {
        val fagsak = fagsakRepository.finnFagsakForBehandlingId(behandlingId)
        return fagsak?.tilFagsakMedPerson(fagsakPersonService.hentIdenter(fagsak.fagsakPersonId))
            ?: throw Feil("Finner ikke fagsak til behandlingId=$behandlingId")
    }

    private fun opprettFagsak(
        stønadstype: Stønadstype,
        eksternId: String,
        fagsystem: Fagsystem,
        fagsakPerson: FagsakPerson
    ): FagsakDomain {
        return fagsakRepository.insert(
            FagsakDomain(
                fagsakPersonId = fagsakPerson.id,
                stønadstype = stønadstype,
                eksternId = eksternId,
                fagsystem = fagsystem
            )
        )
    }
}
