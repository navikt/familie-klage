package no.nav.familie.klage.personopplysninger

import no.nav.familie.klage.behandling.BehandlingsRepository
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.personopplysninger.domain.Kjønn
import no.nav.familie.klage.personopplysninger.domain.PersonopplysningerDto
import no.nav.familie.klage.personopplysninger.pdl.PdlClient
import no.nav.familie.klage.personopplysninger.pdl.gjeldende
import no.nav.familie.klage.personopplysninger.pdl.gjelende
import no.nav.familie.klage.personopplysninger.pdl.visningsnavn
import no.nav.familie.klage.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PersonopplysningerService(
    private val behandlingsRepository: BehandlingsRepository,
    private val fagsakService: FagsakService,
    private val pdlClient: PdlClient
) {

    fun hentPersonopplysninger(behandlingId: UUID): PersonopplysningerDto {
        val behandling = behandlingsRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)

        return pdlClient.hentPerson(fagsak.hentAktivIdent()).let {
            PersonopplysningerDto(
                personIdent = fagsak.hentAktivIdent(),
                navn = it.navn.gjeldende().visningsnavn(),
                kjønn = Kjønn.valueOf(it.kjønn.gjelende().kjønn.toString()),
                telefonnummer = it.telefonnummer.single().nummer,
                adresse = "" // TODO

            )
        }
    }
}
