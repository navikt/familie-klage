package no.nav.familie.klage.brev.avvistbrev

import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.domain.FormVilkår.IKKE_OPPFYLT
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import org.springframework.stereotype.Component

interface AvvistBrevInnholdUtleder<T : FormkravVilkår> {
    val fagsystem: Fagsystem

    fun utledBrevInnhold(
        fagsak: Fagsak,
        form: Form,
    ): AvvistBrevInnhold {
        val ikkeOppfylteFormkrav = utledIkkeOppfylteFormkrav(form)
        if (ikkeOppfylteFormkrav.isEmpty()) {
            throw Feil("Kan ikke utlede brevinnhold for avvist brev dersom alle formkrav er oppfylt")
        }
        return AvvistBrevInnhold(
            årsakTilAvvisning = utledÅrsakTilAvvisningstekst(ikkeOppfylteFormkrav, fagsak.erInstitusjonssak()),
            brevtekstFraSaksbehandler =
                form.brevtekst ?: error("Må ha brevtekst fra saksbehandler for å generere brev ved formkrav ikke oppfylt"),
            lovtekst = utledLovtekst(ikkeOppfylteFormkrav.tilFormkravVilkår()),
        )
    }

    fun utledLovtekst(ikkeOppfylteFormkravVilkår: Set<T>): String

    fun Set<Formkrav>.tilFormkravVilkår(): Set<T>

    companion object {
        private fun utledIkkeOppfylteFormkrav(form: Form): Set<Formkrav> =
            setOf(
                if (form.klagePart == IKKE_OPPFYLT) Formkrav.KLAGE_PART else null,
                if (form.klageKonkret == IKKE_OPPFYLT) Formkrav.KLAGE_KONKRET else null,
                if (form.klageSignert == IKKE_OPPFYLT) Formkrav.KLAGE_SIGNERT else null,
                if (form.klagefristOverholdt == IKKE_OPPFYLT) Formkrav.KLAGEFRIST_OVERHOLDT else null,
            ).filterNotNull().toSet()

        private fun innholdstekstPrefiks(erInstitusjonssak: Boolean) = "Vi har avvist klagen ${if (erInstitusjonssak) "deres" else "din"} fordi"

        private fun utledÅrsakTilAvvisningstekst(
            formkrav: Set<Formkrav>,
            erInstitusjonssak: Boolean,
        ): String =
            if (formkrav.size > 1) {
                "${innholdstekstPrefiks(erInstitusjonssak)} ${formkrav.joinToString("") { "\n  •  ${it.hentTekst(erInstitusjonssak)}" }}"
            } else {
                "${innholdstekstPrefiks(erInstitusjonssak)} ${formkrav.single().hentTekst(erInstitusjonssak)}."
            }

        internal fun utledParagrafer(paragrafer: Set<String>): String =
            if (paragrafer.size == 1) {
                "§ ${paragrafer.first()}"
            } else {
                val alleUnntattSiste = paragrafer.toList().dropLast(1)
                val siste = paragrafer.toList().last()
                "§§ ${alleUnntattSiste.joinToString { it }} og $siste"
            }
    }

    @Component
    class Lookup(
        private val avvistBrevInnholdUtledere: List<AvvistBrevInnholdUtleder<*>>,
    ) {
        fun hentAvvistBrevUtlederForFagsystem(fagsystem: Fagsystem) = avvistBrevInnholdUtledere.single { it.fagsystem == fagsystem }
    }
}

data class AvvistBrevInnhold(
    val årsakTilAvvisning: String,
    val brevtekstFraSaksbehandler: String,
    val lovtekst: String,
)
