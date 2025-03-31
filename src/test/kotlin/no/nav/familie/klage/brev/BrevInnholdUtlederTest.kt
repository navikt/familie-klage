package no.nav.familie.klage.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.klage.brev.avvistbrev.AvvistBrevInnholdUtleder
import no.nav.familie.klage.brev.avvistbrev.EFAvvistBrevInnholdUtleder
import no.nav.familie.klage.formkrav.domain.FormVilkår
import no.nav.familie.klage.testutil.DomainUtil.oppfyltForm
import no.nav.familie.klage.testutil.DomainUtil.påklagetVedtakDetaljer
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.FagsystemType
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDateTime
import java.util.UUID
import no.nav.familie.klage.brev.avvistbrev.BAAvvistBrevInnholdUtleder
import no.nav.familie.klage.brev.avvistbrev.KSAvvistBrevInnholdUtleder
import no.nav.familie.klage.felles.util.StønadstypeVisningsnavn.visningsnavn
import org.junit.jupiter.api.BeforeEach

internal class BrevInnholdUtlederTest {
    private val vedtakstidspunkt = LocalDateTime.of(2021, 11, 5, 14, 56, 22)
    private val avvistBrevInnholdUtlederLookup = mockk<AvvistBrevInnholdUtleder.Lookup>()
    private val brevInnholdUtleder = BrevInnholdUtleder(avvistBrevInnholdUtlederLookup)

    private val lesMerUrls = mapOf(
        Stønadstype.OVERGANGSSTØNAD to "nav.no/alene-med-barn",
        Stønadstype.BARNETILSYN to "nav.no/alene-med-barn",
        Stønadstype.SKOLEPENGER to "nav.no/alene-med-barn",
        Stønadstype.BARNETRYGD to "nav.no/barnetrygd",
        Stønadstype.KONTANTSTØTTE to "nav.no/kontantstotte",
    )

    private val klageUrls = mapOf(
        Stønadstype.OVERGANGSSTØNAD to "nav.no/klage#overgangsstonad-til-enslig-mor-eller-far",
        Stønadstype.BARNETILSYN to "nav.no/klage#stonad-til-barnetilsyn-for-enslig-mor-eller-far",
        Stønadstype.SKOLEPENGER to "nav.no/klage#stonad-til-skolepenger-for-enslig-mor-eller-far",
        Stønadstype.BARNETRYGD to "nav.no/klage#barnetrygd",
        Stønadstype.KONTANTSTØTTE to "nav.no/klage#kontantstotte",
    )

    @BeforeEach
    fun oppsett() {
        every { avvistBrevInnholdUtlederLookup.hentAvvistBrevUtlederForFagsystem(Fagsystem.EF) } returns EFAvvistBrevInnholdUtleder()
        every { avvistBrevInnholdUtlederLookup.hentAvvistBrevUtlederForFagsystem(Fagsystem.BA) } returns BAAvvistBrevInnholdUtleder()
        every { avvistBrevInnholdUtlederLookup.hentAvvistBrevUtlederForFagsystem(Fagsystem.KS) } returns KSAvvistBrevInnholdUtleder()
    }

    @Nested
    inner class LagOpprettholdelseBrev {
        private val klageMottatt = vedtakstidspunkt.plusDays(1).toLocalDate()

        @ParameterizedTest
        @EnumSource(
            value = Stønadstype::class,
            names = ["BARNETRYGD", "KONTANTSTØTTE"],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `brev for opprettholdelse skal inneholde blant annat dato og stønadstype`(
            stønadstype: Stønadstype
        ) {
            // Arrange
            val ident = "12345678903"
            val navn = "Navn Navnesen"
            val instillingKlageinstans = "Innhold for innstilling klageinstans"

            val påklagetVedtakDetaljer = påklagetVedtakDetaljer(
                eksternFagsystemBehandlingId = "123",
                vedtakstidspunkt = vedtakstidspunkt
            )

            // Act
            val opprettholdelsesbrev =
                brevInnholdUtleder.lagOpprettholdelseBrev(
                    ident = ident,
                    instillingKlageinstans = instillingKlageinstans,
                    navn = navn,
                    stønadstype = stønadstype,
                    påklagetVedtakDetaljer = påklagetVedtakDetaljer,
                    klageMottatt = klageMottatt,
                )

            // Assert
            assertThat(opprettholdelsesbrev.overskrift).isEqualTo("Vi har sendt klagen din til Nav Klageinstans Nord")
            assertThat(opprettholdelsesbrev.personIdent).isEqualTo(ident)
            assertThat(opprettholdelsesbrev.navn).isEqualTo(navn)
            assertThat(opprettholdelsesbrev.avsnitt).hasSize(5)
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(0)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo(
                    "Vi har 06.11.2021 fått klagen din på vedtaket om ${stønadstype.visningsnavn()} som ble gjort 05.11.2021, " +
                            "og kommet frem til at vi ikke endrer vedtaket. Nav Klageinstans skal derfor vurdere saken din på nytt."
                )
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(1)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Saksbehandlingstidene finner du på nav.no/saksbehandlingstider.")
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(2)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Dette er vurderingen vi har sendt til Nav Klageinstans")
                assertThat(it.innhold).isEqualTo(instillingKlageinstans)
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(3)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Har du nye opplysninger?")
                assertThat(it.innhold).isEqualTo("Har du nye opplysninger eller ønsker å uttale deg, kan du sende oss dette via \n${klageUrls[stønadstype]}.")
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(4)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Har du spørsmål?")
                assertThat(it.innhold).isEqualTo(
                    "Du finner mer informasjon på ${lesMerUrls[stønadstype]}.\n\n" +
                            "På nav.no/kontakt kan du chatte eller skrive til oss.\n\n" +
                            "Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00-15.00."
                )
            })
        }

        @ParameterizedTest
        @EnumSource(
            value = Stønadstype::class,
            names = ["BARNETRYGD", "KONTANTSTØTTE"],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `brev for opprettholdelse skal ha med info om tilbakebetaling`(
            stønadstype: Stønadstype
        ) {
            // Arrange
            val ident = "12345678903"
            val navn = "Navn Navnesen"
            val instillingKlageinstans = "Innhold for innstilling klageinstans"

            val påklagetVedtakDetaljer =
                påklagetVedtakDetaljer(
                    "123",
                    vedtakstidspunkt = vedtakstidspunkt,
                    fagsystemType = FagsystemType.TILBAKEKREVING,
                )

            // Act
            val opprettholdelsesbrev =
                brevInnholdUtleder.lagOpprettholdelseBrev(
                    ident = ident,
                    instillingKlageinstans = instillingKlageinstans,
                    navn = navn,
                    stønadstype = stønadstype,
                    påklagetVedtakDetaljer = påklagetVedtakDetaljer,
                    klageMottatt = klageMottatt,
                )

            // Assert
            assertThat(opprettholdelsesbrev.overskrift).isEqualTo("Vi har sendt klagen din til Nav Klageinstans Nord")
            assertThat(opprettholdelsesbrev.personIdent).isEqualTo(ident)
            assertThat(opprettholdelsesbrev.navn).isEqualTo(navn)
            assertThat(opprettholdelsesbrev.avsnitt).hasSize(5)
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(0)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo(
                    "Vi har 06.11.2021 fått klagen din på vedtaket om tilbakebetaling av ${stønadstype.visningsnavn()} som ble gjort 05.11.2021, " +
                            "og kommet frem til at vi ikke endrer vedtaket. Nav Klageinstans skal derfor vurdere saken din på nytt."
                )
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(1)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Saksbehandlingstidene finner du på nav.no/saksbehandlingstider.")
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(2)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Dette er vurderingen vi har sendt til Nav Klageinstans")
                assertThat(it.innhold).isEqualTo(instillingKlageinstans)
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(3)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Har du nye opplysninger?")
                assertThat(it.innhold).isEqualTo("Har du nye opplysninger eller ønsker å uttale deg, kan du sende oss dette via \n${klageUrls[stønadstype]}.")
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(4)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Har du spørsmål?")
                assertThat(it.innhold).isEqualTo(
                    "Du finner mer informasjon på ${lesMerUrls[stønadstype]}.\n\n" +
                            "På nav.no/kontakt kan du chatte eller skrive til oss.\n\n" +
                            "Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00-15.00."
                )
            })
        }

        @ParameterizedTest
        @EnumSource(
            value = Stønadstype::class,
            names = ["BARNETRYGD", "KONTANTSTØTTE"],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `skal utlede brev for opprettholdelse for EF og skal ha med info om sanksjon`(
            stønadstype: Stønadstype
        ) {
            // Arrange
            val ident = "12345678903"
            val navn = "Navn Navnesen"
            val instillingKlageinstans = "Innhold for innstilling klageinstans"

            val påklagetVedtakDetaljer =
                påklagetVedtakDetaljer(
                    "123",
                    vedtakstidspunkt = vedtakstidspunkt,
                    fagsystemType = FagsystemType.SANKSJON_1_MND,
                )

            // Act
            val opprettholdelsesbrev =
                brevInnholdUtleder.lagOpprettholdelseBrev(
                    ident = ident,
                    instillingKlageinstans = instillingKlageinstans,
                    navn = navn,
                    stønadstype = stønadstype,
                    påklagetVedtakDetaljer = påklagetVedtakDetaljer,
                    klageMottatt = klageMottatt,
                )

            // Assert
            assertThat(opprettholdelsesbrev.overskrift).isEqualTo("Vi har sendt klagen din til Nav Klageinstans Nord")
            assertThat(opprettholdelsesbrev.personIdent).isEqualTo(ident)
            assertThat(opprettholdelsesbrev.navn).isEqualTo(navn)
            assertThat(opprettholdelsesbrev.avsnitt).hasSize(5)
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(0)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo(
                    "Vi har 06.11.2021 fått klagen din på vedtaket om sanksjon som ble gjort 05.11.2021, " +
                            "og kommet frem til at vi ikke endrer vedtaket. Nav Klageinstans skal derfor vurdere saken din på nytt."
                )
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(1)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Saksbehandlingstidene finner du på nav.no/saksbehandlingstider.")
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(2)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Dette er vurderingen vi har sendt til Nav Klageinstans")
                assertThat(it.innhold).isEqualTo(instillingKlageinstans)
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(3)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Har du nye opplysninger?")
                assertThat(it.innhold).isEqualTo("Har du nye opplysninger eller ønsker å uttale deg, kan du sende oss dette via \n${klageUrls[stønadstype]}.")
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(4)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Har du spørsmål?")
                assertThat(it.innhold).isEqualTo(
                    "Du finner mer informasjon på ${lesMerUrls[stønadstype]}.\n\n" +
                            "På nav.no/kontakt kan du chatte eller skrive til oss.\n\n" +
                            "Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00-15.00."
                )
            })
        }

        @ParameterizedTest
        @EnumSource(
            value = Stønadstype::class,
            names = ["BARNETRYGD", "KONTANTSTØTTE"],
            mode = EnumSource.Mode.INCLUDE,
        )
        fun `skal utlede opprettholdselesbrev for BA og KS sanksjon`(
            stønadstype: Stønadstype
        ) {
            // Arrange
            val ident = "12345678903"
            val navn = "Navn Navnesen"
            val dokumentasjonOgUtredning = "innhold for dokumentasjon og utredning"
            val spørsmåletISaken = "innhold for spørsmålet i saken"
            val aktuelleRettskilder = "innhold i aktuelle rettskilder"
            val klagersAnførsler = "innhold i klagers anførsler"
            val vurderingAvKlagen = "innhold i vurdering av klagen"

            val påklagetVedtakDetaljer =
                påklagetVedtakDetaljer(
                    "123",
                    vedtakstidspunkt = vedtakstidspunkt,
                    fagsystemType = FagsystemType.SANKSJON_1_MND,
                )

            // Act
            val opprettholdelsesbrev =
                brevInnholdUtleder.lagOpprettholdelseBrev(
                    ident = ident,
                    dokumentasjonOgUtredning = dokumentasjonOgUtredning,
                    spørsmåletISaken = spørsmåletISaken,
                    aktuelleRettskilder = aktuelleRettskilder,
                    klagersAnførsler = klagersAnførsler,
                    vurderingAvKlagen = vurderingAvKlagen,
                    navn = navn,
                    stønadstype = stønadstype,
                    påklagetVedtakDetaljer = påklagetVedtakDetaljer,
                    klageMottatt = klageMottatt,
                )

            // Assert
            assertThat(opprettholdelsesbrev.overskrift).isEqualTo("Vi har sendt klagen din til Nav Klageinstans Nord")
            assertThat(opprettholdelsesbrev.personIdent).isEqualTo(ident)
            assertThat(opprettholdelsesbrev.navn).isEqualTo(navn)
            assertThat(opprettholdelsesbrev.avsnitt).hasSize(11)
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(0)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo(
                    "Vi har 06.11.2021 fått klagen din på vedtaket om sanksjon som ble gjort 05.11.2021, og kommet frem til at vi ikke endrer vedtaket. Nav Klageinstans skal derfor vurdere saken din på nytt."
                )
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(1)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Saksbehandlingstidene finner du på nav.no/saksbehandlingstider.")
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(2)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Dette er vurderingen vi har sendt til Nav Klageinstans")
                assertThat(it.innhold).isEmpty()
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(3)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Dokumentasjon og utredning")
                assertThat(it.innhold).isEqualTo(dokumentasjonOgUtredning)
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(4)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Spørsmålet i saken")
                assertThat(it.innhold).isEqualTo(spørsmåletISaken)
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(5)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Aktuelle rettskilder")
                assertThat(it.innhold).isEqualTo(aktuelleRettskilder)
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(6)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Klagers anførsler")
                assertThat(it.innhold).isEqualTo(klagersAnførsler)
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(7)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Vurdering av klagen")
                assertThat(it.innhold).isEqualTo(vurderingAvKlagen)
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(8)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Har du nye opplysninger?")
                assertThat(it.innhold).isEqualTo("Har du nye opplysninger eller ønsker å uttale deg, kan du sende oss dette via \n${klageUrls[stønadstype]}.")
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(9)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til innsyn i saken din")
                assertThat(it.innhold).isEqualTo(
                    "Du har rett til å se dokumentene i saken din. Dette følger av forvaltningsloven § 18. Kontakt oss om du vil se dokumentene i saken din. Ta kontakt på nav.no/kontakt eller på telefon 55 55 33 33 <34>. Du kan lese mer om innsynsretten på nav.no/personvernerklaering."
                )
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(10)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Har du spørsmål?")
                assertThat(it.innhold).isEqualTo(
                    "Du finner mer informasjon på ${lesMerUrls[stønadstype]}.\n\n" +
                            "På nav.no/kontakt kan du chatte eller skrive til oss.\n\n" +
                            "Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00-15.00."
                )
            })
        }

        @ParameterizedTest
        @EnumSource(
            value = Stønadstype::class,
            names = ["BARNETRYGD", "KONTANTSTØTTE"],
            mode = EnumSource.Mode.INCLUDE,
        )
        fun `skal utlede opprettholdselesbrev for BA og KS tilbakekreving`(
            stønadstype: Stønadstype
        ) {
            // Arrange
            val ident = "12345678903"
            val navn = "Navn Navnesen"
            val dokumentasjonOgUtredning = "innhold for dokumentasjon og utredning"
            val spørsmåletISaken = "innhold for spørsmålet i saken"
            val aktuelleRettskilder = "innhold i aktuelle rettskilder"
            val klagersAnførsler = "innhold i klagers anførsler"
            val vurderingAvKlagen = "innhold i vurdering av klagen"

            val påklagetVedtakDetaljer =
                påklagetVedtakDetaljer(
                    "123",
                    vedtakstidspunkt = vedtakstidspunkt,
                    fagsystemType = FagsystemType.TILBAKEKREVING,
                )

            // Act
            val opprettholdelsesbrev =
                brevInnholdUtleder.lagOpprettholdelseBrev(
                    ident = ident,
                    dokumentasjonOgUtredning = dokumentasjonOgUtredning,
                    spørsmåletISaken = spørsmåletISaken,
                    aktuelleRettskilder = aktuelleRettskilder,
                    klagersAnførsler = klagersAnførsler,
                    vurderingAvKlagen = vurderingAvKlagen,
                    navn = navn,
                    stønadstype = stønadstype,
                    påklagetVedtakDetaljer = påklagetVedtakDetaljer,
                    klageMottatt = klageMottatt,
                )

            // Assert
            assertThat(opprettholdelsesbrev.overskrift).isEqualTo("Vi har sendt klagen din til Nav Klageinstans Nord")
            assertThat(opprettholdelsesbrev.personIdent).isEqualTo(ident)
            assertThat(opprettholdelsesbrev.navn).isEqualTo(navn)
            assertThat(opprettholdelsesbrev.avsnitt).hasSize(11)
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(0)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo(
                    "Vi har 06.11.2021 fått klagen din på vedtaket om tilbakebetaling av ${stønadstype.visningsnavn()} som ble gjort 05.11.2021, og kommet frem til at vi ikke endrer vedtaket. Nav Klageinstans skal derfor vurdere saken din på nytt."
                )
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(1)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Saksbehandlingstidene finner du på nav.no/saksbehandlingstider.")
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(2)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Dette er vurderingen vi har sendt til Nav Klageinstans")
                assertThat(it.innhold).isEmpty()
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(3)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Dokumentasjon og utredning")
                assertThat(it.innhold).isEqualTo(dokumentasjonOgUtredning)
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(4)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Spørsmålet i saken")
                assertThat(it.innhold).isEqualTo(spørsmåletISaken)
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(5)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Aktuelle rettskilder")
                assertThat(it.innhold).isEqualTo(aktuelleRettskilder)
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(6)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Klagers anførsler")
                assertThat(it.innhold).isEqualTo(klagersAnførsler)
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(7)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Vurdering av klagen")
                assertThat(it.innhold).isEqualTo(vurderingAvKlagen)
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(8)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Har du nye opplysninger?")
                assertThat(it.innhold).isEqualTo("Har du nye opplysninger eller ønsker å uttale deg, kan du sende oss dette via \n${klageUrls[stønadstype]}.")
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(9)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til innsyn i saken din")
                assertThat(it.innhold).isEqualTo(
                    "Du har rett til å se dokumentene i saken din. Dette følger av forvaltningsloven § 18. Kontakt oss om du vil se dokumentene i saken din. Ta kontakt på nav.no/kontakt eller på telefon 55 55 33 33 <34>. Du kan lese mer om innsynsretten på nav.no/personvernerklaering."
                )
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(10)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Har du spørsmål?")
                assertThat(it.innhold).isEqualTo(
                    "Du finner mer informasjon på ${lesMerUrls[stønadstype]}.\n\n" +
                            "På nav.no/kontakt kan du chatte eller skrive til oss.\n\n" +
                            "Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00-15.00."
                )
            })
        }

        @ParameterizedTest
        @EnumSource(
            value = Stønadstype::class,
            names = ["BARNETRYGD", "KONTANTSTØTTE"],
            mode = EnumSource.Mode.INCLUDE,
        )
        fun `skal utlede opprettholdselesbrev for BA og KS utestengelse`(
            stønadstype: Stønadstype
        ) {
            // Arrange
            val ident = "12345678903"
            val navn = "Navn Navnesen"
            val dokumentasjonOgUtredning = "innhold for dokumentasjon og utredning"
            val spørsmåletISaken = "innhold for spørsmålet i saken"
            val aktuelleRettskilder = "innhold i aktuelle rettskilder"
            val klagersAnførsler = "innhold i klagers anførsler"
            val vurderingAvKlagen = "innhold i vurdering av klagen"

            val påklagetVedtakDetaljer =
                påklagetVedtakDetaljer(
                    "123",
                    vedtakstidspunkt = vedtakstidspunkt,
                    fagsystemType = FagsystemType.UTESTENGELSE,
                )

            // Act
            val opprettholdelsesbrev =
                brevInnholdUtleder.lagOpprettholdelseBrev(
                    ident = ident,
                    dokumentasjonOgUtredning = dokumentasjonOgUtredning,
                    spørsmåletISaken = spørsmåletISaken,
                    aktuelleRettskilder = aktuelleRettskilder,
                    klagersAnførsler = klagersAnførsler,
                    vurderingAvKlagen = vurderingAvKlagen,
                    navn = navn,
                    stønadstype = stønadstype,
                    påklagetVedtakDetaljer = påklagetVedtakDetaljer,
                    klageMottatt = klageMottatt,
                )

            // Assert
            assertThat(opprettholdelsesbrev.overskrift).isEqualTo("Vi har sendt klagen din til Nav Klageinstans Nord")
            assertThat(opprettholdelsesbrev.personIdent).isEqualTo(ident)
            assertThat(opprettholdelsesbrev.navn).isEqualTo(navn)
            assertThat(opprettholdelsesbrev.avsnitt).hasSize(11)
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(0)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo(
                    "Vi har 06.11.2021 fått klagen din på vedtaket om utestengelse som ble gjort 05.11.2021, og kommet frem til at vi ikke endrer vedtaket. Nav Klageinstans skal derfor vurdere saken din på nytt."
                )
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(1)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Saksbehandlingstidene finner du på nav.no/saksbehandlingstider.")
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(2)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Dette er vurderingen vi har sendt til Nav Klageinstans")
                assertThat(it.innhold).isEmpty()
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(3)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Dokumentasjon og utredning")
                assertThat(it.innhold).isEqualTo(dokumentasjonOgUtredning)
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(4)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Spørsmålet i saken")
                assertThat(it.innhold).isEqualTo(spørsmåletISaken)
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(5)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Aktuelle rettskilder")
                assertThat(it.innhold).isEqualTo(aktuelleRettskilder)
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(6)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Klagers anførsler")
                assertThat(it.innhold).isEqualTo(klagersAnførsler)
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(7)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Vurdering av klagen")
                assertThat(it.innhold).isEqualTo(vurderingAvKlagen)
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(8)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Har du nye opplysninger?")
                assertThat(it.innhold).isEqualTo("Har du nye opplysninger eller ønsker å uttale deg, kan du sende oss dette via \n${klageUrls[stønadstype]}.")
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(9)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til innsyn i saken din")
                assertThat(it.innhold).isEqualTo(
                    "Du har rett til å se dokumentene i saken din. Dette følger av forvaltningsloven § 18. Kontakt oss om du vil se dokumentene i saken din. Ta kontakt på nav.no/kontakt eller på telefon 55 55 33 33 <34>. Du kan lese mer om innsynsretten på nav.no/personvernerklaering."
                )
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(10)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Har du spørsmål?")
                assertThat(it.innhold).isEqualTo(
                    "Du finner mer informasjon på ${lesMerUrls[stønadstype]}.\n\n" +
                            "På nav.no/kontakt kan du chatte eller skrive til oss.\n\n" +
                            "Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00-15.00."
                )
            })
        }

        @ParameterizedTest
        @EnumSource(
            value = Stønadstype::class,
            names = ["BARNETRYGD", "KONTANTSTØTTE"],
            mode = EnumSource.Mode.INCLUDE,
        )
        fun `skal utlede opprettholdselesbrev for BA og KS ordinær`(
            stønadstype: Stønadstype
        ) {
            // Arrange
            val ident = "12345678903"
            val navn = "Navn Navnesen"
            val dokumentasjonOgUtredning = "innhold for dokumentasjon og utredning"
            val spørsmåletISaken = "innhold for spørsmålet i saken"
            val aktuelleRettskilder = "innhold i aktuelle rettskilder"
            val klagersAnførsler = "innhold i klagers anførsler"
            val vurderingAvKlagen = "innhold i vurdering av klagen"

            val påklagetVedtakDetaljer =
                påklagetVedtakDetaljer(
                    "123",
                    vedtakstidspunkt = vedtakstidspunkt,
                    fagsystemType = FagsystemType.ORDNIÆR,
                )

            // Act
            val opprettholdelsesbrev =
                brevInnholdUtleder.lagOpprettholdelseBrev(
                    ident = ident,
                    dokumentasjonOgUtredning = dokumentasjonOgUtredning,
                    spørsmåletISaken = spørsmåletISaken,
                    aktuelleRettskilder = aktuelleRettskilder,
                    klagersAnførsler = klagersAnførsler,
                    vurderingAvKlagen = vurderingAvKlagen,
                    navn = navn,
                    stønadstype = stønadstype,
                    påklagetVedtakDetaljer = påklagetVedtakDetaljer,
                    klageMottatt = klageMottatt,
                )

            // Assert
            assertThat(opprettholdelsesbrev.overskrift).isEqualTo("Vi har sendt klagen din til Nav Klageinstans Nord")
            assertThat(opprettholdelsesbrev.personIdent).isEqualTo(ident)
            assertThat(opprettholdelsesbrev.navn).isEqualTo(navn)
            assertThat(opprettholdelsesbrev.avsnitt).hasSize(11)
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(0)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo(
                    "Vi har 06.11.2021 fått klagen din på vedtaket om ${stønadstype.visningsnavn()} som ble gjort 05.11.2021, og kommet frem til at vi ikke endrer vedtaket. Nav Klageinstans skal derfor vurdere saken din på nytt."
                )
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(1)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Saksbehandlingstidene finner du på nav.no/saksbehandlingstider.")
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(2)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Dette er vurderingen vi har sendt til Nav Klageinstans")
                assertThat(it.innhold).isEmpty()
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(3)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Dokumentasjon og utredning")
                assertThat(it.innhold).isEqualTo(dokumentasjonOgUtredning)
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(4)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Spørsmålet i saken")
                assertThat(it.innhold).isEqualTo(spørsmåletISaken)
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(5)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Aktuelle rettskilder")
                assertThat(it.innhold).isEqualTo(aktuelleRettskilder)
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(6)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Klagers anførsler")
                assertThat(it.innhold).isEqualTo(klagersAnførsler)
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(7)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Vurdering av klagen")
                assertThat(it.innhold).isEqualTo(vurderingAvKlagen)
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(8)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Har du nye opplysninger?")
                assertThat(it.innhold).isEqualTo("Har du nye opplysninger eller ønsker å uttale deg, kan du sende oss dette via \n${klageUrls[stønadstype]}.")
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(9)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til innsyn i saken din")
                assertThat(it.innhold).isEqualTo(
                    "Du har rett til å se dokumentene i saken din. Dette følger av forvaltningsloven § 18. Kontakt oss om du vil se dokumentene i saken din. Ta kontakt på nav.no/kontakt eller på telefon 55 55 33 33 <34>. Du kan lese mer om innsynsretten på nav.no/personvernerklaering."
                )
            })
            assertThat(opprettholdelsesbrev.avsnitt.elementAt(10)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Har du spørsmål?")
                assertThat(it.innhold).isEqualTo(
                    "Du finner mer informasjon på ${lesMerUrls[stønadstype]}.\n\n" +
                            "På nav.no/kontakt kan du chatte eller skrive til oss.\n\n" +
                            "Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00-15.00."
                )
            })
        }
    }

    @Nested
    inner class LagFormkravAvvistBrev {
        @ParameterizedTest
        @EnumSource(
            value = Stønadstype::class,
            names = ["BARNETRYGD", "KONTANTSTØTTE"],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `brev for avvist formkrav skal ha med info om tilbakebetaling for EF`(
            stønadstype: Stønadstype
        ) {
            // Arrange
            val ident = "123456789"
            val navn = "Navn Navnesen"

            val påklagetVedtakDetaljer =
                påklagetVedtakDetaljer(
                    "123",
                    vedtakstidspunkt = vedtakstidspunkt,
                    fagsystemType = FagsystemType.TILBAKEKREVING,
                )

            // Act
            val formkravAvvistBrev =
                brevInnholdUtleder.lagFormkravAvvistBrev(
                    ident = ident,
                    navn = navn,
                    form = ikkeOppfyltForm(),
                    stønadstype = stønadstype,
                    påklagetVedtakDetaljer = påklagetVedtakDetaljer,
                    fagsystem = Fagsystem.EF,
                )

            // Assert
            assertThat(formkravAvvistBrev.overskrift).isEqualTo("Vi har avvist klagen din på vedtaket om tilbakebetaling av ${stønadstype.visningsnavn()}")
            assertThat(formkravAvvistBrev.personIdent).isEqualTo(ident)
            assertThat(formkravAvvistBrev.navn).isEqualTo(navn)
            assertThat(formkravAvvistBrev.avsnitt).hasSize(6)
            assertThat(formkravAvvistBrev.avsnitt.elementAt(0)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Vi har avvist klagen din fordi du har klaget på et vedtak som ikke gjelder deg.")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(1)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("brevtekst")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(2)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Vedtaket er gjort etter forvaltningsloven §§ 28 og 33.")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(3)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til å klage")
                assertThat(it.innhold).isEqualTo(
                    "Hvis du vil klage, må du gjøre dette innen 6 uker fra den datoen du fikk dette brevet. " +
                            "Du finner skjema og informasjon på ${klageUrls[stønadstype]}."
                )
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(4)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til innsyn")
                assertThat(it.innhold).isEqualTo(
                    "På nav.no/dittnav kan du se dokumentene i saken din."
                )
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(5)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Har du spørsmål?")
                assertThat(it.innhold).isEqualTo(
                    "Du finner mer informasjon på ${lesMerUrls[stønadstype]}.\n\n" +
                            "På nav.no/kontakt kan du chatte eller skrive til oss.\n\n" +
                            "Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00-15.00."
                )
            })
        }

        @Test
        fun `brev for avvist formkrav skal ha med info om tilbakebetaling for BA`() {
            // Arrange
            val ident = "123456789"
            val navn = "Navn Navnesen"
            val stønadstype = Stønadstype.BARNETRYGD

            val påklagetVedtakDetaljer =
                påklagetVedtakDetaljer(
                    "123",
                    vedtakstidspunkt = vedtakstidspunkt,
                    fagsystemType = FagsystemType.TILBAKEKREVING,
                )

            // Act
            val formkravAvvistBrev =
                brevInnholdUtleder.lagFormkravAvvistBrev(
                    ident = ident,
                    navn = navn,
                    form = ikkeOppfyltForm(),
                    stønadstype = stønadstype,
                    påklagetVedtakDetaljer = påklagetVedtakDetaljer,
                    fagsystem = Fagsystem.BA,
                )

            // Assert
            assertThat(formkravAvvistBrev.overskrift).isEqualTo("Vi har avvist klagen din på vedtaket om tilbakebetaling av ${stønadstype.visningsnavn()}")
            assertThat(formkravAvvistBrev.personIdent).isEqualTo(ident)
            assertThat(formkravAvvistBrev.navn).isEqualTo(navn)
            assertThat(formkravAvvistBrev.avsnitt).hasSize(6)
            assertThat(formkravAvvistBrev.avsnitt.elementAt(0)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Vi har avvist klagen din fordi du har klaget på et vedtak som ikke gjelder deg.")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(1)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("brevtekst")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(2)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Vedtaket er gjort etter forvaltningsloven §§ 28 og 33.")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(3)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til å klage")
                assertThat(it.innhold).isEqualTo(
                    "Hvis du vil klage, må du gjøre dette innen 6 uker fra den datoen du fikk dette brevet. " +
                            "Du finner skjema og informasjon på ${klageUrls[stønadstype]}."
                )
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(4)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til innsyn i saken din")
                assertThat(it.innhold).isEqualTo(
                    "Du har rett til å se dokumentene i saken din. Dette følger av forvaltningsloven § 18. Kontakt oss om du vil se dokumentene i saken din. Ta kontakt på nav.no/kontakt eller på telefon 55 55 33 33 <34>. Du kan lese mer om innsynsretten på nav.no/personvernerklaering."
                )
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(5)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Har du spørsmål?")
                assertThat(it.innhold).isEqualTo(
                    "Du finner mer informasjon på ${lesMerUrls[stønadstype]}.\n\n" +
                            "På nav.no/kontakt kan du chatte eller skrive til oss.\n\n" +
                            "Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00-15.00."
                )
            })
        }

        @Test
        fun `brev for avvist formkrav skal ha med info om tilbakebetaling for KS`() {
            // Arrange
            val ident = "123456789"
            val navn = "Navn Navnesen"
            val stønadstype = Stønadstype.KONTANTSTØTTE

            val påklagetVedtakDetaljer =
                påklagetVedtakDetaljer(
                    "123",
                    vedtakstidspunkt = vedtakstidspunkt,
                    fagsystemType = FagsystemType.TILBAKEKREVING,
                )

            // Act
            val formkravAvvistBrev =
                brevInnholdUtleder.lagFormkravAvvistBrev(
                    ident = ident,
                    navn = navn,
                    form = ikkeOppfyltForm(),
                    stønadstype = stønadstype,
                    påklagetVedtakDetaljer = påklagetVedtakDetaljer,
                    fagsystem = Fagsystem.KS,
                )

            // Assert
            assertThat(formkravAvvistBrev.overskrift).isEqualTo("Vi har avvist klagen din på vedtaket om tilbakebetaling av ${stønadstype.visningsnavn()}")
            assertThat(formkravAvvistBrev.personIdent).isEqualTo(ident)
            assertThat(formkravAvvistBrev.navn).isEqualTo(navn)
            assertThat(formkravAvvistBrev.avsnitt).hasSize(6)
            assertThat(formkravAvvistBrev.avsnitt.elementAt(0)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Vi har avvist klagen din fordi du har klaget på et vedtak som ikke gjelder deg.")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(1)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("brevtekst")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(2)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Vedtaket er gjort etter forvaltningsloven §§ 28 og 33.")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(3)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til å klage")
                assertThat(it.innhold).isEqualTo(
                    "Hvis du vil klage, må du gjøre dette innen 6 uker fra den datoen du fikk dette brevet. " +
                            "Du finner skjema og informasjon på ${klageUrls[stønadstype]}."
                )
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(4)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til innsyn i saken din")
                assertThat(it.innhold).isEqualTo(
                    "Du har rett til å se dokumentene i saken din. Dette følger av forvaltningsloven § 18. Kontakt oss om du vil se dokumentene i saken din. Ta kontakt på nav.no/kontakt eller på telefon 55 55 33 33 <34>. Du kan lese mer om innsynsretten på nav.no/personvernerklaering."
                )
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(5)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Har du spørsmål?")
                assertThat(it.innhold).isEqualTo(
                    "Du finner mer informasjon på ${lesMerUrls[stønadstype]}.\n\n" +
                            "På nav.no/kontakt kan du chatte eller skrive til oss.\n\n" +
                            "Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00-15.00."
                )
            })
        }

        @Test
        fun `brev for avvist formkrav skal ha med info om sanksjon for EF`() {
            // Arrange
            val ident = "123456789"
            val navn = "Navn Navnesen"

            val påklagetVedtakDetaljer =
                påklagetVedtakDetaljer(
                    "123",
                    vedtakstidspunkt = vedtakstidspunkt,
                    fagsystemType = FagsystemType.SANKSJON_1_MND,
                )

            // Act
            val formkravAvvistBrev =
                brevInnholdUtleder.lagFormkravAvvistBrev(
                    ident = ident,
                    navn = navn,
                    form = ikkeOppfyltForm(),
                    stønadstype = Stønadstype.BARNETILSYN,
                    påklagetVedtakDetaljer = påklagetVedtakDetaljer,
                    fagsystem = Fagsystem.EF,
                )

            // Assert
            assertThat(formkravAvvistBrev.overskrift).isEqualTo("Vi har avvist klagen din på vedtaket om sanksjon")
            assertThat(formkravAvvistBrev.personIdent).isEqualTo(ident)
            assertThat(formkravAvvistBrev.navn).isEqualTo(navn)
            assertThat(formkravAvvistBrev.avsnitt).hasSize(6)
            assertThat(formkravAvvistBrev.avsnitt.elementAt(0)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Vi har avvist klagen din fordi du har klaget på et vedtak som ikke gjelder deg.")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(1)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("brevtekst")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(2)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Vedtaket er gjort etter forvaltningsloven §§ 28 og 33.")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(3)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til å klage")
                assertThat(it.innhold).isEqualTo(
                    "Hvis du vil klage, må du gjøre dette innen 6 uker fra den datoen du fikk dette brevet. " +
                            "Du finner skjema og informasjon på ${klageUrls[Stønadstype.BARNETILSYN]}."
                )
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(4)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til innsyn")
                assertThat(it.innhold).isEqualTo(
                    "På nav.no/dittnav kan du se dokumentene i saken din."
                )
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(5)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Har du spørsmål?")
                assertThat(it.innhold).isEqualTo(
                    "Du finner mer informasjon på ${lesMerUrls[Stønadstype.BARNETILSYN]}.\n\n" +
                            "På nav.no/kontakt kan du chatte eller skrive til oss.\n\n" +
                            "Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00-15.00."
                )
            })
        }

        @Test
        fun `brev for avvist formkrav skal ha med info om sanksjon for BA`() {
            // Arrange
            val ident = "123456789"
            val navn = "Navn Navnesen"

            val påklagetVedtakDetaljer =
                påklagetVedtakDetaljer(
                    "123",
                    vedtakstidspunkt = vedtakstidspunkt,
                    fagsystemType = FagsystemType.SANKSJON_1_MND,
                )

            // Act
            val formkravAvvistBrev =
                brevInnholdUtleder.lagFormkravAvvistBrev(
                    ident = ident,
                    navn = navn,
                    form = ikkeOppfyltForm(),
                    stønadstype = Stønadstype.BARNETRYGD,
                    påklagetVedtakDetaljer = påklagetVedtakDetaljer,
                    fagsystem = Fagsystem.BA,
                )

            // Assert
            assertThat(formkravAvvistBrev.overskrift).isEqualTo("Vi har avvist klagen din på vedtaket om sanksjon")
            assertThat(formkravAvvistBrev.personIdent).isEqualTo(ident)
            assertThat(formkravAvvistBrev.navn).isEqualTo(navn)
            assertThat(formkravAvvistBrev.avsnitt).hasSize(6)
            assertThat(formkravAvvistBrev.avsnitt.elementAt(0)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Vi har avvist klagen din fordi du har klaget på et vedtak som ikke gjelder deg.")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(1)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("brevtekst")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(2)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Vedtaket er gjort etter forvaltningsloven §§ 28 og 33.")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(3)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til å klage")
                assertThat(it.innhold).isEqualTo(
                    "Hvis du vil klage, må du gjøre dette innen 6 uker fra den datoen du fikk dette brevet. " +
                            "Du finner skjema og informasjon på ${klageUrls[Stønadstype.BARNETRYGD]}."
                )
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(4)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til innsyn i saken din")
                assertThat(it.innhold).isEqualTo(
                    "Du har rett til å se dokumentene i saken din. Dette følger av forvaltningsloven § 18. Kontakt oss om du vil se dokumentene i saken din. Ta kontakt på nav.no/kontakt eller på telefon 55 55 33 33 <34>. Du kan lese mer om innsynsretten på nav.no/personvernerklaering."
                )
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(5)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Har du spørsmål?")
                assertThat(it.innhold).isEqualTo(
                    "Du finner mer informasjon på ${lesMerUrls[Stønadstype.BARNETRYGD]}.\n\n" +
                            "På nav.no/kontakt kan du chatte eller skrive til oss.\n\n" +
                            "Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00-15.00."
                )
            })
        }

        @Test
        fun `brev for avvist formkrav skal ha med info om sanksjon for KS`() {
            // Arrange
            val ident = "123456789"
            val navn = "Navn Navnesen"

            val påklagetVedtakDetaljer =
                påklagetVedtakDetaljer(
                    "123",
                    vedtakstidspunkt = vedtakstidspunkt,
                    fagsystemType = FagsystemType.SANKSJON_1_MND,
                )

            // Act
            val formkravAvvistBrev =
                brevInnholdUtleder.lagFormkravAvvistBrev(
                    ident = ident,
                    navn = navn,
                    form = ikkeOppfyltForm(),
                    stønadstype = Stønadstype.KONTANTSTØTTE,
                    påklagetVedtakDetaljer = påklagetVedtakDetaljer,
                    fagsystem = Fagsystem.KS,
                )

            // Assert
            assertThat(formkravAvvistBrev.overskrift).isEqualTo("Vi har avvist klagen din på vedtaket om sanksjon")
            assertThat(formkravAvvistBrev.personIdent).isEqualTo(ident)
            assertThat(formkravAvvistBrev.navn).isEqualTo(navn)
            assertThat(formkravAvvistBrev.avsnitt).hasSize(6)
            assertThat(formkravAvvistBrev.avsnitt.elementAt(0)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Vi har avvist klagen din fordi du har klaget på et vedtak som ikke gjelder deg.")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(1)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("brevtekst")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(2)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Vedtaket er gjort etter forvaltningsloven §§ 28 og 33.")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(3)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til å klage")
                assertThat(it.innhold).isEqualTo(
                    "Hvis du vil klage, må du gjøre dette innen 6 uker fra den datoen du fikk dette brevet. " +
                            "Du finner skjema og informasjon på ${klageUrls[Stønadstype.KONTANTSTØTTE]}."
                )
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(4)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til innsyn i saken din")
                assertThat(it.innhold).isEqualTo(
                    "Du har rett til å se dokumentene i saken din. Dette følger av forvaltningsloven § 18. Kontakt oss om du vil se dokumentene i saken din. Ta kontakt på nav.no/kontakt eller på telefon 55 55 33 33 <34>. Du kan lese mer om innsynsretten på nav.no/personvernerklaering."
                )
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(5)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Har du spørsmål?")
                assertThat(it.innhold).isEqualTo(
                    "Du finner mer informasjon på ${lesMerUrls[Stønadstype.KONTANTSTØTTE]}.\n\n" +
                            "På nav.no/kontakt kan du chatte eller skrive til oss.\n\n" +
                            "Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00-15.00."
                )
            })
        }

        @Test
        fun `brev for avvist formkrav skal ha med info om utestengelse for EF`() {
            // Arrange
            val ident = "123456789"
            val navn = "Navn Navnesen"

            val påklagetVedtakDetaljer =
                påklagetVedtakDetaljer(
                    "123",
                    vedtakstidspunkt = vedtakstidspunkt,
                    fagsystemType = FagsystemType.UTESTENGELSE,
                )

            // Act
            val formkravAvvistBrev =
                brevInnholdUtleder.lagFormkravAvvistBrev(
                    ident = ident,
                    navn = navn,
                    form = ikkeOppfyltForm(),
                    stønadstype = Stønadstype.BARNETILSYN,
                    påklagetVedtakDetaljer = påklagetVedtakDetaljer,
                    fagsystem = Fagsystem.EF,
                )

            // Assert
            assertThat(formkravAvvistBrev.overskrift).isEqualTo("Vi har avvist klagen din på vedtaket om utestengelse")
            assertThat(formkravAvvistBrev.personIdent).isEqualTo(ident)
            assertThat(formkravAvvistBrev.navn).isEqualTo(navn)
            assertThat(formkravAvvistBrev.avsnitt).hasSize(6)
            assertThat(formkravAvvistBrev.avsnitt.elementAt(0)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Vi har avvist klagen din fordi du har klaget på et vedtak som ikke gjelder deg.")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(1)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("brevtekst")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(2)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Vedtaket er gjort etter forvaltningsloven §§ 28 og 33.")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(3)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til å klage")
                assertThat(it.innhold).isEqualTo(
                    "Hvis du vil klage, må du gjøre dette innen 6 uker fra den datoen du fikk dette brevet. " +
                            "Du finner skjema og informasjon på ${klageUrls[Stønadstype.BARNETILSYN]}."
                )
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(4)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til innsyn")
                assertThat(it.innhold).isEqualTo(
                    "På nav.no/dittnav kan du se dokumentene i saken din."
                )
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(5)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Har du spørsmål?")
                assertThat(it.innhold).isEqualTo(
                    "Du finner mer informasjon på ${lesMerUrls[Stønadstype.BARNETILSYN]}.\n\n" +
                            "På nav.no/kontakt kan du chatte eller skrive til oss.\n\n" +
                            "Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00-15.00."
                )
            })
        }

        @Test
        fun `brev for avvist formkrav skal ha med info om utestengelse for BA`() {
            // Arrange
            val ident = "123456789"
            val navn = "Navn Navnesen"

            val påklagetVedtakDetaljer =
                påklagetVedtakDetaljer(
                    "123",
                    vedtakstidspunkt = vedtakstidspunkt,
                    fagsystemType = FagsystemType.UTESTENGELSE,
                )

            // Act
            val formkravAvvistBrev =
                brevInnholdUtleder.lagFormkravAvvistBrev(
                    ident = ident,
                    navn = navn,
                    form = ikkeOppfyltForm(),
                    stønadstype = Stønadstype.BARNETRYGD,
                    påklagetVedtakDetaljer = påklagetVedtakDetaljer,
                    fagsystem = Fagsystem.BA,
                )

            // Assert
            assertThat(formkravAvvistBrev.overskrift).isEqualTo("Vi har avvist klagen din på vedtaket om utestengelse")
            assertThat(formkravAvvistBrev.personIdent).isEqualTo(ident)
            assertThat(formkravAvvistBrev.navn).isEqualTo(navn)
            assertThat(formkravAvvistBrev.avsnitt).hasSize(6)
            assertThat(formkravAvvistBrev.avsnitt.elementAt(0)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Vi har avvist klagen din fordi du har klaget på et vedtak som ikke gjelder deg.")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(1)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("brevtekst")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(2)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Vedtaket er gjort etter forvaltningsloven §§ 28 og 33.")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(3)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til å klage")
                assertThat(it.innhold).isEqualTo(
                    "Hvis du vil klage, må du gjøre dette innen 6 uker fra den datoen du fikk dette brevet. " +
                            "Du finner skjema og informasjon på ${klageUrls[Stønadstype.BARNETRYGD]}."
                )
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(4)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til innsyn i saken din")
                assertThat(it.innhold).isEqualTo(
                    "Du har rett til å se dokumentene i saken din. Dette følger av forvaltningsloven § 18. Kontakt oss om du vil se dokumentene i saken din. Ta kontakt på nav.no/kontakt eller på telefon 55 55 33 33 <34>. Du kan lese mer om innsynsretten på nav.no/personvernerklaering."
                )
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(5)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Har du spørsmål?")
                assertThat(it.innhold).isEqualTo(
                    "Du finner mer informasjon på ${lesMerUrls[Stønadstype.BARNETRYGD]}.\n\n" +
                            "På nav.no/kontakt kan du chatte eller skrive til oss.\n\n" +
                            "Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00-15.00."
                )
            })
        }

        @Test
        fun `brev for avvist formkrav skal ha med info om utestengelse for KS`() {
            // Arrange
            val ident = "123456789"
            val navn = "Navn Navnesen"

            val påklagetVedtakDetaljer =
                påklagetVedtakDetaljer(
                    "123",
                    vedtakstidspunkt = vedtakstidspunkt,
                    fagsystemType = FagsystemType.UTESTENGELSE,
                )

            // Act
            val formkravAvvistBrev =
                brevInnholdUtleder.lagFormkravAvvistBrev(
                    ident = ident,
                    navn = navn,
                    form = ikkeOppfyltForm(),
                    stønadstype = Stønadstype.KONTANTSTØTTE,
                    påklagetVedtakDetaljer = påklagetVedtakDetaljer,
                    fagsystem = Fagsystem.KS,
                )

            // Assert
            assertThat(formkravAvvistBrev.overskrift).isEqualTo("Vi har avvist klagen din på vedtaket om utestengelse")
            assertThat(formkravAvvistBrev.personIdent).isEqualTo(ident)
            assertThat(formkravAvvistBrev.navn).isEqualTo(navn)
            assertThat(formkravAvvistBrev.avsnitt).hasSize(6)
            assertThat(formkravAvvistBrev.avsnitt.elementAt(0)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Vi har avvist klagen din fordi du har klaget på et vedtak som ikke gjelder deg.")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(1)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("brevtekst")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(2)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Vedtaket er gjort etter forvaltningsloven §§ 28 og 33.")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(3)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til å klage")
                assertThat(it.innhold).isEqualTo(
                    "Hvis du vil klage, må du gjøre dette innen 6 uker fra den datoen du fikk dette brevet. " +
                            "Du finner skjema og informasjon på ${klageUrls[Stønadstype.KONTANTSTØTTE]}."
                )
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(4)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til innsyn i saken din")
                assertThat(it.innhold).isEqualTo(
                    "Du har rett til å se dokumentene i saken din. Dette følger av forvaltningsloven § 18. Kontakt oss om du vil se dokumentene i saken din. Ta kontakt på nav.no/kontakt eller på telefon 55 55 33 33 <34>. Du kan lese mer om innsynsretten på nav.no/personvernerklaering."
                )
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(5)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Har du spørsmål?")
                assertThat(it.innhold).isEqualTo(
                    "Du finner mer informasjon på ${lesMerUrls[Stønadstype.KONTANTSTØTTE]}.\n\n" +
                            "På nav.no/kontakt kan du chatte eller skrive til oss.\n\n" +
                            "Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00-15.00."
                )
            })
        }

        @Test
        fun `skal utlede avvist brev om vedtak for BA`() {
            // Arrange
            val ident = "123456789"
            val navn = "Navn Navnesen"
            val stønadstype = Stønadstype.BARNETRYGD

            val påklagetVedtakDetaljer =
                påklagetVedtakDetaljer(
                    "123",
                    vedtakstidspunkt = vedtakstidspunkt,
                    fagsystemType = FagsystemType.ORDNIÆR,
                )

            // Act
            val formkravAvvistBrev = brevInnholdUtleder.lagFormkravAvvistBrev(
                ident = ident,
                navn = navn,
                form = ikkeOppfyltForm(),
                stønadstype = stønadstype,
                påklagetVedtakDetaljer = påklagetVedtakDetaljer,
                fagsystem = Fagsystem.BA,
            )

            // Assert
            assertThat(formkravAvvistBrev.overskrift).isEqualTo("Vi har avvist klagen din på vedtaket om barnetrygd")
            assertThat(formkravAvvistBrev.personIdent).isEqualTo(ident)
            assertThat(formkravAvvistBrev.navn).isEqualTo(navn)
            assertThat(formkravAvvistBrev.avsnitt).hasSize(6)
            assertThat(formkravAvvistBrev.avsnitt.elementAt(0)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Vi har avvist klagen din fordi du har klaget på et vedtak som ikke gjelder deg.")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(1)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("brevtekst")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(2)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Vedtaket er gjort etter forvaltningsloven §§ 28 og 33.")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(3)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til å klage")
                assertThat(it.innhold).isEqualTo(
                    "Hvis du vil klage, må du gjøre dette innen 6 uker fra den datoen du fikk dette brevet. " +
                            "Du finner skjema og informasjon på ${klageUrls[stønadstype]}."
                )
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(4)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til innsyn i saken din")
                assertThat(it.innhold).isEqualTo(
                    "Du har rett til å se dokumentene i saken din. Dette følger av forvaltningsloven § 18. Kontakt oss om du vil se dokumentene i saken din. Ta kontakt på nav.no/kontakt eller på telefon 55 55 33 33 <34>. Du kan lese mer om innsynsretten på nav.no/personvernerklaering."
                )
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(5)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Har du spørsmål?")
                assertThat(it.innhold).isEqualTo(
                    "Du finner mer informasjon på ${lesMerUrls[stønadstype]}.\n\n" +
                            "På nav.no/kontakt kan du chatte eller skrive til oss.\n\n" +
                            "Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00-15.00."
                )
            })
        }

        @Test
        fun `skal utlede avvist brev om vedtak for BA uten påklaget vedtak detaljer`() {
            // Arrange
            val ident = "123456789"
            val navn = "Navn Navnesen"
            val stønadstype = Stønadstype.BARNETRYGD

            // Act
            val formkravAvvistBrev = brevInnholdUtleder.lagFormkravAvvistBrev(
                ident = ident,
                navn = navn,
                form = ikkeOppfyltForm(),
                stønadstype = stønadstype,
                påklagetVedtakDetaljer = null,
                fagsystem = Fagsystem.BA,
            )

            // Assert
            assertThat(formkravAvvistBrev.overskrift).isEqualTo("Vi har avvist klagen din på vedtaket om barnetrygd")
            assertThat(formkravAvvistBrev.personIdent).isEqualTo(ident)
            assertThat(formkravAvvistBrev.navn).isEqualTo(navn)
            assertThat(formkravAvvistBrev.avsnitt).hasSize(6)
            assertThat(formkravAvvistBrev.avsnitt.elementAt(0)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Vi har avvist klagen din fordi du har klaget på et vedtak som ikke gjelder deg.")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(1)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("brevtekst")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(2)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Vedtaket er gjort etter forvaltningsloven §§ 28 og 33.")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(3)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til å klage")
                assertThat(it.innhold).isEqualTo(
                    "Hvis du vil klage, må du gjøre dette innen 6 uker fra den datoen du fikk dette brevet. " +
                            "Du finner skjema og informasjon på ${klageUrls[stønadstype]}."
                )
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(4)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til innsyn i saken din")
                assertThat(it.innhold).isEqualTo(
                    "Du har rett til å se dokumentene i saken din. Dette følger av forvaltningsloven § 18. Kontakt oss om du vil se dokumentene i saken din. Ta kontakt på nav.no/kontakt eller på telefon 55 55 33 33 <34>. Du kan lese mer om innsynsretten på nav.no/personvernerklaering."
                )
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(5)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Har du spørsmål?")
                assertThat(it.innhold).isEqualTo(
                    "Du finner mer informasjon på ${lesMerUrls[stønadstype]}.\n\n" +
                            "På nav.no/kontakt kan du chatte eller skrive til oss.\n\n" +
                            "Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00-15.00."
                )
            })
        }

        @Test
        fun `skal utlede avvist brev om vedtak for KS`() {
            // Arrange
            val ident = "123456789"
            val navn = "Navn Navnesen"
            val stønadstype = Stønadstype.KONTANTSTØTTE

            val påklagetVedtakDetaljer =
                påklagetVedtakDetaljer(
                    "123",
                    vedtakstidspunkt = vedtakstidspunkt,
                    fagsystemType = FagsystemType.ORDNIÆR,
                )

            // Act
            val formkravAvvistBrev = brevInnholdUtleder.lagFormkravAvvistBrev(
                ident = ident,
                navn = navn,
                form = ikkeOppfyltForm(),
                stønadstype = stønadstype,
                påklagetVedtakDetaljer = påklagetVedtakDetaljer,
                fagsystem = Fagsystem.KS,
            )

            // Assert
            assertThat(formkravAvvistBrev.overskrift).isEqualTo("Vi har avvist klagen din på vedtaket om kontantstøtte")
            assertThat(formkravAvvistBrev.personIdent).isEqualTo(ident)
            assertThat(formkravAvvistBrev.navn).isEqualTo(navn)
            assertThat(formkravAvvistBrev.avsnitt).hasSize(6)
            assertThat(formkravAvvistBrev.avsnitt.elementAt(0)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Vi har avvist klagen din fordi du har klaget på et vedtak som ikke gjelder deg.")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(1)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("brevtekst")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(2)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Vedtaket er gjort etter forvaltningsloven §§ 28 og 33.")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(3)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til å klage")
                assertThat(it.innhold).isEqualTo(
                    "Hvis du vil klage, må du gjøre dette innen 6 uker fra den datoen du fikk dette brevet. " +
                            "Du finner skjema og informasjon på ${klageUrls[stønadstype]}."
                )
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(4)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til innsyn i saken din")
                assertThat(it.innhold).isEqualTo(
                    "Du har rett til å se dokumentene i saken din. Dette følger av forvaltningsloven § 18. Kontakt oss om du vil se dokumentene i saken din. Ta kontakt på nav.no/kontakt eller på telefon 55 55 33 33 <34>. Du kan lese mer om innsynsretten på nav.no/personvernerklaering."
                )
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(5)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Har du spørsmål?")
                assertThat(it.innhold).isEqualTo(
                    "Du finner mer informasjon på ${lesMerUrls[stønadstype]}.\n\n" +
                            "På nav.no/kontakt kan du chatte eller skrive til oss.\n\n" +
                            "Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00-15.00."
                )
            })
        }

        @Test
        fun `skal utlede avvist brev om vedtak for KS uten påklaget vedtak detaljer`() {
            // Arrange
            val ident = "123456789"
            val navn = "Navn Navnesen"
            val stønadstype = Stønadstype.KONTANTSTØTTE

            // Act
            val formkravAvvistBrev = brevInnholdUtleder.lagFormkravAvvistBrev(
                ident = ident,
                navn = navn,
                form = ikkeOppfyltForm(),
                stønadstype = stønadstype,
                påklagetVedtakDetaljer = null,
                fagsystem = Fagsystem.KS,
            )

            // Assert
            assertThat(formkravAvvistBrev.overskrift).isEqualTo("Vi har avvist klagen din på vedtaket om kontantstøtte")
            assertThat(formkravAvvistBrev.personIdent).isEqualTo(ident)
            assertThat(formkravAvvistBrev.navn).isEqualTo(navn)
            assertThat(formkravAvvistBrev.avsnitt).hasSize(6)
            assertThat(formkravAvvistBrev.avsnitt.elementAt(0)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Vi har avvist klagen din fordi du har klaget på et vedtak som ikke gjelder deg.")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(1)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("brevtekst")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(2)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Vedtaket er gjort etter forvaltningsloven §§ 28 og 33.")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(3)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til å klage")
                assertThat(it.innhold).isEqualTo(
                    "Hvis du vil klage, må du gjøre dette innen 6 uker fra den datoen du fikk dette brevet. " +
                            "Du finner skjema og informasjon på ${klageUrls[stønadstype]}."
                )
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(4)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til innsyn i saken din")
                assertThat(it.innhold).isEqualTo(
                    "Du har rett til å se dokumentene i saken din. Dette følger av forvaltningsloven § 18. Kontakt oss om du vil se dokumentene i saken din. Ta kontakt på nav.no/kontakt eller på telefon 55 55 33 33 <34>. Du kan lese mer om innsynsretten på nav.no/personvernerklaering."
                )
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(5)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Har du spørsmål?")
                assertThat(it.innhold).isEqualTo(
                    "Du finner mer informasjon på ${lesMerUrls[stønadstype]}.\n\n" +
                            "På nav.no/kontakt kan du chatte eller skrive til oss.\n\n" +
                            "Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00-15.00."
                )
            })
        }

        @ParameterizedTest
        @EnumSource(
            value = Stønadstype::class,
            names = ["BARNETRYGD", "KONTANTSTØTTE"],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `skal utlede avvist brev om vedtak for EF`(
            stønadstype: Stønadstype
        ) {
            // Arrange
            val ident = "123456789"
            val navn = "Navn Navnesen"

            val påklagetVedtakDetaljer =
                påklagetVedtakDetaljer(
                    "123",
                    vedtakstidspunkt = vedtakstidspunkt,
                )

            // Act
            val formkravAvvistBrev = brevInnholdUtleder.lagFormkravAvvistBrev(
                ident = ident,
                navn = navn,
                form = ikkeOppfyltForm(),
                stønadstype = stønadstype,
                påklagetVedtakDetaljer = påklagetVedtakDetaljer,
                fagsystem = Fagsystem.EF,
            )

            // Assert
            assertThat(formkravAvvistBrev.overskrift).isEqualTo("Vi har avvist klagen din på vedtaket om ${stønadstype.visningsnavn()}")
            assertThat(formkravAvvistBrev.overskrift).satisfiesAnyOf(
                { overskrift ->
                    assertThat(overskrift).isEqualTo("Vi har avvist klagen din på vedtaket om stønad til barnetilsyn")
                },
                { overskrift ->
                    assertThat(overskrift).isEqualTo("Vi har avvist klagen din på vedtaket om stønad til skolepenger")
                },
                { overskrift ->
                    assertThat(overskrift).isEqualTo("Vi har avvist klagen din på vedtaket om overgangsstønad")
                },
            )
            assertThat(formkravAvvistBrev.personIdent).isEqualTo(ident)
            assertThat(formkravAvvistBrev.navn).isEqualTo(navn)
            assertThat(formkravAvvistBrev.avsnitt).hasSize(6)
            assertThat(formkravAvvistBrev.avsnitt.elementAt(0)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Vi har avvist klagen din fordi du har klaget på et vedtak som ikke gjelder deg.")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(1)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("brevtekst")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(2)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Vedtaket er gjort etter forvaltningsloven §§ 28 og 33.")
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(3)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til å klage")
                assertThat(it.innhold).isEqualTo(
                    "Hvis du vil klage, må du gjøre dette innen 6 uker fra den datoen du fikk dette brevet. " +
                            "Du finner skjema og informasjon på ${klageUrls[stønadstype]}."
                )
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(4)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til innsyn")
                assertThat(it.innhold).isEqualTo(
                    "På nav.no/dittnav kan du se dokumentene i saken din."
                )
            })
            assertThat(formkravAvvistBrev.avsnitt.elementAt(5)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Har du spørsmål?")
                assertThat(it.innhold).isEqualTo(
                    "Du finner mer informasjon på ${lesMerUrls[stønadstype]}.\n\n" +
                            "På nav.no/kontakt kan du chatte eller skrive til oss.\n\n" +
                            "Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00-15.00."
                )
            })
        }
    }

    @Nested
    inner class LagFormkravAvvistBrevIkkePåklagetVedtak {
        @Test
        fun `skal kaste feil om brevtekst mangler i formkrav`() {
            // Arrange
            val ident = "123456789"
            val navn = "Navn Navnesen"

            // Act & assert
            val exception = assertThrows<IllegalStateException> {
                brevInnholdUtleder.lagFormkravAvvistBrevIkkePåklagetVedtak(
                    ident = ident,
                    navn = navn,
                    formkrav = ikkeOppfyltForm().copy(brevtekst = null),
                    stønadstype = Stønadstype.BARNETRYGD,
                )
            }
            assertThat(exception.message).isEqualTo("Må ha brevtekst fra saksbehandler for å generere brev ved formkrav ikke oppfylt")
        }

        @ParameterizedTest
        @EnumSource(
            value = Stønadstype::class,
            names = ["BARNETRYGD", "KONTANTSTØTTE"],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `skal utlede brev for avvist formkrav uten påklaget vedtak for EF`(
            stønadstype: Stønadstype,
        ) {
            // Arrange
            val ident = "123456789"
            val navn = "Navn Navnesen"

            // Act
            val brev =
                brevInnholdUtleder.lagFormkravAvvistBrevIkkePåklagetVedtak(
                    ident = ident,
                    navn = navn,
                    formkrav = ikkeOppfyltForm(),
                    stønadstype = stønadstype,
                )

            // Assert
            assertThat(brev.overskrift).isEqualTo("Vi har avvist klagen din")
            assertThat(brev.avsnitt).hasSize(6)
            assertThat(brev.avsnitt.elementAt(0)).satisfies({
                assertThat(it.innhold).isEqualTo("Vi har avvist klagen din fordi du ikke har klaget på et vedtak.")
            })
            assertThat(brev.avsnitt.elementAt(1)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("brevtekst")
            })
            assertThat(brev.avsnitt.elementAt(2)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Vedtaket er gjort etter forvaltningsloven §§ 28 og 33.")
            })
            assertThat(brev.avsnitt.elementAt(3)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til å klage")
                assertThat(it.innhold).isEqualTo(
                    "Hvis du vil klage, må du gjøre dette innen 6 uker fra den datoen du fikk dette brevet. " +
                            "Du finner skjema og informasjon på ${klageUrls[stønadstype]}.",
                )
            })
            assertThat(brev.avsnitt.elementAt(4)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til innsyn")
                assertThat(it.innhold).isEqualTo("På nav.no/dittnav kan du se dokumentene i saken din.")
            })
            assertThat(brev.avsnitt.elementAt(5)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Har du spørsmål?")
                assertThat(it.innhold).isEqualTo(
                    "Du finner mer informasjon på ${lesMerUrls[stønadstype]}.\n\n" +
                            "På nav.no/kontakt kan du chatte eller skrive til oss.\n\n" +
                            "Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00-15.00.",
                )
            })
        }

        @ParameterizedTest
        @EnumSource(
            value = Stønadstype::class,
            names = ["BARNETRYGD", "KONTANTSTØTTE"],
            mode = EnumSource.Mode.INCLUDE,
        )
        fun `skal utlede brev for avvist formkrav uten påklaget vedtak for BA og KS`(
            stønadstype: Stønadstype,
        ) {
            // Arrange
            val ident = "123456789"
            val navn = "Navn Navnesen"

            // Act
            val brev =
                brevInnholdUtleder.lagFormkravAvvistBrevIkkePåklagetVedtak(
                    ident = ident,
                    navn = navn,
                    formkrav = ikkeOppfyltForm(),
                    stønadstype = stønadstype,
                )

            // Assert
            assertThat(brev.overskrift).isEqualTo("Vi har avvist klagen din")
            assertThat(brev.avsnitt).hasSize(6)
            assertThat(brev.avsnitt.elementAt(0)).satisfies({
                assertThat(it.innhold).isEqualTo("Vi har avvist klagen din fordi du ikke har klaget på et vedtak.")
            })
            assertThat(brev.avsnitt.elementAt(1)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("brevtekst")
            })
            assertThat(brev.avsnitt.elementAt(2)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Vedtaket er gjort etter forvaltningsloven §§ 28 og 33.")
            })
            assertThat(brev.avsnitt.elementAt(3)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til å klage")
                assertThat(it.innhold).isEqualTo(
                    "Hvis du vil klage, må du gjøre dette innen 6 uker fra den datoen du fikk dette brevet. " +
                            "Du finner skjema og informasjon på ${klageUrls[stønadstype]}.",
                )
            })
            assertThat(brev.avsnitt.elementAt(4)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til innsyn i saken din")
                assertThat(it.innhold).isEqualTo(
                    "Du har rett til å se dokumentene i saken din. Dette følger av forvaltningsloven § 18. Kontakt oss om du vil se dokumentene i saken din. Ta kontakt på nav.no/kontakt eller på telefon 55 55 33 33 <34>. Du kan lese mer om innsynsretten på nav.no/personvernerklaering.",
                )
            })
            assertThat(brev.avsnitt.elementAt(5)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Har du spørsmål?")
                assertThat(it.innhold).isEqualTo(
                    "Du finner mer informasjon på ${lesMerUrls[stønadstype]}.\n\n" +
                            "På nav.no/kontakt kan du chatte eller skrive til oss.\n\n" +
                            "Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00-15.00.",
                )
            })
        }
    }

    @Nested
    inner class LagHenleggelsesbrevBaksInnhold {
        @ParameterizedTest
        @EnumSource(
            value = Stønadstype::class,
            names = ["BARNETRYGD", "KONTANTSTØTTE"],
            mode = EnumSource.Mode.INCLUDE,
        )
        fun `skal utlede brevinnhold for henleggelsesbrev for BA og KS`(
            stønadstype: Stønadstype,
        ) {
            // Arrange
            val ident = "123456789"
            val navn = "Navn Navnesen"

            // Act
            val henleggelsesbrevBaksInnhold = brevInnholdUtleder.lagHenleggelsesbrevBaksInnhold(
                ident = ident,
                navn = navn,
                stønadstype = stønadstype,
            )

            // Assert
            assertThat(henleggelsesbrevBaksInnhold.overskrift).isEqualTo("Saken din er avsluttet")
            assertThat(henleggelsesbrevBaksInnhold.personIdent).isEqualTo(ident)
            assertThat(henleggelsesbrevBaksInnhold.navn).isEqualTo(navn)
            assertThat(henleggelsesbrevBaksInnhold.avsnitt).hasSize(3)
            assertThat(henleggelsesbrevBaksInnhold.avsnitt.elementAt(0)).satisfies({
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Du har gitt oss beskjed om at du trekker klagen din på vedtaket om ${stønadstype.name.lowercase()}. Vi har derfor avsluttet saken din.")
            })
            assertThat(henleggelsesbrevBaksInnhold.avsnitt.elementAt(1)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Du har rett til innsyn i saken din")
                assertThat(it.innhold).isEqualTo("Du har rett til å se dokumentene i saken din. Dette følger av forvaltningsloven § 18. Kontakt oss om du vil se dokumentene i saken din. Ta kontakt på nav.no/kontakt eller på telefon 55 55 33 33 <34>. Du kan lese mer om innsynsretten på nav.no/personvernerklaering.")
            })
            assertThat(henleggelsesbrevBaksInnhold.avsnitt.elementAt(2)).satisfies({
                assertThat(it.deloverskrift).isEqualTo("Har du spørsmål?")
                assertThat(it.innhold).isEqualTo(
                    "Du finner mer informasjon på ${lesMerUrls[stønadstype]}.\n\n" +
                            "På nav.no/kontakt kan du chatte eller skrive til oss.\n\n" +
                            "Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00-15.00.",
                )
            })
        }

        @ParameterizedTest
        @EnumSource(
            value = Stønadstype::class,
            names = ["BARNETRYGD", "KONTANTSTØTTE"],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `skal utlede brevinnhold for henleggelsesbrev for EF`(
            stønadstype: Stønadstype,
        ) {
            // Arrange
            val ident = "123456789"
            val navn = "Navn Navnesen"

            // Act
            val henleggelsesbrevBaksInnhold = brevInnholdUtleder.lagHenleggelsesbrevBaksInnhold(
                ident = ident,
                navn = navn,
                stønadstype = stønadstype,
            )

            // Assert
            assertThat(henleggelsesbrevBaksInnhold.overskrift).isEqualTo("Saken din er avsluttet")
            assertThat(henleggelsesbrevBaksInnhold.personIdent).isEqualTo(ident)
            assertThat(henleggelsesbrevBaksInnhold.navn).isEqualTo(navn)
            assertThat(henleggelsesbrevBaksInnhold.avsnitt).hasSize(2)
            assertThat(henleggelsesbrevBaksInnhold.avsnitt).anySatisfy {
                assertThat(it.deloverskrift).isEmpty()
                assertThat(it.innhold).isEqualTo("Du har gitt oss beskjed om at du trekker klagen din på vedtaket om ${stønadstype.name.lowercase()}. Vi har derfor avsluttet saken din.")
            }
            assertThat(henleggelsesbrevBaksInnhold.avsnitt).anySatisfy {
                assertThat(it.deloverskrift).isEqualTo("Har du spørsmål?")
                assertThat(it.innhold).isEqualTo(
                    "Du finner mer informasjon på ${lesMerUrls[stønadstype]}.\n\n" +
                            "På nav.no/kontakt kan du chatte eller skrive til oss.\n\n" +
                            "Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00-15.00.",
                )
            }
        }
    }

    private fun ikkeOppfyltForm() =
        oppfyltForm(
            behandlingId = UUID.randomUUID()
        ).copy(
            klagePart = FormVilkår.IKKE_OPPFYLT,
            brevtekst = "brevtekst"
        )
}
