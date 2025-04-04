package no.nav.familie.klage.brevmottaker

import jakarta.transaction.Transactional
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandling.domain.erLåstForVidereBehandling
import no.nav.familie.klage.brev.BrevRepository
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonMedIdent
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonUtenIdent
import no.nav.familie.klage.brevmottaker.domain.Brevmottakere
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.repository.findByIdOrThrow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class BrevmottakerErstatter(
    private val behandlingService: BehandlingService,
    private val brevRepository: BrevRepository,
) {
    private val logger = LoggerFactory.getLogger(BrevmottakerErstatter::class.java)

    @Transactional
    fun erstattBrevmottakere(behandlingId: UUID, brevmottakere: Brevmottakere): Brevmottakere {
        logger.debug("Erstatter brevmottakere for behandling {}.", behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)
        validerMinimumEnMottaker(behandling, brevmottakere)
        validerRedigerbarBehandling(behandling)
        validerKorrektBehandlingssteg(behandling)
        validerUnikeBrevmottakere(behandling, brevmottakere)
        val eksisterendeBrev = brevRepository.findByIdOrThrow(behandlingId)
        val oppdatertBrev = brevRepository.update(eksisterendeBrev.copy(mottakere = brevmottakere))
        return oppdatertBrev.mottakere ?: error("Fant ikke brevmottakere for behandling $behandlingId.")
    }

    private fun validerMinimumEnMottaker(behandling: Behandling, brevmottakere: Brevmottakere) {
        if (brevmottakere.personer.isEmpty() && brevmottakere.organisasjoner.isEmpty()) {
            throw Feil("Må ha minimum en brevmottaker for behandling ${behandling.id}.")
        }
    }

    private fun validerRedigerbarBehandling(behandling: Behandling) {
        if (behandling.status.erLåstForVidereBehandling()) {
            throw Feil("Behandling ${behandling.id} er låst for videre behandling.")
        }
    }

    private fun validerKorrektBehandlingssteg(behandling: Behandling) {
        if (behandling.steg != StegType.BREV) {
            throw Feil("Behandlingen er i steg ${behandling.steg}, forventet steg ${StegType.BREV}.")
        }
    }

    private fun validerUnikeBrevmottakere(behandling: Behandling, brevmottakere: Brevmottakere) {
        val personBrevmottakerIdentifikatorer = brevmottakere.personer.map {
            when (it) {
                is BrevmottakerPersonMedIdent -> it.personIdent
                is BrevmottakerPersonUtenIdent -> it.id.toString()
            }
        }
        if (personBrevmottakerIdentifikatorer.distinct().size != personBrevmottakerIdentifikatorer.size) {
            throw Feil("En person kan bare legges til en gang som brevmottaker for behandling ${behandling.id}.")
        }

        val organisasjonBrevmottakerIdenter = brevmottakere.organisasjoner.map { it.organisasjonsnummer }
        if (organisasjonBrevmottakerIdenter.distinct().size != organisasjonBrevmottakerIdenter.size) {
            throw Feil("En organisasjon kan bare legges til en gang som brevmottaker for behandling ${behandling.id}.")
        }
    }
}
