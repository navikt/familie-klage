package no.nav.familie.klage.brev

import no.nav.familie.klage.behandling.domain.PåklagetVedtakDetaljer
import no.nav.familie.klage.brev.avvistbrev.AvvistBrevInnholdUtleder
import no.nav.familie.klage.brev.dto.AvsnittDto
import no.nav.familie.klage.brev.dto.FritekstBrevRequestDto
import no.nav.familie.klage.felles.util.StønadstypeVisningsnavn.visningsnavn
import no.nav.familie.klage.felles.util.TekstUtil.norskFormat
import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.FagsystemType
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class BrevInnholdUtleder(
    private val avvistBrevInnholdUtlederLookup: AvvistBrevInnholdUtleder.Lookup,
) {
    fun lagOpprettholdelseBrev(
        ident: String,
        instillingKlageinstans: String,
        navn: String,
        stønadstype: Stønadstype,
        påklagetVedtakDetaljer: PåklagetVedtakDetaljer,
        klageMottatt: LocalDate,
    ): FritekstBrevRequestDto =
        FritekstBrevRequestDto(
            overskrift = "Vi har sendt klagen din til Nav Klageinstans Nord",
            navn = navn,
            personIdent = ident,
            avsnitt =
                listOf(
                    AvsnittDto(
                        deloverskrift = "",
                        innhold =
                            "Vi har ${klageMottatt.norskFormat()} fått klagen din på vedtaket om " +
                                "${visningsnavn(stønadstype, påklagetVedtakDetaljer)} som ble gjort " +
                                "${påklagetVedtakDetaljer.vedtakstidspunkt.norskFormat()}, " +
                                "og kommet frem til at vi ikke endrer vedtaket. Nav Klageinstans skal derfor vurdere saken din på nytt.",
                    ),
                    AvsnittDto(
                        deloverskrift = "",
                        innhold = "Saksbehandlingstidene finner du på nav.no/saksbehandlingstider.",
                    ),
                    AvsnittDto(
                        deloverskrift = "Dette er vurderingen vi har sendt til Nav Klageinstans",
                        innhold = instillingKlageinstans,
                    ),
                    AvsnittDto(
                        deloverskrift = "Har du nye opplysninger?",
                        innhold =
                            "Har du nye opplysninger eller ønsker å uttale deg, kan du sende oss dette via \n${stønadstype.klageUrl()}.",
                    ),
                    harDuSpørsmålAvsnitt(stønadstype),
                ),
        )

    fun lagOpprettholdelseBrev(
        ident: String,
        saksbehandlerFritekst: String?,
        dokumentasjonOgUtredning: String,
        spørsmåletISaken: String,
        aktuelleRettskilder: String,
        klagersAnførsler: String,
        vurderingAvKlagen: String,
        navn: String,
        stønadstype: Stønadstype,
        påklagetVedtakDetaljer: PåklagetVedtakDetaljer,
        klageMottatt: LocalDate,
    ): FritekstBrevRequestDto =
        FritekstBrevRequestDto(
            overskrift = "Vi har sendt klagen din til Nav Klageinstans Nord",
            navn = navn,
            personIdent = ident,
            avsnitt =
                listOfNotNull(
                    AvsnittDto(
                        deloverskrift = "",
                        innhold =
                            "Vi har ${klageMottatt.norskFormat()} fått klagen din på vedtaket om " +
                                "${visningsnavn(stønadstype, påklagetVedtakDetaljer)} som ble gjort " +
                                "${påklagetVedtakDetaljer.vedtakstidspunkt.norskFormat()}, " +
                                "og kommet frem til at vi ikke endrer vedtaket. Nav Klageinstans skal derfor vurdere saken din på nytt.",
                    ),
                    saksbehandlerFritekst?.let {
                        AvsnittDto(
                            deloverskrift = "",
                            innhold = saksbehandlerFritekst,
                        )
                    },
                    AvsnittDto(
                        deloverskrift = "",
                        innhold = "Saksbehandlingstidene finner du på nav.no/saksbehandlingstider.",
                    ),
                    AvsnittDto(
                        deloverskrift = "Dette er vurderingen vi har sendt til Nav Klageinstans",
                        innhold = "",
                    ),
                    AvsnittDto(
                        deloverskrift = "Dokumentasjon og utredning",
                        innhold = dokumentasjonOgUtredning,
                    ),
                    AvsnittDto(
                        deloverskrift = "Spørsmålet i saken",
                        innhold = spørsmåletISaken,
                    ),
                    AvsnittDto(
                        deloverskrift = "Aktuelle rettskilder",
                        innhold = aktuelleRettskilder,
                    ),
                    AvsnittDto(
                        deloverskrift = "Klagers anførsler",
                        innhold = klagersAnførsler,
                    ),
                    AvsnittDto(
                        deloverskrift = "Vurdering av klagen",
                        innhold = vurderingAvKlagen,
                    ),
                    AvsnittDto(
                        deloverskrift = "Har du nye opplysninger?",
                        innhold =
                            "Har du nye opplysninger eller ønsker å uttale deg, kan du sende oss dette via \n${stønadstype.klageUrl()}.",
                    ),
                    harDuSpørsmålAvsnitt(stønadstype),
                ),
        )

    fun lagFormkravAvvistBrev(
        ident: String,
        navn: String,
        form: Form,
        stønadstype: Stønadstype,
        påklagetVedtakDetaljer: PåklagetVedtakDetaljer?,
        fagsystem: Fagsystem,
    ): FritekstBrevRequestDto {
        val avvistBrevUtleder = avvistBrevInnholdUtlederLookup.hentAvvistBrevUtlederForFagsystem(fagsystem)
        val avvistBrevInnhold = avvistBrevUtleder.utledBrevInnhold(form)

        return FritekstBrevRequestDto(
            overskrift = "Vi har avvist klagen din på vedtaket om ${visningsnavn(stønadstype, påklagetVedtakDetaljer)}",
            personIdent = ident,
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
                    duHarRettTilÅKlageAvsnitt(stønadstype),
                    AvsnittDto(
                        deloverskrift = "Du har rett til innsyn",
                        innhold =
                            "På nav.no/dittnav kan du se dokumentene i saken din.",
                    ),
                    harDuSpørsmålAvsnitt(stønadstype),
                ),
        )
    }

    fun lagFormkravAvvistBrevIkkePåklagetVedtak(
        ident: String,
        navn: String,
        formkrav: Form,
        stønadstype: Stønadstype,
    ): FritekstBrevRequestDto {
        val brevtekstFraSaksbehandler =
            formkrav.brevtekst ?: error("Må ha brevtekst fra saksbehandler for å generere brev ved formkrav ikke oppfylt")

        return FritekstBrevRequestDto(
            overskrift = "Vi har avvist klagen din",
            personIdent = ident,
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
                    duHarRettTilÅKlageAvsnitt(stønadstype),
                    AvsnittDto(
                        deloverskrift = "Du har rett til innsyn",
                        innhold =
                            "På nav.no/dittnav kan du se dokumentene i saken din.",
                    ),
                    harDuSpørsmålAvsnitt(stønadstype),
                ),
        )
    }

    fun lagHenleggelsesbrevBaksInnhold(
        ident: String,
        navn: String,
        stønadstype: Stønadstype,
    ): FritekstBrevRequestDto =
        FritekstBrevRequestDto(
            overskrift = "Saken din er avsluttet",
            personIdent = ident,
            navn = navn,
            avsnitt =
                listOf(
                    AvsnittDto(
                        deloverskrift = "",
                        innhold =
                            "Du har gitt oss beskjed om at du trekker klagen din på vedtaket om ${stønadstype.name.lowercase()}." +
                                "Vi har derfor avsluttet saken din.",
                    ),
                    harDuSpørsmålAvsnitt(stønadstype),
                ),
        )

    private fun duHarRettTilÅKlageAvsnitt(stønadstype: Stønadstype) =
        AvsnittDto(
            deloverskrift = "Du har rett til å klage",
            innhold =
                "Hvis du vil klage, må du gjøre dette innen 6 uker fra den datoen du fikk dette brevet. " +
                    "Du finner skjema og informasjon på ${stønadstype.klageUrl()}.",
        )

    private fun harDuSpørsmålAvsnitt(stønadstype: Stønadstype) =
        AvsnittDto(
            deloverskrift = "Har du spørsmål?",
            innhold =
                "Du finner mer informasjon på ${stønadstype.lesMerUrl()}.\n\n" +
                    "På nav.no/kontakt kan du chatte eller skrive til oss.\n\n" +
                    "Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00-15.00.",
        )

    private fun visningsnavn(
        stønadstype: Stønadstype,
        påklagetVedtakDetaljer: PåklagetVedtakDetaljer?,
    ): String =
        when (påklagetVedtakDetaljer?.fagsystemType) {
            FagsystemType.TILBAKEKREVING -> "tilbakebetaling av ${stønadstype.visningsnavn()}"
            FagsystemType.SANKSJON_1_MND -> "sanksjon"
            FagsystemType.UTESTENGELSE -> "utestengelse"
            else ->
                stønadstype.visningsnavn()
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
}
