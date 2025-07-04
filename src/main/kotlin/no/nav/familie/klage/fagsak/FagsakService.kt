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
    fun hentEllerOpprettFagsak(
        ident: String,
        eksternId: String,
        fagsystem: Fagsystem,
        stønadstype: Stønadstype,
    ): Fagsak {
        val personIdenter = pdlClient.hentPersonidenter(ident, stønadstype, true)
        val gjeldendePersonIdent = personIdenter.gjeldende()
        val person = fagsakPersonService.hentEllerOpprettPerson(personIdenter.identer(), gjeldendePersonIdent.ident)
        val oppdatertPerson = fagsakPersonService.oppdaterIdent(person, gjeldendePersonIdent.ident)
        val fagsak =
            fagsakRepository.findByEksternIdAndFagsystemAndStønadstype(eksternId, fagsystem, stønadstype)
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

    fun hentFagsakForEksternIdOgFagsystem(
        eksternId: String,
        fagsystem: Fagsystem,
        stønadstype: Stønadstype? = null,
    ): Fagsak {
        val stønadstype =
            stønadstype ?: when (fagsystem) {
                Fagsystem.KS -> Stønadstype.KONTANTSTØTTE
                Fagsystem.BA -> Stønadstype.BARNETRYGD
                else -> throw Feil("Stønadstype må spesifiseres for fagsystem $fagsystem")
            }
        val fagsak = fagsakRepository.findByEksternIdAndFagsystemAndStønadstype(eksternId, fagsystem, stønadstype)
        return fagsak?.tilFagsakMedPerson(fagsakPersonService.hentIdenter(fagsak.fagsakPersonId))
            ?: throw Feil("Finner ikke fagsak for eksternId=$eksternId, fagsystem=$fagsystem og stønadstype=$stønadstype")
    }

    private fun opprettFagsak(
        stønadstype: Stønadstype,
        eksternId: String,
        fagsystem: Fagsystem,
        fagsakPerson: FagsakPerson,
    ): FagsakDomain =
        fagsakRepository.insert(
            FagsakDomain(
                fagsakPersonId = fagsakPerson.id,
                stønadstype = stønadstype,
                eksternId = eksternId,
                fagsystem = fagsystem,
            ),
        )
}
