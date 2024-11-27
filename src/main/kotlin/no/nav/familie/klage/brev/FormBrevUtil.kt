package no.nav.familie.klage.brev

import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.domain.FormVilkår.IKKE_OPPFYLT
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.infrastruktur.exception.feilHvis
import no.nav.familie.kontrakter.felles.klage.Fagsystem

object FormBrevUtil {

    fun utledIkkeOppfylteFormkrav(formkrav: Form): Set<Formkrav> {
        return setOf(
            if (formkrav.klagePart == IKKE_OPPFYLT) Formkrav.KLAGE_PART else null,
            if (formkrav.klageKonkret == IKKE_OPPFYLT) Formkrav.KLAGE_KONKRET else null,
            if (formkrav.klageSignert == IKKE_OPPFYLT) Formkrav.KLAGE_SIGNERT else null,
            if (formkrav.klagefristOverholdt == IKKE_OPPFYLT) Formkrav.KLAGEFRIST_OVERHOLDT else null,
        ).filterNotNull().toSet()
    }

    fun utledÅrsakTilAvvisningstekst(formkrav: Set<Formkrav>): String {
        feilHvis(formkrav.isEmpty()) {
            "Skal ikke kunne utlede innholdstekst til formkrav avvist brev uten ikke oppfylte formkrav"
        }
        if (formkrav.size > 1) {
            return "$innholdstekstPrefix ${formkrav.joinToString("") { "\n  •  ${it.tekst}" }}"
        } else {
            return "$innholdstekstPrefix ${formkrav.single().tekst}."
        }
    }

    fun utledLovtekst(formkrav: Set<Formkrav>, fagsystem: Fagsystem): String =
        when(fagsystem) {
            Fagsystem.EF -> utledLovtekstEF(formkrav)
            Fagsystem.BA -> utledLovtekstBA(formkrav)
            Fagsystem.KS -> utledLovtekstKS(formkrav)
        }
    }

    fun utledLovtekstEF(formkrav: Set<Formkrav>): String {
        val formkravVilkårEF: Set<FormkravVilkårEF> = formkrav.tilFormkravVilkårEF()
        val folketrygdloven = formkravVilkårEF.flatMap { it.folketrygdLoven }.sorted().toSet()
        val forvaltningsloven = formkravVilkårEF.flatMap { it.forvaltningsloven }.sorted().toSet()
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

    fun utledLovtekstBA(formkrav: Set<Formkrav>): String {
        val formkravVilkårBA: Set<FormkravVilkårBA> = formkrav.tilFormkravVilkårBA()
        val barnetrygdloven = formkravVilkårBA.flatMap { it.barnetrygdloven }.sorted().toSet()
        val forvaltningsloven = formkravVilkårBA.flatMap { it.forvaltningsloven }.sorted().toSet()
        val harBarnetrygdlov = barnetrygdloven.isNotEmpty()
        val harForvaltningslov = forvaltningsloven.isNotEmpty()

        return if (harBarnetrygdlov && harForvaltningslov) {
            "Vedtaket er gjort etter barnetrygdloven ${utledParagrafer(barnetrygdloven)} og forvaltningsloven ${
                utledParagrafer(forvaltningsloven)
            }."
        } else if (harBarnetrygdlov) {
            "Vedtaket er gjort etter barnetrygdloven ${utledParagrafer(barnetrygdloven)}."
        } else if (harForvaltningslov) {
            "Vedtaket er gjort etter forvaltningsloven ${utledParagrafer(forvaltningsloven)}."
        } else {
            throw Feil("Har ingen paragrafer å utlede i vedtaksbrev ved formkrav avvist")
        }
    }
    fun utledLovtekstKS(formkrav: Set<Formkrav>): String {
        val formkravVilkårKS: Set<FormkravVilkårKS> = formkrav.tilFormkravVilkårKS()
        val kontantstøtteloven = formkravVilkårKS.flatMap { it.kontantstøtteloven }.sorted().toSet()
        val forvaltningsloven = formkravVilkårKS.flatMap { it.forvaltningsloven }.sorted().toSet()
        val harKontantstøttelov = kontantstøtteloven.isNotEmpty()
        val harForvaltningslov = forvaltningsloven.isNotEmpty()

        return if (harKontantstøttelov && harForvaltningslov) {
            "Vedtaket er gjort etter kontantstøtteloven ${utledParagrafer(kontantstøtteloven)} og forvaltningsloven ${
                utledParagrafer(forvaltningsloven)
            }."
        } else if (harKontantstøttelov) {
            "Vedtaket er gjort etter kontantstøtteloven ${utledParagrafer(kontantstøtteloven)}."
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

    enum class FormkravVilkårEF(val folketrygdLoven: Set<String>, val forvaltningsloven: Set<String>): FormkravVilkår {
        KLAGE_KONKRET(emptySet(), setOf("32", "33")),
        KLAGE_PART(emptySet(), setOf("28", "33")),
        KLAGE_SIGNERT( emptySet(), setOf("31", "33")),
        KLAGEFRIST_OVERHOLDT( setOf("21-12"), setOf("31", "33")),
    }

    enum class FormkravVilkårBA(val barnetrygdloven: Set<String>, val forvaltningsloven: Set<String>): FormkravVilkår {
        KLAGE_KONKRET( emptySet(), setOf("31", "33")),
        KLAGE_PART(emptySet(), setOf("31", "33")),
        KLAGE_SIGNERT(emptySet(), setOf("31", "33")),
        KLAGEFRIST_OVERHOLDT(setOf("15"), setOf("31", "33")),
    }

    enum class FormkravVilkårKS(val kontantstøtteloven: Set<String>, val forvaltningsloven: Set<String>): FormkravVilkår {
        KLAGE_KONKRET(emptySet(), setOf("31", "33")),
        KLAGE_PART(emptySet(), setOf("31", "33")),
        KLAGE_SIGNERT(emptySet(), setOf("31", "33")),
        KLAGEFRIST_OVERHOLDT(setOf("15"), setOf("31", "33")),
    }

    interface FormkravVilkår {}

    enum class Formkrav(val tekst: String) {
        KLAGE_KONKRET("du ikke har sagt hva du klager på"),
        KLAGE_PART("du har klaget på et vedtak som ikke gjelder deg"),
        KLAGE_SIGNERT("du ikke har underskrevet den"),
        KLAGEFRIST_OVERHOLDT("du har klaget for sent")
    }

    fun Set<Formkrav>.tilFormkravVilkårEF(): Set<FormkravVilkårEF> =
        this.map {
            when(it) {
                Formkrav.KLAGE_PART -> FormkravVilkårEF.KLAGE_PART
                Formkrav.KLAGE_KONKRET -> FormkravVilkårEF.KLAGE_KONKRET
                Formkrav.KLAGE_SIGNERT -> FormkravVilkårEF.KLAGE_SIGNERT
                Formkrav.KLAGEFRIST_OVERHOLDT -> FormkravVilkårEF.KLAGEFRIST_OVERHOLDT
            }
        }.toSet()

    fun Set<Formkrav>.tilFormkravVilkårBA(): Set<FormkravVilkårBA> =
        this.map {
            when(it) {
                Formkrav.KLAGE_PART -> FormkravVilkårBA.KLAGE_PART
                Formkrav.KLAGE_KONKRET -> FormkravVilkårBA.KLAGE_KONKRET
                Formkrav.KLAGE_SIGNERT -> FormkravVilkårBA.KLAGE_SIGNERT
                Formkrav.KLAGEFRIST_OVERHOLDT -> FormkravVilkårBA.KLAGEFRIST_OVERHOLDT
            }
        }.toSet()

    fun Set<Formkrav>.tilFormkravVilkårKS(): Set<FormkravVilkårKS> =
        this.map {
            when(it) {
                Formkrav.KLAGE_PART -> FormkravVilkårKS.KLAGE_PART
                Formkrav.KLAGE_KONKRET -> FormkravVilkårKS.KLAGE_KONKRET
                Formkrav.KLAGE_SIGNERT -> FormkravVilkårKS.KLAGE_SIGNERT
                Formkrav.KLAGEFRIST_OVERHOLDT -> FormkravVilkårKS.KLAGEFRIST_OVERHOLDT
            }
        }.toSet()
