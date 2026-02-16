package no.nav.familie.klage.brev

import no.nav.familie.klage.behandling.domain.PåklagetVedtakDetaljer
import no.nav.familie.klage.brev.avvistbrev.AvvistBrevInnholdUtleder
import no.nav.familie.klage.brev.dto.AvsnittDto
import no.nav.familie.klage.brev.dto.FritekstBrevRequestDto
import no.nav.familie.klage.brev.dto.Heading
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.felles.util.StønadstypeVisningsnavn.visningsnavn
import no.nav.familie.klage.felles.util.TekstUtil.norskFormat
import no.nav.familie.klage.felles.util.TekstUtil.norskFormatLang
import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.vurdering.VurderingValidator.validerVurdering
import no.nav.familie.klage.vurdering.domain.Vurdering
import no.nav.familie.klage.vurdering.dto.tilDto
import no.nav.familie.kontrakter.felles.klage.FagsystemType
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class BrevInnholdUtleder(
    private val avvistBrevInnholdUtlederLookup: AvvistBrevInnholdUtleder.Lookup,
) {
    fun lagOpprettholdelseBrev(
        fagsak: Fagsak,
        innstillingKlageinstans: String,
        navn: String,
        påklagetVedtakDetaljer: PåklagetVedtakDetaljer,
        klageMottatt: LocalDate,
    ): FritekstBrevRequestDto =
        FritekstBrevRequestDto(
            overskrift = "Vi har sendt klagen din til Nav Klageinstans Nord",
            navn = navn,
            personIdent = fagsak.hentAktivIdent(),
            avsnitt =
                listOf(
                    AvsnittDto(
                        deloverskrift = "",
                        innhold =
                            "Vi har ${klageMottatt.norskFormat()} fått klagen din på vedtaket om " +
                                "${visningsnavn(fagsak.stønadstype, påklagetVedtakDetaljer)} som ble gjort " +
                                "${påklagetVedtakDetaljer.vedtakstidspunkt.norskFormat()}, " +
                                "og kommet frem til at vi ikke endrer vedtaket. Nav Klageinstans skal derfor vurdere saken din på nytt.",
                    ),
                    AvsnittDto(
                        deloverskrift = "",
                        innhold = "Saksbehandlingstidene finner du på nav.no/saksbehandlingstider.",
                    ),
                    AvsnittDto(
                        deloverskrift = "Dette er vurderingen vi har sendt til Nav Klageinstans",
                        innhold = innstillingKlageinstans,
                    ),
                    harDuNyeOpplysningerAvsnitt(fagsak),
                    harDuSpørsmålAvsnitt(fagsak),
                ),
        )

    fun lagOpprettholdelseBrev(
        fagsak: Fagsak,
        klagefristUnntakBegrunnelse: String?,
        vurdering: Vurdering,
        navn: String,
        påklagetVedtakDetaljer: PåklagetVedtakDetaljer,
        klageMottatt: LocalDate,
    ): FritekstBrevRequestDto {
        validerVurdering(vurdering.tilDto(), fagsak.fagsystem)
        return FritekstBrevRequestDto(
            overskrift = "Vi har sendt klagen din til Nav Klageinstans Nord",
            navn = navn,
            personIdent = fagsak.hentAktivIdent(),
            avsnitt =
                listOfNotNull(
                    AvsnittDto(
                        deloverskrift = "",
                        innhold =
                            "Vi har ${klageMottatt.norskFormatLang()} fått klagen din på vedtaket om " +
                                "${visningsnavn(fagsak.stønadstype, påklagetVedtakDetaljer)} som ble gjort " +
                                "${påklagetVedtakDetaljer.vedtakstidspunkt.norskFormatLang()}, " +
                                "og kommet frem til at vi ikke endrer vedtaket. Nav Klageinstans skal derfor vurdere saken din på nytt.",
                    ),
                    AvsnittDto(
                        deloverskrift = "",
                        innhold = "Saksbehandlingstidene finner du på nav.no/saksbehandlingstider.",
                    ),
                    AvsnittDto(
                        deloverskrift = "Dette er vurderingen vi har sendt til Nav Klageinstans",
                        deloverskriftHeading = Heading.H2,
                        innhold = "",
                    ),
                    AvsnittDto(
                        deloverskrift = "Dokumentasjon og utredning",
                        deloverskriftHeading = Heading.H3,
                        innhold = klagefristUnntakBegrunnelse ?: vurdering.dokumentasjonOgUtredning!!,
                    ),
                    klagefristUnntakBegrunnelse?.let {
                        AvsnittDto(
                            deloverskrift = "",
                            innhold = vurdering.dokumentasjonOgUtredning!!,
                        )
                    },
                    AvsnittDto(
                        deloverskrift = "Spørsmålet i saken",
                        deloverskriftHeading = Heading.H3,
                        innhold = vurdering.spørsmåletISaken!!,
                    ),
                    AvsnittDto(
                        deloverskrift = "Aktuelle rettskilder",
                        deloverskriftHeading = Heading.H3,
                        innhold = vurdering.aktuelleRettskilder!!,
                    ),
                    AvsnittDto(
                        deloverskrift = "Klagers anførsler",
                        deloverskriftHeading = Heading.H3,
                        innhold = vurdering.klagersAnførsler!!,
                    ),
                    AvsnittDto(
                        deloverskrift = "Vurdering av klagen",
                        deloverskriftHeading = Heading.H3,
                        innhold = vurdering.vurderingAvKlagen!!,
                    ),
                    harDuNyeOpplysningerAvsnitt(fagsak),
                    duHarRettTilInnsynAvsnitt(fagsak),
                    harDuSpørsmålAvsnitt(fagsak),
                ),
        )
    }

    fun lagFormkravAvvistBrev(
        fagsak: Fagsak,
        navn: String,
        form: Form,
        påklagetVedtakDetaljer: PåklagetVedtakDetaljer?,
    ): FritekstBrevRequestDto {
        val avvistBrevUtleder = avvistBrevInnholdUtlederLookup.hentAvvistBrevUtlederForFagsystem(fagsak.fagsystem)
        val avvistBrevInnhold = avvistBrevUtleder.utledBrevInnhold(form)

        return FritekstBrevRequestDto(
            overskrift = "Vi har avvist klagen din på vedtaket om ${visningsnavn(fagsak.stønadstype, påklagetVedtakDetaljer)}",
            personIdent = fagsak.hentAktivIdent(),
            navn = navn,
            avsnitt =
                listOf(
                    AvsnittDto(
                        deloverskrift = "",
                        innhold = avvistBrevInnhold.årsakTilAvvisning,
                    ),
                    AvsnittDto(
                        deloverskrift = "",
                        innhold = avvistBrevInnhold.brevtekstFraSaksbehandler,
                    ),
                    AvsnittDto(
                        deloverskrift = "",
                        innhold = avvistBrevInnhold.lovtekst,
                    ),
                    duHarRettTilÅKlageAvsnitt(fagsak),
                    duHarRettTilInnsynAvsnitt(fagsak),
                    harDuSpørsmålAvsnitt(fagsak),
                ),
        )
    }

    fun lagFormkravAvvistBrevIkkePåklagetVedtak(
        fagsak: Fagsak,
        navn: String,
        formkrav: Form,
    ): FritekstBrevRequestDto {
        val brevtekstFraSaksbehandler =
            formkrav.brevtekst ?: error("Må ha brevtekst fra saksbehandler for å generere brev ved formkrav ikke oppfylt")

        return FritekstBrevRequestDto(
            overskrift = "Vi har avvist klagen din",
            personIdent = fagsak.hentAktivIdent(),
            navn = navn,
            avsnitt =
                listOf(
                    AvsnittDto(
                        deloverskrift = "",
                        innhold = "Vi har avvist klagen din fordi du ikke har klaget på et vedtak.",
                    ),
                    AvsnittDto(
                        deloverskrift = "",
                        innhold = brevtekstFraSaksbehandler,
                    ),
                    AvsnittDto(
                        deloverskrift = "",
                        innhold = "Vedtaket er gjort etter forvaltningsloven §§ 28 og 33.",
                    ),
                    duHarRettTilÅKlageAvsnitt(fagsak),
                    duHarRettTilInnsynAvsnitt(fagsak),
                    harDuSpørsmålAvsnitt(fagsak),
                ),
        )
    }

    fun lagHenleggelsesbrevBaksInnhold(
        fagsak: Fagsak,
        navn: String,
    ): FritekstBrevRequestDto =
        FritekstBrevRequestDto(
            overskrift = "Saken din er avsluttet",
            personIdent = fagsak.hentAktivIdent(),
            navn = navn,
            avsnitt =
                listOfNotNull(
                    AvsnittDto(
                        deloverskrift = "",
                        innhold =
                            "Du har trukket klagen din på vedtaket om " +
                                "${fagsak.stønadstype.name.lowercase()}. Vi har derfor avsluttet saken din.",
                    ),
                    duHarRettTilInnsynAvsnitt(fagsak),
                    harDuSpørsmålAvsnitt(fagsak),
                ),
        )

    private fun duHarRettTilÅKlageAvsnitt(fagsak: Fagsak): AvsnittDto {
        val stønadstype = fagsak.stønadstype
        return AvsnittDto(
            deloverskrift = "Du har rett til å klage",
            deloverskriftHeading = utledDeloverskriftHeading(stønadstype),
            innhold =
                "Hvis du vil klage, må du gjøre dette innen ${utledKlagefrist(stønadstype)} uker fra den datoen du fikk dette brevet. " +
                    "Du finner skjema og informasjon på ${stønadstype.klageUrl()}.",
        )
    }

    private fun utledKlagefrist(stønadstype: Stønadstype): Int =
        when (stønadstype) {
            Stønadstype.KONTANTSTØTTE -> 3
            else -> 6
        }

    private fun harDuNyeOpplysningerAvsnitt(fagsak: Fagsak): AvsnittDto {
        val stønadstype = fagsak.stønadstype
        return AvsnittDto(
            deloverskrift = "Har du nye opplysninger?",
            deloverskriftHeading = utledDeloverskriftHeading(stønadstype),
            innhold = "Har du nye opplysninger eller ønsker å uttale deg, kan du sende oss dette via \n${stønadstype.klageUrl()}.",
        )
    }

    private fun duHarRettTilInnsynAvsnitt(fagsak: Fagsak): AvsnittDto {
        val stønadstype = fagsak.stønadstype
        return if (stønadstype.erBarnetrygdEllerKontantstøtte()) {
            AvsnittDto(
                deloverskrift = "Du har rett til innsyn i saken din",
                deloverskriftHeading = utledDeloverskriftHeading(stønadstype),
                innhold =
                    "Du har rett til å se dokumentene i saken din. Dette følger av forvaltningsloven § 18. " +
                        "Kontakt oss om du vil se dokumentene i saken din. Ta kontakt på nav.no/kontakt eller på telefon " +
                            "55 55 33 33. Du kan lese mer om innsynsretten på nav.no/personvernerklaering.",
            )
        } else {
            AvsnittDto(
                deloverskrift = "Du har rett til innsyn",
                innhold = "På nav.no/dittnav kan du se dokumentene i saken din.",
            )
        }
    }

    private fun harDuSpørsmålAvsnitt(fagsak: Fagsak): AvsnittDto {
        val stønadstype = fagsak.stønadstype
        return AvsnittDto(
            deloverskrift = "Har du spørsmål?",
            deloverskriftHeading = utledDeloverskriftHeading(stønadstype),
            innhold =
                if (stønadstype.erBarnetrygdEllerKontantstøtte()) {
                    "Du finner mer informasjon på ${stønadstype.lesMerUrl()}. " +
                        "På nav.no/kontakt kan du chatte eller skrive til oss. " +
                        "Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00-15.00."
                } else {
                    "Du finner mer informasjon på ${stønadstype.lesMerUrl()}.\n\n" +
                        "På nav.no/kontakt kan du chatte eller skrive til oss.\n\n" +
                        "Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00-15.00."
                },
        )
    }

    private fun visningsnavn(
        stønadstype: Stønadstype,
        påklagetVedtakDetaljer: PåklagetVedtakDetaljer?,
    ): String =
        when (påklagetVedtakDetaljer?.fagsystemType) {
            FagsystemType.TILBAKEKREVING -> {
                "tilbakebetaling av ${stønadstype.visningsnavn()}"
            }

            FagsystemType.SANKSJON_1_MND -> {
                "sanksjon"
            }

            FagsystemType.UTESTENGELSE -> {
                "utestengelse"
            }

            else -> {
                stønadstype.visningsnavn()
            }
        }

    private fun utledDeloverskriftHeading(stønadstype: Stønadstype) =
        if (stønadstype.erBarnetrygdEllerKontantstøtte()) {
            Heading.H2
        } else {
            null
        }

    private fun Stønadstype.lesMerUrl() =
        when (this) {
            Stønadstype.OVERGANGSSTØNAD,
            Stønadstype.BARNETILSYN,
            Stønadstype.SKOLEPENGER,
            -> "nav.no/alene-med-barn"

            Stønadstype.BARNETRYGD -> "nav.no/barnetrygd"

            Stønadstype.KONTANTSTØTTE -> "nav.no/kontantstotte"
        }

    private fun Stønadstype.klageUrl() =
        when (this) {
            Stønadstype.OVERGANGSSTØNAD,
            -> "nav.no/klage#overgangsstonad-til-enslig-mor-eller-far"

            Stønadstype.BARNETILSYN,
            -> "nav.no/klage#stonad-til-barnetilsyn-for-enslig-mor-eller-far"

            Stønadstype.SKOLEPENGER,
            -> "nav.no/klage#stonad-til-skolepenger-for-enslig-mor-eller-far"

            Stønadstype.BARNETRYGD -> "nav.no/klage#barnetrygd"

            Stønadstype.KONTANTSTØTTE -> "nav.no/klage#kontantstotte"
        }

    private fun Stønadstype.erBarnetrygdEllerKontantstøtte() = this in setOf(Stønadstype.BARNETRYGD, Stønadstype.KONTANTSTØTTE)
}
