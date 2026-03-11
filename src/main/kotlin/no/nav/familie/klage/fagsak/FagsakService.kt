package no.nav.familie.klage.fagsak

import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.fagsak.domain.FagsakDomain
import no.nav.familie.klage.fagsak.domain.FagsakPerson
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.infrastruktur.repository.findByIdOrThrow
import no.nav.familie.klage.institusjon.Institusjon
import no.nav.familie.klage.institusjon.InstitusjonService
import no.nav.familie.klage.personopplysninger.pdl.PdlClient
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FagsakService(
    private val fagsakRepository: FagsakRepository,
    private val fagsakPersonService: FagsakPersonService,
    private val pdlClient: PdlClient,
    private val institusjonService: InstitusjonService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun hentEllerOpprettFagsak(
        fagsakEierIdent: String,
        søkerIdent: String,
        orgNummer: String? = null,
        eksternId: String,
        fagsystem: Fagsystem,
        stønadstype: Stønadstype,
    ): Fagsak {
        val fagsakEier = hentEllerOpprettPersonOgOppdaterIdenter(fagsakEierIdent, stønadstype)
        val søker = if (fagsakEierIdent == søkerIdent) fagsakEier else hentEllerOpprettPersonOgOppdaterIdenter(søkerIdent, stønadstype)
        val institusjon = orgNummer?.let { institusjonService.hentEllerLagreInstitusjon(orgNummer) }

        val fagsakDomain = fagsakRepository.findByEksternIdAndFagsystemAndStønadstype(eksternId, fagsystem, stønadstype)
        if (fagsakDomain != null && fagsakDomain.institusjonId == null && orgNummer != null) {
            logger.error("Fagsak med eksternId=$eksternId finnes allerede, men ikke med institusjon=$orgNummer.")
            throw Feil("Fagsak med eksternId=$eksternId finnes allerede, men ikke med institusjon=$orgNummer.")
        }

        val fagsak = fagsakDomain ?: opprettFagsak(stønadstype, eksternId, fagsystem, fagsakEier, søker, institusjon)

        return fagsak.tilFagsakMedPersonOgInstitusjon(fagsakEier.identer, søker.identer, institusjon)
    }

    private fun hentEllerOpprettPersonOgOppdaterIdenter(
        ident: String,
        stønadstype: Stønadstype,
    ): FagsakPerson {
        val personIdenter = pdlClient.hentPersonidenter(ident, stønadstype, true)
        val gjeldendePersonIdent = personIdenter.gjeldende()
        val person = fagsakPersonService.hentEllerOpprettPerson(personIdenter.identer(), gjeldendePersonIdent.ident)
        val oppdatertPerson = fagsakPersonService.oppdaterIdent(person, gjeldendePersonIdent.ident)
        return oppdatertPerson
    }

    fun hentFagsak(id: UUID): Fagsak = tilFagsakMedIdenterOgInstitusjon(fagsakRepository.findByIdOrThrow(id))

    fun hentFagsakForBehandling(behandlingId: UUID): Fagsak =
        fagsakRepository.finnFagsakForBehandlingId(behandlingId)?.let { tilFagsakMedIdenterOgInstitusjon(it) }
            ?: throw Feil("Finner ikke fagsak til behandlingId=$behandlingId")

    fun hentFagsakForEksternIdOgFagsystem(
        eksternId: String,
        fagsystem: Fagsystem,
        stønadstype: Stønadstype? = null,
    ): Fagsak? {
        val stønadstype =
            stønadstype ?: when (fagsystem) {
                Fagsystem.KS -> Stønadstype.KONTANTSTØTTE
                Fagsystem.BA -> Stønadstype.BARNETRYGD
                else -> throw Feil("Stønadstype må spesifiseres for fagsystem $fagsystem")
            }
        return fagsakRepository
            .findByEksternIdAndFagsystemAndStønadstype(eksternId, fagsystem, stønadstype)
            ?.let { tilFagsakMedIdenterOgInstitusjon(it) }
    }

    private fun tilFagsakMedIdenterOgInstitusjon(fagsak: FagsakDomain): Fagsak =
        fagsak.tilFagsakMedPersonOgInstitusjon(
            fagsakEierIdenter = fagsakPersonService.hentIdenter(fagsak.fagsakEierPersonId),
            søkerIdenter = fagsakPersonService.hentIdenter(fagsak.søkerPersonId),
            institusjon = fagsak.institusjonId?.let { institusjonService.finnInstitusjon(it) },
        )

    private fun opprettFagsak(
        stønadstype: Stønadstype,
        eksternId: String,
        fagsystem: Fagsystem,
        fagsakEier: FagsakPerson,
        søker: FagsakPerson,
        institusjon: Institusjon?,
    ): FagsakDomain =
        fagsakRepository.insert(
            FagsakDomain(
                fagsakEierPersonId = fagsakEier.id,
                søkerPersonId = søker.id,
                institusjonId = institusjon?.id,
                stønadstype = stønadstype,
                eksternId = eksternId,
                fagsystem = fagsystem,
            ),
        )
}
