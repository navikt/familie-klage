package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.BehandlingStatus
import no.nav.familie.klage.behandling.domain.BehandlingSteg
import no.nav.familie.klage.behandling.domain.BehandlingsÅrsak
import no.nav.familie.klage.behandling.domain.Fagsystem
import no.nav.familie.klage.behandling.domain.StønadsType
import no.nav.familie.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.klage.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.familie.klage.behandlingshistorikk.domain.Steg
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import no.nav.familie.klage.personopplysninger.domain.Personopplysninger
import no.nav.familie.klage.personopplysninger.domain.Kjønn
import no.nav.familie.klage.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class BehandlingService(
        private val behandlingsRepository: BehandlingsRepository,
        private val behandlingshistorikkService: BehandlingshistorikkService,
        private val personopplysningerService: PersonopplysningerService,
    ) {

    fun hentBehandling(behandlingId: UUID): Behandling = behandlingsRepository.findByIdOrThrow(behandlingId)

    fun hentNavnFraBehandlingsId(behandlingId: UUID): String = behandlingsRepository.findNavnByBehandlingId(behandlingId)

    fun opprettBehandling(): Behandling {
        val fagsakId = UUID.randomUUID()
        val fødselsnummer = (0..999999999).random().toString() // TODO legge inn faktisk fødselsnummber

        personopplysningerService.opprettPersonopplysninger(
            personopplysninger = Personopplysninger(
                personId = fødselsnummer,
                navn = "Juni",
                kjønn = Kjønn.KVINNE,
                adresse = "Korsgata 21A",
                telefonnummer = "46840856"
            )
        )

        val behandling = behandlingsRepository.insert(
            Behandling(
                fagsakId = fagsakId,
                personId = fødselsnummer,
                steg = BehandlingSteg.FORMALKRAV,
                status = BehandlingStatus.OPPRETTET,
                endretTid = LocalDateTime.now(),
                opprettetTid = LocalDateTime.now(),
                fagsystem = Fagsystem.EF,
                stonadsType = StønadsType.BARNETILSYN,
                behandlingsArsak = BehandlingsÅrsak.KLAGE
            )
        )

        behandlingshistorikkService.opprettBehandlingshistorikk(
            behandlingshistorikk = Behandlingshistorikk(
                behandlingId = behandling.id,
                steg = Steg.OPPRETTET,
                opprettetAv = "Juni Leirvik"
            )
        )

        /*val navn = Navn(
            fornavn = "Juni",
            mellomnavn = "Leirvik",
            etternavn = "Larsen",
            visningsnavn = "Juni Leirvik"
        )*/

        /*val telefon = Telefonnummer(
            landskode = "+47",
            nummer = "46840856"
        )*/

        return behandling
    }
}