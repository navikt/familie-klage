package no.nav.familie.klage.brev

import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.domain.FormVilkår.IKKE_OPPFYLT
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.infrastruktur.exception.feilHvis

object FormBrevUtil {

    fun utledIkkeOppfylteFormkrav(formkrav: Form): Set<FormkravVilkår> {
        return setOf(
            if (formkrav.klagePart == IKKE_OPPFYLT) FormkravVilkår.KLAGE_PART else null,
            if (formkrav.klageKonkret == IKKE_OPPFYLT) FormkravVilkår.KLAGE_KONKRET else null,
            if (formkrav.klageSignert == IKKE_OPPFYLT) FormkravVilkår.KLAGE_SIGNERT else null,
            if (formkrav.klagefristOverholdt == IKKE_OPPFYLT) FormkravVilkår.KLAGEFRIST_OVERHOLDT else null
        ).filterNotNull().toSet()
    }

    fun utledÅrsakTilAvvisningstekst(formkravVilkår: Set<FormkravVilkår>): String {
        feilHvis(formkravVilkår.isEmpty()) {
            "Skal ikke kunne utlede innholdstekst til formkrav avvist brev uten ikke oppfylte formkrav"
        }
        if (formkravVilkår.size > 1) {
            return "$innholdstekstPrefix ${formkravVilkår.joinToString("") { "\n  •  ${it.tekst}" }}"
        } else {
            return "$innholdstekstPrefix ${formkravVilkår.single().tekst}."
        }
    }

    fun utledLovtekst(formkravVilkår: Set<FormkravVilkår>): String {
        val folketrygdloven = formkravVilkår.flatMap { it.folketrygdLoven }.sorted().toSet()
        val forvaltningsloven = formkravVilkår.flatMap { it.forvaltningsloven }.sorted().toSet()
        val harFolketrygdlov = folketrygdloven.isNotEmpty()
        val harForvaltningslov = forvaltningsloven.isNotEmpty()

        return if (harFolketrygdlov && harForvaltningslov) {
            "Vedtaket er gjort etter folketrygdloven ${utledParagrafer(folketrygdloven)} og forvaltningsloven ${
            utledParagrafer(forvaltningsloven)
            }."
        } else if (harFolketrygdlov) {
            "Vedtaket er gjort etter folketrygdloven ${utledParagrafer(folketrygdloven)}."
        } else if (harForvaltningslov) {
            "Vedtaket er gjort etter forvaltningsloven ${utledParagrafer(forvaltningsloven)}."
        } else {
            throw Feil("Har ingen paragrafer å utlede i vedtaksbrev ved formkrav avvist")
        }
    }

    private fun utledParagrafer(paragrafer: Set<String>): String {
        return if (paragrafer.size == 1) {
            "§ ${paragrafer.first()}"
        } else {
            val alleUnntattSiste = paragrafer.toList().dropLast(1)
            val siste = paragrafer.toList().last()
            "§§ ${alleUnntattSiste.joinToString { it }} og $siste"
        }
    }

    const val innholdstekstPrefix = "Vi har avvist klagen din fordi"

    enum class FormkravVilkår(val tekst: String, val folketrygdLoven: Set<String>, val forvaltningsloven: Set<String>) {
        KLAGE_KONKRET("du ikke har sagt hva du klager på", emptySet(), setOf("32", "33")),
        KLAGE_PART("du har klaget på et vedtak som ikke gjelder deg", emptySet(), setOf("28", "33")),
        KLAGE_SIGNERT("du ikke har underskrevet den", emptySet(), setOf("31", "33")),
        KLAGEFRIST_OVERHOLDT("du har klaget for sent", setOf("21-12"), setOf("31", "33"))
    }
}
