package no.nav.familie.ef.klage.infrastruktur.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import no.nav.familie.klage.arbeidsfordeling.Arbeidsfordelingsenhet
import no.nav.familie.klage.felles.dto.EgenAnsattResponse
import no.nav.familie.klage.felles.dto.Tilgang
import no.nav.familie.klage.infrastruktur.config.IntegrasjonerConfig
import no.nav.familie.kontrakter.ef.sak.DokumentBrevkode
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Ressurs
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
import no.nav.familie.kontrakter.felles.kodeverk.BeskrivelseDto
import no.nav.familie.kontrakter.felles.kodeverk.BetydningDto
import no.nav.familie.kontrakter.felles.kodeverk.InntektKodeverkDto
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkDto
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import no.nav.familie.kontrakter.felles.navkontor.NavKontorEnhet
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class FamilieIntegrasjonerMock(integrasjonerConfig: IntegrasjonerConfig) {

    private val responses =
        listOf(
            get(urlEqualTo(integrasjonerConfig.pingUri.path))
                .willReturn(aResponse().withStatus(200)),
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
                                objectMapper.writeValueAsBytes(Testsøknad.søknadOvergangsstønad)
                            )
                        )
                    )
                ),
            get(urlPathMatching("${integrasjonerConfig.journalPostUri.path}/hentdokument/([0-9]*)/([0-9]*)"))
                .withQueryParam("variantFormat", equalTo("ARKIV"))
                .willReturn(okJson(objectMapper.writeValueAsString(Ressurs.success(pdfAsBase64String)))),
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
                                    .UGRADERT
                            )
                        )
                    )
                ),
            post(urlEqualTo(integrasjonerConfig.distribuerDokumentUri.path))
                .willReturn(WireMock.okJson(objectMapper.writeValueAsString(
                    Ressurs.success(
                        "123"
                    )
                )).withStatus(200)),
            post(urlEqualTo(integrasjonerConfig.sendTilKabalUri.path))
                .willReturn(WireMock.okJson(objectMapper.writeValueAsString(
                    Ressurs.success(
                        "123456"
                    )
                )).withStatus(200)),

        )

    private fun lagIkkeTilgangResponse() = Tilgang(
        false,
        "Mock sier: Du har " +
            "ikke tilgang " +
            "til person ikkeTilgang"
    )

    @Bean("mock-integrasjoner")
    @Profile("mock-integrasjoner")
    fun integrationMockServer(): WireMockServer {
        val mockServer = WireMockServer(8385)
        responses.forEach {
            mockServer.stubFor(it)
        }
        mockServer.start()
        return mockServer
    }

    companion object {

        private val egenAnsatt = Ressurs.success(EgenAnsattResponse(false))
        private val poststed =
            KodeverkDto(
                mapOf(
                    "0575" to listOf(
                        BetydningDto(
                            LocalDate.MIN,
                            LocalDate.MAX,
                            mapOf(
                                "nb" to BeskrivelseDto(
                                    "OSLO",
                                    "OSLO"
                                )
                            )
                        )
                    )
                )
            )
        private val land = KodeverkDto(
            mapOf(
                "NOR" to listOf(
                    BetydningDto(
                        LocalDate.MIN,
                        LocalDate.MAX,
                        mapOf(
                            "nb" to BeskrivelseDto(
                                "NORGE",
                                "NORGE"
                            )
                        )
                    )
                )
            )
        )
        private val kodeverkPoststed = Ressurs.success(poststed)
        private val kodeverkLand = Ressurs.success(land)
        private val kodeverkInntekt: Ressurs<InntektKodeverkDto> = Ressurs.success(emptyMap())

        private val arbeidsfordeling =
            Ressurs.success(listOf(Arbeidsfordelingsenhet("4489", "nerd-enhet")))

        private const val fnr = "23097825289"
        private val medl =
            Ressurs.success(
                Medlemskapsinfo(
                    personIdent = fnr,
                    gyldigePerioder = emptyList(),
                    uavklartePerioder = emptyList(),
                    avvistePerioder = emptyList()
                )
            )

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
                            Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV),
                            Dokumentvariant(variantformat = Dokumentvariantformat.ORIGINAL)
                        )
                    ),
                    DokumentInfo(
                        dokumentInfoId = "12345",
                        tittel = "Søknad om barnetilsyn - dokument 1",
                        brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                        dokumentvarianter =
                        listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV))
                    ),
                    DokumentInfo(
                        dokumentInfoId = "12345",
                        tittel = "Samboeravtale",
                        brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                        dokumentvarianter =
                        listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV))
                    ),
                    DokumentInfo(
                        dokumentInfoId = "12345",
                        tittel = "Manuelt skannet dokument",
                        brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                        dokumentvarianter =
                        listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV)),
                        logiskeVedlegg = listOf(
                            LogiskVedlegg(
                                logiskVedleggId = "1",
                                tittel = "Manuelt skannet samværsavtale"
                            ),
                            LogiskVedlegg(
                                logiskVedleggId = "2",
                                tittel = "Annen fritekst fra gosys"
                            )
                        )
                    ),
                    DokumentInfo(
                        dokumentInfoId = "12345",
                        tittel = "EtFrykteligLangtDokumentNavnSomTroligIkkeBrekkerOgØdeleggerGUI",
                        brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                        dokumentvarianter =
                        listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV))
                    ),
                    DokumentInfo(
                        dokumentInfoId = "12345",
                        tittel = "Søknad om overgangsstønad - dokument 2",
                        brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                        dokumentvarianter =
                        listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV))
                    ),
                    DokumentInfo(
                        dokumentInfoId = "12345",
                        tittel = "Søknad om overgangsstønad - dokument 3",
                        brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                        dokumentvarianter =
                        listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV))
                    )
                )
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
                        listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV))
                    )
                )
            )

        private val journalpost = Ressurs.success(journalpostFraIntegrasjoner)
        private val journalpostPapirsøknad = Ressurs.success(journalpostPapirsøknadFraIntegrasjoner)
        private val journalposter = Ressurs.success(listOf(journalpostFraIntegrasjoner))
        private val navKontorEnhet = Ressurs.success(
            NavKontorEnhet(
                enhetId = 100000194,
                navn = "NAV Kristiansand",
                enhetNr = "1001",
                status = "Aktiv"
            )
        )
    }
}
