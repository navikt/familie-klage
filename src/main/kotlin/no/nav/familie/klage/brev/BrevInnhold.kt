package no.nav.familie.klage.brev

import no.nav.familie.klage.brev.FormBrevUtil.utledIkkeOppfylteFormkrav
import no.nav.familie.klage.brev.FormBrevUtil.utledÅrsakTilAvvisningstekst
import no.nav.familie.klage.brev.FormBrevUtil.utledLovtekst
import no.nav.familie.klage.brev.dto.AvsnittDto
import no.nav.familie.klage.brev.dto.FritekstBrevRequestDto
import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.kontrakter.felles.klage.Stønadstype

object BrevInnhold {

    fun lagOpprettholdelseBrev(
        ident: String,
        instillingKlageinstans: String,
        navn: String,
        stønadstype: Stønadstype
    ): FritekstBrevRequestDto {

        return FritekstBrevRequestDto(
            overskrift = "Vi har sendt klagen din til NAV Klageinstans",
            navn = navn,
            personIdent = ident,
            avsnitt =
            listOf(
                AvsnittDto(
                    deloverskrift = "",
                    innhold =
                    "Vi har fått klagen din på vedtaket om ${stønadstype.tilVisningsnavn()}, og kommet frem til at det ikke endres. NAV Klageinstans skal derfor vurdere saken din på nytt."
                ),
                AvsnittDto(
                    deloverskrift = "",
                    innhold = "Saksbehandlingstidene finner du på nav.no/saksbehandlingstider."
                ),
                AvsnittDto(
                    deloverskrift = "Dette er vurderingen vi har sendt til NAV Klageinstans",
                    innhold = instillingKlageinstans
                ),
                AvsnittDto(
                    deloverskrift = "",
                    innhold =
                    "Har du nye opplysninger eller ønsker å uttale deg, kan du sende oss dette via nav.no/klage."
                ),
                AvsnittDto(
                    deloverskrift = "Har du spørsmål?",
                    innhold = "Du finner informasjon som kan være nyttig for deg på ${stønadstype.lesMerUrl()}. Du kan også kontakte oss på nav.no/kontakt."
                )
            )
        )
    }

    fun lagFormkravAvvistBrev(
            ident: String,
            navn: String,
            formkrav: Form,
            stønadstype: Stønadstype
    ): FritekstBrevRequestDto {
        val ikkeOppfylteFormkrav = utledIkkeOppfylteFormkrav(formkrav)
        val brevtekstFraSaksbehandler = formkrav.brevtekst ?: error("Må ha brevtekst fra saksbehandler for å generere brev ved formkrav ikke oppfylt")

        return FritekstBrevRequestDto(
            overskrift = "Vi har avvist klagen din på vedtaket om ${stønadstype.tilVisningsnavn()}",
            personIdent = ident,
            navn = navn,
            avsnitt =
            listOf(
                AvsnittDto(
                    deloverskrift = "",
                    innhold = utledÅrsakTilAvvisningstekst(ikkeOppfylteFormkrav)
                ),
                AvsnittDto(
                        deloverskrift = "",
                        innhold = brevtekstFraSaksbehandler
                ),
                AvsnittDto(
                        deloverskrift = "",
                        innhold = utledLovtekst(ikkeOppfylteFormkrav)
                ),
                AvsnittDto(
                    deloverskrift = "Du har rett til å klage",
                    innhold =
                    "Hvis du vil klage, må du gjøre dette innen 3 uker fra den datoen du fikk dette brevet. Du finner skjema og informasjon på nav.no/klage."
                ),
                AvsnittDto(
                    deloverskrift = "Du har rett til innsyn",
                    innhold =
                    "På nav.no/dittnav kan du se dokumentene i saken din."
                ),
                AvsnittDto(
                        deloverskrift = "Har du spørsmål?",
                        innhold =
                        "Du finner informasjon som kan være nyttig for deg på ${stønadstype.lesMerUrl()}. Du kan også kontakte oss på nav.no/kontakt."
                )
            )
        )
    }

    private fun Stønadstype.tilVisningsnavn() = this.name.lowercase()

    private fun Stønadstype.lesMerUrl() = when (this) {
        Stønadstype.OVERGANGSSTØNAD,
        Stønadstype.BARNETILSYN,
        Stønadstype.SKOLEPENGER -> "nav.no/familie/alene-med-barn"
        Stønadstype.BARNETRYGD -> "nav.no/barnetrygd"
        Stønadstype.KONTANTSTØTTE -> "nav.no/kontantstotte"
    }
}
