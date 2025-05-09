package no.nav.familie.klage.infrastruktur.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.patch
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import no.nav.familie.klage.arbeidsfordeling.Arbeidsfordelingsenhet
import no.nav.familie.klage.felles.dto.EgenAnsattResponse
import no.nav.familie.klage.felles.dto.Tilgang
import no.nav.familie.klage.infrastruktur.config.PdfMock.PDFA_AS_BASE64_STRING
import no.nav.familie.klage.mappe.statiskDummyMapper
import no.nav.familie.kontrakter.ef.sak.DokumentBrevkode
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.OppdaterJournalpostResponse
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.DokumentInfo
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariant
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariantformat
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus
import no.nav.familie.kontrakter.felles.journalpost.LogiskVedlegg
import no.nav.familie.kontrakter.felles.journalpost.RelevantDato
import no.nav.familie.kontrakter.felles.navkontor.NavKontorEnhet
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.FinnMappeResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.StatusEnum
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.saksbehandler.Saksbehandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.UUID
import kotlin.math.absoluteValue
import kotlin.random.Random

@Component
class FamilieIntegrasjonerMock(
    integrasjonerConfig: IntegrasjonerConfig,
) {
    private val responses =
        listOf(
            get(urlEqualTo(integrasjonerConfig.pingUri.path))
                .willReturn(aResponse().withStatus(200)),
            get(urlPathMatching("${integrasjonerConfig.saksbehandlerUri.path}/([A-Za-z0-9]*)"))
                .willReturn(okJson(objectMapper.writeValueAsString(saksbehandler))),
            post(urlEqualTo(integrasjonerConfig.egenAnsattUri.path))
                .willReturn(okJson(objectMapper.writeValueAsString(egenAnsatt))),
            post(urlEqualTo(integrasjonerConfig.tilgangRelasjonerUri.path))
                .withRequestBody(matching(".*ikkeTilgang.*"))
                .atPriority(1)
                .willReturn(okJson(objectMapper.writeValueAsString(lagIkkeTilgangResponse()))),
            post(urlEqualTo(integrasjonerConfig.tilgangRelasjonerUri.path))
                .willReturn(okJson(objectMapper.writeValueAsString(Tilgang(true, null)))),
            post(urlEqualTo(integrasjonerConfig.tilgangPersonUri.path))
                .withRequestBody(matching(".*ikkeTilgang.*"))
                .atPriority(1)
                .willReturn(okJson(objectMapper.writeValueAsString(listOf(lagIkkeTilgangResponse())))),
            post(urlEqualTo(integrasjonerConfig.tilgangPersonUri.path))
                .willReturn(okJson(objectMapper.writeValueAsString(listOf(Tilgang(true, null))))),
            post(urlEqualTo(integrasjonerConfig.arbeidsfordelingUri.path))
                .willReturn(okJson(objectMapper.writeValueAsString(arbeidsfordeling))),
            post(urlEqualTo(integrasjonerConfig.arbeidsfordelingMedRelasjonerUri.path))
                .willReturn(okJson(objectMapper.writeValueAsString(arbeidsfordeling))),
            get(urlPathEqualTo(integrasjonerConfig.journalPostUri.path))
                .withQueryParam("journalpostId", equalTo("1234"))
                .willReturn(okJson(objectMapper.writeValueAsString(journalpost))),
            get(urlPathEqualTo(integrasjonerConfig.journalPostUri.path))
                .withQueryParam("journalpostId", equalTo("2345"))
                .willReturn(okJson(objectMapper.writeValueAsString(journalpostPapirsøknad))),
            post(urlPathEqualTo(integrasjonerConfig.journalPostUri.path))
                .willReturn(okJson(objectMapper.writeValueAsString(journalposter))),
            get(urlPathMatching("${integrasjonerConfig.journalPostUri.path}/hentdokument/([0-9]*)/([0-9]*)"))
                .withQueryParam("variantFormat", equalTo("ORIGINAL"))
                .willReturn(
                    okJson(
                        objectMapper.writeValueAsString(
                            Ressurs.success(
                                objectMapper.writeValueAsBytes(Testsøknad.søknadOvergangsstønad),
                            ),
                        ),
                    ),
                ),
            get(urlPathMatching("${integrasjonerConfig.journalPostUri.path}/hentdokument/([0-9]*)/([0-9]*)"))
                .withQueryParam("variantFormat", equalTo("ARKIV"))
                .willReturn(okJson(objectMapper.writeValueAsString(Ressurs.success(PDFA_AS_BASE64_STRING)))),
            get(urlPathMatching("${integrasjonerConfig.oppgaveUri.path}/([0-9]*)"))
                .willReturn(
                    okJson(
                        objectMapper.writeValueAsString(
                            Ressurs.success(
                                Oppgave(
                                    id = Random.nextLong().absoluteValue,
                                    tildeltEnhetsnr = "4489",
                                    tilordnetRessurs = "Z994152",
                                    tema = Tema.ENF,
                                    status = StatusEnum.UNDER_BEHANDLING,
                                ),
                            ),
                        ),
                    ),
                ),
            get(urlPathMatching("${integrasjonerConfig.saksbehandlerUri.path}/Z994152"))
                .willReturn(
                    okJson(
                        objectMapper.writeValueAsString(
                            Ressurs.success(
                                Saksbehandler(
                                    UUID.randomUUID(),
                                    "Z994152",
                                    "Luke",
                                    "Skywalker",
                                    "4405",
                                    "Skien",
                                ),
                            ),
                        ),
                    ),
                ),
            put(urlMatching("${integrasjonerConfig.dokarkivUri.path}.*"))
                .willReturn(okJson(objectMapper.writeValueAsString(oppdatertJournalpostResponse))),
            post(urlMatching("${integrasjonerConfig.dokarkivUri.path}.*"))
                .willReturn(okJson(objectMapper.writeValueAsString(arkiverDokumentResponse))),
            post(urlEqualTo(integrasjonerConfig.navKontorUri.path))
                .willReturn(okJson(objectMapper.writeValueAsString(navKontorEnhet))),
            post(urlEqualTo(integrasjonerConfig.adressebeskyttelse.path))
                .willReturn(
                    okJson(
                        objectMapper.writeValueAsString(
                            Ressurs.success(
                                ADRESSEBESKYTTELSEGRADERING
                                    .UGRADERT,
                            ),
                        ),
                    ),
                ),
            post(urlEqualTo(integrasjonerConfig.distribuerDokumentUri.path))
                .willReturn(
                    okJson(
                        objectMapper.writeValueAsString(
                            Ressurs.success(
                                "123",
                            ),
                        ),
                    ).withStatus(200),
                ),
            post(urlEqualTo(integrasjonerConfig.sendTilKabalUri.path))
                .willReturn(
                    okJson(
                        objectMapper.writeValueAsString(
                            Ressurs.success(
                                "123456",
                            ),
                        ),
                    ).withStatus(200),
                ),
            post(urlEqualTo("${integrasjonerConfig.oppgaveUri.path}/opprett"))
                .willReturn(okJson(objectMapper.writeValueAsString(Ressurs.success(OppgaveResponse(Random.nextLong().absoluteValue))))),
            patch(urlPathMatching("${integrasjonerConfig.oppgaveUri.path}/([0-9]*)/ferdigstill"))
                .willReturn(okJson(objectMapper.writeValueAsString(Ressurs.success(OppgaveResponse(Random.nextLong().absoluteValue))))),
            get(urlEqualTo("${integrasjonerConfig.oppgaveUri.path}/mappe/sok?enhetsnr=4489&limit=1000")).willReturn(
                okJson(
                    objectMapper.writeValueAsString(
                        Ressurs.success(
                            data =
                                FinnMappeResponseDto(
                                    antallTreffTotalt = 1,
                                    mapper = statiskDummyMapper,
                                ),
                        ),
                    ),
                ),
            ),
            patch(urlPathMatching("${integrasjonerConfig.oppgaveUri.path}/([0-9]*)/oppdater"))
                .willReturn(
                    okJson(
                        objectMapper.writeValueAsString(
                            Ressurs.success(OppgaveResponse(Random.nextLong().absoluteValue)),
                        ),
                    ),
                ),
        )

    private fun lagIkkeTilgangResponse() =
        Tilgang(
            false,
            "Mock sier: Du har " +
                "ikke tilgang " +
                "til person ikkeTilgang",
        )

    @Bean("mock-integrasjoner")
    @Profile("mock-integrasjoner")
    fun integrationMockServer(): WireMockServer {
        val mockServer = WireMockServer(8386)
        responses.forEach {
            mockServer.stubFor(it)
        }
        mockServer.start()
        return mockServer
    }

    companion object {
        private val egenAnsatt = Ressurs.success(EgenAnsattResponse(false))

        private val arbeidsfordeling =
            Ressurs.success(listOf(Arbeidsfordelingsenhet("4489", "nerd-enhet")))

        private val fnr = "23097825289"

        private val oppdatertJournalpostResponse =
            Ressurs.success(OppdaterJournalpostResponse(journalpostId = "1234"))
        private val arkiverDokumentResponse = Ressurs.success(ArkiverDokumentResponse(journalpostId = "1234", ferdigstilt = true))
        private val journalpostFraIntegrasjoner =
            Journalpost(
                journalpostId = "1234",
                journalposttype = Journalposttype.I,
                journalstatus = Journalstatus.MOTTATT,
                tema = "ENF",
                behandlingstema = "ab0071",
                tittel = "abrakadabra",
                bruker = Bruker(type = BrukerIdType.FNR, id = fnr),
                journalforendeEnhet = "4817",
                kanal = "SKAN_IM",
                relevanteDatoer = listOf(RelevantDato(LocalDateTime.now(), "DATO_REGISTRERT")),
                dokumenter =
                    listOf(
                        DokumentInfo(
                            dokumentInfoId = "12345",
                            tittel = "Søknad om overgangsstønad - dokument 1",
                            brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                            dokumentvarianter =
                                listOf(
                                    Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV, saksbehandlerHarTilgang = true),
                                    Dokumentvariant(variantformat = Dokumentvariantformat.ORIGINAL, saksbehandlerHarTilgang = true),
                                ),
                        ),
                        DokumentInfo(
                            dokumentInfoId = "12345",
                            tittel = "Søknad om barnetilsyn - dokument 1",
                            brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                            dokumentvarianter =
                                listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV, saksbehandlerHarTilgang = true)),
                        ),
                        DokumentInfo(
                            dokumentInfoId = "12345",
                            tittel = "Samboeravtale",
                            brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                            dokumentvarianter =
                                listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV, saksbehandlerHarTilgang = true)),
                        ),
                        DokumentInfo(
                            dokumentInfoId = "12345",
                            tittel = "Manuelt skannet dokument",
                            brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                            dokumentvarianter =
                                listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV, saksbehandlerHarTilgang = true)),
                            logiskeVedlegg =
                                listOf(
                                    LogiskVedlegg(
                                        logiskVedleggId = "1",
                                        tittel = "Manuelt skannet samværsavtale",
                                    ),
                                    LogiskVedlegg(
                                        logiskVedleggId = "2",
                                        tittel = "Annen fritekst fra gosys",
                                    ),
                                ),
                        ),
                        DokumentInfo(
                            dokumentInfoId = "12345",
                            tittel = "EtFrykteligLangtDokumentNavnSomTroligIkkeBrekkerOgØdeleggerGUI",
                            brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                            dokumentvarianter =
                                listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV, saksbehandlerHarTilgang = true)),
                        ),
                        DokumentInfo(
                            dokumentInfoId = "12345",
                            tittel = "Søknad om overgangsstønad - dokument 2",
                            brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                            dokumentvarianter =
                                listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV, saksbehandlerHarTilgang = true)),
                        ),
                        DokumentInfo(
                            dokumentInfoId = "12345",
                            tittel = "Søknad om overgangsstønad - dokument 3",
                            brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                            dokumentvarianter =
                                listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV, saksbehandlerHarTilgang = true)),
                        ),
                    ),
            )
        private val journalpostPapirsøknadFraIntegrasjoner =
            Journalpost(
                journalpostId = "1234",
                journalposttype = Journalposttype.I,
                journalstatus = Journalstatus.MOTTATT,
                tema = "ENF",
                behandlingstema = "ab0071",
                tittel = "abrakadabra",
                bruker = Bruker(type = BrukerIdType.FNR, id = fnr),
                journalforendeEnhet = "4817",
                kanal = "SKAN_IM",
                relevanteDatoer = listOf(RelevantDato(LocalDateTime.now(), "DATO_REGISTRERT")),
                dokumenter =
                    listOf(
                        DokumentInfo(
                            dokumentInfoId = "12345",
                            tittel = "Søknad om overgangsstønad - dokument 1",
                            brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                            dokumentvarianter =
                                listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV, saksbehandlerHarTilgang = true)),
                        ),
                    ),
            )

        private val journalpost = Ressurs.success(journalpostFraIntegrasjoner)
        private val journalpostPapirsøknad = Ressurs.success(journalpostPapirsøknadFraIntegrasjoner)
        private val journalposter = Ressurs.success(listOf(journalpostFraIntegrasjoner))
        private val navKontorEnhet =
            Ressurs.success(
                NavKontorEnhet(
                    enhetId = 100000194,
                    navn = "NAV Kristiansand",
                    enhetNr = "1001",
                    status = "Aktiv",
                ),
            )
        private val saksbehandler =
            Ressurs.success(
                Saksbehandler(
                    azureId = UUID.randomUUID(),
                    navIdent = "Z999999",
                    fornavn = "Darth",
                    etternavn = "Vader",
                    enhet = "4405",
                    enhetsnavn = "Skien",
                ),
            )
    }
}
