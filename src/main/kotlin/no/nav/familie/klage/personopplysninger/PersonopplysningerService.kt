package no.nav.familie.klage.personopplysninger

import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.personopplysninger.dto.Adressebeskyttelse
import no.nav.familie.klage.personopplysninger.dto.Folkeregisterpersonstatus
import no.nav.familie.klage.personopplysninger.dto.FullmaktDto
import no.nav.familie.klage.personopplysninger.dto.Kjønn
import no.nav.familie.klage.personopplysninger.dto.PersonopplysningerDto
import no.nav.familie.klage.personopplysninger.dto.VergemålDto
import no.nav.familie.klage.personopplysninger.fullmakt.FullmaktService
import no.nav.familie.klage.personopplysninger.pdl.Fullmakt
import no.nav.familie.klage.personopplysninger.pdl.PdlClient
import no.nav.familie.klage.personopplysninger.pdl.PdlPerson
import no.nav.familie.klage.personopplysninger.pdl.gjeldende
import no.nav.familie.klage.personopplysninger.pdl.visningsnavn
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class PersonopplysningerService(
    private val fagsakService: FagsakService,
    private val pdlClient: PdlClient,
    private val integrasjonerClient: PersonopplysningerIntegrasjonerClient,
    private val fullmaktService: FullmaktService,
) {
    @Cacheable("hentPersonopplysninger", cacheManager = "shortCache", key = "'fagsakEier:' + #behandlingId")
    fun hentPersonopplysningerFagsakEier(behandlingId: UUID): PersonopplysningerDto {
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        return hentPersonopplysninger(fagsak.hentFagsakEierIdent(), fagsak.stønadstype)
    }

    @Cacheable("hentPersonopplysninger", cacheManager = "shortCache", key = "'soker:' + #behandlingId")
    fun hentPersonopplysningerSøker(behandlingId: UUID): PersonopplysningerDto {
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        return hentPersonopplysninger(fagsak.hentSøkerIdent(), fagsak.stønadstype)
    }

    @Cacheable("hentPersonopplysninger", cacheManager = "shortCache")
    fun hentPersonopplysninger(
        ident: String,
        stønadstype: Stønadstype,
    ): PersonopplysningerDto {
        val egenAnsatt = integrasjonerClient.egenAnsatt(ident)
        val pdlPerson = pdlClient.hentPerson(ident, stønadstype)
        val fullmakt = fullmaktService.hentFullmakt(ident)
        return PersonopplysningerDto(
            personIdent = ident,
            navn = pdlPerson.navn.gjeldende().visningsnavn(),
            fødselsdato = pdlPerson.fødselsdato.gjeldende()?.let { it.fødselsdato ?: LocalDate.of(it.fødselsår, 1, 1) },
            kjønn =
                Kjønn.valueOf(
                    pdlPerson.kjønn
                        .gjeldende()
                        .kjønn.name,
                ),
            adressebeskyttelse = pdlPerson.adressebeskyttelse.gjeldende()?.let { Adressebeskyttelse.valueOf(it.gradering.name) },
            folkeregisterpersonstatus =
                pdlPerson.folkeregisterpersonstatus
                    .gjeldende()
                    ?.let { Folkeregisterpersonstatus.fraPdl(it) },
            dødsdato = pdlPerson.dødsfall.gjeldende()?.dødsdato,
            fullmakt = mapFullmakt(fullmakt),
            egenAnsatt = egenAnsatt,
            vergemål = mapVergemål(pdlPerson),
        )
    }

    private fun mapFullmakt(fullmakt: List<Fullmakt>) =
        fullmakt
            .map {
                FullmaktDto(
                    gyldigFraOgMed = it.gyldigFraOgMed,
                    gyldigTilOgMed = it.gyldigTilOgMed,
                    motpartsPersonident = it.motpartsPersonident,
                    navn = it.fullmektigsNavn,
                    områder = it.omraader.map { område -> mapOmråde(område) },
                )
            }.sortedByDescending(FullmaktDto::gyldigFraOgMed)

    private fun mapVergemål(søker: PdlPerson) =
        søker.vergemaalEllerFremtidsfullmakt.filter { it.type != "stadfestetFremtidsfullmakt" }.map {
            VergemålDto(
                embete = it.embete,
                type = it.type,
                motpartsPersonident = it.vergeEllerFullmektig.motpartsPersonident,
                navn = it.vergeEllerFullmektig.navn?.visningsnavn(),
                omfang = it.vergeEllerFullmektig.omfang,
            )
        }

    private fun mapOmråde(område: String): String =
        when (område) {
            "*" -> "ALLE"
            else -> område
        }
}
