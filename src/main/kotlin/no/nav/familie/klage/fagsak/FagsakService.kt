package no.nav.familie.klage.fagsak

import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.fagsak.domain.FagsakDomain
import no.nav.familie.klage.fagsak.domain.FagsakPerson
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.klage.infrastruktur.featuretoggle.Toggle
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
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun hentEllerOpprettFagsak(
        ident: String,
        orgNummer: String? = null,
        eksternId: String,
        fagsystem: Fagsystem,
        stønadstype: Stønadstype,
    ): Fagsak {
        val personIdenter = pdlClient.hentPersonidenter(ident, stønadstype, true)
        val gjeldendePersonIdent = personIdenter.gjeldende()
        val person = fagsakPersonService.hentEllerOpprettPerson(personIdenter.identer(), gjeldendePersonIdent.ident)
        val oppdatertPerson = fagsakPersonService.oppdaterIdent(person, gjeldendePersonIdent.ident)

        val institusjon =
            if (orgNummer != null && featureToggleService.isEnabled(Toggle.SKAL_KUNNE_BEHANDLE_BA_INSTITUSJON_FAGSAKER)) {
                institusjonService.hentEllerLagreInstitusjon(orgNummer)
            } else {
                null
            }

        val fagsakDomain = fagsakRepository.findByEksternIdAndFagsystemAndStønadstype(eksternId, fagsystem, stønadstype)
        if (fagsakDomain != null && fagsakDomain.institusjonId == null && orgNummer != null) {
            logger.error("Fagsak med eksternId=$eksternId finnes allerede, men ikke med institusjon=$orgNummer.")
            throw Feil("Fagsak med eksternId=$eksternId finnes allerede, men ikke med institusjon=$orgNummer.")
        }
        val fagsak = fagsakDomain ?: opprettFagsak(stønadstype, eksternId, fagsystem, oppdatertPerson, institusjon)

        return fagsak.tilFagsakMedPersonOgInstitusjon(oppdatertPerson.identer, institusjon)
    }

    fun hentFagsak(id: UUID): Fagsak {
        val fagsak = fagsakRepository.findByIdOrThrow(id)
        return fagsak.tilFagsakMedPersonOgInstitusjon(
            identer = fagsakPersonService.hentIdenter(fagsak.fagsakPersonId),
            institusjon = fagsak.institusjonId?.let { institusjonService.finnInstitusjon(it) },
        )
    }

    fun hentFagsakForBehandling(behandlingId: UUID): Fagsak {
        val fagsak = fagsakRepository.finnFagsakForBehandlingId(behandlingId)
        return fagsak?.tilFagsakMedPersonOgInstitusjon(
            identer = fagsakPersonService.hentIdenter(fagsak.fagsakPersonId),
            institusjon = fagsak.institusjonId?.let { institusjonService.finnInstitusjon(it) },
        ) ?: throw Feil("Finner ikke fagsak til behandlingId=$behandlingId")
    }

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
        val fagsak = fagsakRepository.findByEksternIdAndFagsystemAndStønadstype(eksternId, fagsystem, stønadstype)
        return fagsak?.tilFagsakMedPersonOgInstitusjon(
            identer = fagsakPersonService.hentIdenter(fagsak.fagsakPersonId),
            institusjon = fagsak.institusjonId?.let { institusjonService.finnInstitusjon(it) },
        )
    }

    private fun opprettFagsak(
        stønadstype: Stønadstype,
        eksternId: String,
        fagsystem: Fagsystem,
        fagsakPerson: FagsakPerson,
        institusjon: Institusjon?,
    ): FagsakDomain =
        fagsakRepository.insert(
            FagsakDomain(
                fagsakPersonId = fagsakPerson.id,
                institusjonId = institusjon?.id,
                stønadstype = stønadstype,
                eksternId = eksternId,
                fagsystem = fagsystem,
            ),
        )
}
