package no.nav.familie.klage.personopplysninger

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.personopplysninger.dto.Adressebeskyttelse
import no.nav.familie.klage.personopplysninger.dto.Folkeregisterpersonstatus
import no.nav.familie.klage.personopplysninger.dto.FullmaktDto
import no.nav.familie.klage.personopplysninger.dto.Kjønn
import no.nav.familie.klage.personopplysninger.dto.PersonopplysningerDto
import no.nav.familie.klage.personopplysninger.dto.VergemålDto
import no.nav.familie.klage.personopplysninger.pdl.PdlClient
import no.nav.familie.klage.personopplysninger.pdl.PdlSøker
import no.nav.familie.klage.personopplysninger.pdl.gjeldende
import no.nav.familie.klage.personopplysninger.pdl.gjelende
import no.nav.familie.klage.personopplysninger.pdl.visningsnavn
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PersonopplysningerService(
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val pdlClient: PdlClient,
    private val integrasjonerClient: PersonopplysningerIntegrasjonerClient
) {
    @Cacheable("hentPersonopplysninger", cacheManager = "shortCache")
    fun hentPersonopplysninger(behandlingId: UUID): PersonopplysningerDto {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)

        val egenAnsatt = integrasjonerClient.egenAnsatt(fagsak.hentAktivIdent())

        val pdlSøker = pdlClient.hentPerson(fagsak.hentAktivIdent())
        val andreParterNavn = hentNavnAndreParter(pdlSøker)
        return PersonopplysningerDto(
            personIdent = fagsak.hentAktivIdent(),
            navn = pdlSøker.navn.gjeldende().visningsnavn(),
            kjønn = Kjønn.valueOf(pdlSøker.kjønn.gjelende().kjønn.name),
            adressebeskyttelse = pdlSøker.adressebeskyttelse.gjeldende()?.let { Adressebeskyttelse.valueOf(it.gradering.name) },
            folkeregisterpersonstatus = pdlSøker.folkeregisterpersonstatus.gjeldende()
                ?.let { Folkeregisterpersonstatus.fraPdl(it) },
            dødsdato = pdlSøker.dødsfall.gjeldende()?.dødsdato,
            fullmakt = mapFullmakt(pdlSøker, andreParterNavn),
            egenAnsatt = egenAnsatt,
            vergemål = mapVergemål(pdlSøker)
        )
    }

    /**
     * Returnerer map med ident og visningsnavn
     */
    private fun hentNavnAndreParter(pdlSøker: PdlSøker): Map<String, String> {
        return pdlSøker.fullmakt.map { it.motpartsPersonident }.distinct()
            .takeIf { it.isNotEmpty() }
            ?.let { hentNavn(it) }
            ?: emptyMap()
    }

    private fun hentNavn(it: List<String>): Map<String, String> =
        pdlClient.hentNavnBolk(it).map { it.key to it.value.navn.gjeldende().visningsnavn() }.toMap()

    private fun mapFullmakt(pdlSøker: PdlSøker, andreParterNavn: Map<String, String>) = pdlSøker.fullmakt.map {
        FullmaktDto(
            gyldigFraOgMed = it.gyldigFraOgMed,
            gyldigTilOgMed = it.gyldigTilOgMed,
            motpartsPersonident = it.motpartsPersonident,
            navn = andreParterNavn[it.motpartsPersonident] ?: error("Finner ikke navn til ${it.motpartsPersonident}"),
            områder = it.omraader.map { område -> mapOmråde(område) }
        )
    }.sortedByDescending(FullmaktDto::gyldigFraOgMed)

    private fun mapVergemål(søker: PdlSøker) =
        søker.vergemaalEllerFremtidsfullmakt.filter { it.type != "stadfestetFremtidsfullmakt" }.map {
            VergemålDto(
                embete = it.embete,
                type = it.type,
                motpartsPersonident = it.vergeEllerFullmektig.motpartsPersonident,
                navn = it.vergeEllerFullmektig.navn?.visningsnavn(),
                omfang = it.vergeEllerFullmektig.omfang
            )
        }

    private fun mapOmråde(område: String): String {
        return when (område) {
            "*" -> "ALLE"
            else -> område
        }
    }
}
