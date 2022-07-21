package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.BehandlingStatus
import no.nav.familie.klage.behandling.domain.BehandlingSteg
import no.nav.familie.klage.behandling.domain.BehandlingsÅrsak
import no.nav.familie.klage.behandling.domain.StønadsType
import no.nav.familie.klage.behandling.dto.BehandlingDto
import no.nav.familie.klage.behandling.dto.tilDto
import no.nav.familie.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.klage.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.familie.klage.behandlingshistorikk.domain.Steg
import no.nav.familie.klage.brev.AvsnittRepository
import no.nav.familie.klage.brev.BrevRepository
import no.nav.familie.klage.brev.FamilieDokumentClient
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.integrasjoner.FamilieIntegrasjonerClient
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import no.nav.familie.klage.personopplysninger.domain.Personopplysninger
import no.nav.familie.klage.personopplysninger.domain.Kjønn
import no.nav.familie.klage.repository.findByIdOrThrow
import no.nav.familie.kontrakter.ef.søknad.SøknadType
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import no.nav.familie.kontrakter.felles.dokdist.Distribusjonstype
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BehandlingService(
        private val behandlingsRepository: BehandlingsRepository,
        private val behandlingshistorikkService: BehandlingshistorikkService,
        private val personopplysningerService: PersonopplysningerService,
        private val fagsakService: FagsakService,
        private val brevRepository: BrevRepository,
        private val avsnittRepository: AvsnittRepository,
        private val familieDokumentClient: FamilieDokumentClient,
        private val familieIntegrasjonerClient: FamilieIntegrasjonerClient,
    ){

    val logger: Logger = LoggerFactory.getLogger(this::class.java)
    fun hentBehandling(behandlingId: UUID): BehandlingDto {
        val behandling = behandlingsRepository.findByIdOrThrow(behandlingId)
        return behandling.tilDto()
    }

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

        fagsakService.opprettFagsak(
            fagsak = Fagsak(
                id = fagsakId,
                person_id =fødselsnummer,
                søknadsType = SøknadType.BARNETILSYN
            )
        )

        val behandling = behandlingsRepository.insert(
            Behandling(
                fagsakId = fagsakId,
                personId = fødselsnummer,
                steg = BehandlingSteg.FORMALKRAV,
                status = BehandlingStatus.OPPRETTET,
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

    fun ferdigstillBrev(behandlingId: UUID){
        //journalføre og distribuere brev

        val brev = brevRepository.findByIdOrThrow(behandlingId)
        val avsnitt = avsnittRepository.hentAvsnittPåBehandlingId(behandlingId)
        val behandling = behandlingsRepository.findByIdOrThrow(behandlingId)
        val pdf = familieDokumentClient.genererPdfFraHtml(brev.saksbehandlerHtml)

        val arkiverDokumentRequest = lagArkiverDokumentRequest(
            personIdent = behandling.personId,
            pdf = pdf,
            fagsakId = behandling.fagsakId.toString(),
            behandlingId = behandlingId,
            enhet = "enhet",
            stønadstype = behandling.stonadsType,
            dokumenttype = Dokumenttype.BARNETRYGD_VEDTAK_INNVILGELSE,
        )
        logger.info("opprettet arkiverDokumentRequest")

        // arkiverer brev
        val respons = familieIntegrasjonerClient.arkiverDokument(arkiverDokumentRequest, "Maja") //TODO: Hente en saksbehandlere her

        logger.info("arkiver brev respons: $respons")

        val distnummer = familieIntegrasjonerClient.distribuerBrev(
            respons.journalpostId,
            Distribusjonstype.ANNET)

        logger.info("distribuer brev respons: $distnummer")


    }
    private fun lagArkiverDokumentRequest(
        personIdent: String,
        pdf: ByteArray,
        fagsakId: String?,
        behandlingId: UUID,
        enhet: String,
        stønadstype: StønadsType,
        dokumenttype: Dokumenttype
    ): ArkiverDokumentRequest {
        val dokument = no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument(
            pdf,
            Filtype.PDFA,
            null,
            "Brev for ${stønadstype.name.lowercase()}",
            dokumenttype
        )
        return ArkiverDokumentRequest(
            fnr = personIdent,
            forsøkFerdigstill = true,
            hoveddokumentvarianter = listOf(dokument),
            vedleggsdokumenter = listOf(),
            fagsakId = fagsakId,
            journalførendeEnhet = enhet,
            eksternReferanseId = "$behandlingId-blankett"
        )
    }
}