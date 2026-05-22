package no.nav.familie.klage.søk

import no.nav.familie.felles.tokenklient.entraid.EntraIDClient
import no.nav.familie.felles.tokenklient.entraid.EntraIDRestClientFactory
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.felles.domain.AuditLoggerEvent
import no.nav.familie.klage.infrastruktur.exception.ApiFeil
import no.nav.familie.klage.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import no.nav.familie.klage.personopplysninger.pdl.PdlClient
import no.nav.familie.klage.personopplysninger.pdl.gjeldende
import no.nav.familie.klage.personopplysninger.pdl.visningsnavn
import no.nav.familie.klage.søk.dto.PersonIdentDto
import no.nav.familie.klage.søk.dto.PersonTreffDto
import no.nav.familie.klage.søk.ereg.EregService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import no.nav.familie.sikkerhet.EksternBrukerUtils
import no.nav.familie.sikkerhet.context.SpringSecurityTokenContext
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/sok"])
@Validated
class SøkController(
    private val tilgangService: TilgangService,
    private val pdlClient: PdlClient,
    private val eregService: EregService,
    private val fagsakService: FagsakService,
    private val entraIDClient: EntraIDClient,
) {
    @PostMapping("person")
    fun søkPerson(
        @RequestBody personIdentDto: PersonIdentDto,
    ): Ressurs<PersonTreffDto> {
        val personIdent = personIdentDto.personIdent
        val behandlingId = personIdentDto.behandlingId
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        tilgangService.validerTilgangTilPersonMedRelasjoner(personIdent, AuditLoggerEvent.UPDATE)
        val person = pdlClient.hentPerson(personIdent, fagsak.stønadstype)
        val result = PersonTreffDto(personIdent, person.navn.gjeldende().visningsnavn())
        return Ressurs.success(result)
    }

    @GetMapping("organisasjon/{organisasjonsnummer}")
    fun søkOrganisasjon(
        @PathVariable organisasjonsnummer: String,
    ): Ressurs<Organisasjon> {
        if (!ORGNR_REGEX.matches(organisasjonsnummer)) {
            throw ApiFeil("Ugyldig organisasjonsnummer", HttpStatus.BAD_REQUEST)
        }
        return Ressurs.success(eregService.hentOrganisasjon(organisasjonsnummer))
    }

    @GetMapping("obo")
    fun getObo(): String =
        entraIDClient.hentOboToken(
            "api://dev-fss.teamfamilie.familie-integrasjoner/.default",
            EksternBrukerUtils.getBearerTokenForLoggedInUser(),
        )

    @GetMapping("m2m")
    fun getM2M(): String =
        entraIDClient.hentMaskinTilMaskinToken(
            "api://dev-fss.teamfamilie.familie-integrasjoner/.default",
        )

    companion object {
        private val ORGNR_REGEX = """\d{9}""".toRegex()
    }
}
