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
            årsakTilAvvisning =
                if (fagsak.erInstitusjonssak()) {
                    utledÅrsakTilAvvisningstekstForInstitusjon(ikkeOppfylteFormkrav)
                } else {
                    utledÅrsakTilAvvisningstekstForPerson(ikkeOppfylteFormkrav)
                },
            brevtekstFraSaksbehandler =
                form.brevtekst ?: error("Må ha brevtekst fra saksbehandler for å generere brev ved formkrav ikke oppfylt"),
            lovtekst = utledLovtekst(ikkeOppfylteFormkrav.tilFormkravVilkår()),
        )
    }

    fun utledLovtekst(ikkeOppfylteFormkravVilkår: Set<T>): String

    fun Set<Formkrav>.tilFormkravVilkår(): Set<T>

    companion object {
        private const val INNHOLDSTEKST_PREFIX_PERSON = "Vi har avvist klagen din fordi"
        private const val INNHOLDSTEKST_PREFIX_INSTITUSJON = "Vi har avvist klagen fra institusjonen fordi"

        private fun utledIkkeOppfylteFormkrav(form: Form): Set<Formkrav> =
            setOf(
                if (form.klagePart == IKKE_OPPFYLT) Formkrav.KLAGE_PART else null,
                if (form.klageKonkret == IKKE_OPPFYLT) Formkrav.KLAGE_KONKRET else null,
                if (form.klageSignert == IKKE_OPPFYLT) Formkrav.KLAGE_SIGNERT else null,
                if (form.klagefristOverholdt == IKKE_OPPFYLT) Formkrav.KLAGEFRIST_OVERHOLDT else null,
            ).filterNotNull().toSet()

        private fun utledÅrsakTilAvvisningstekstForPerson(formkrav: Set<Formkrav>): String =
            if (formkrav.size > 1) {
                "$INNHOLDSTEKST_PREFIX_PERSON ${formkrav.joinToString("") { "\n  •  ${it.tekstForPerson}" }}"
            } else {
                "$INNHOLDSTEKST_PREFIX_PERSON ${formkrav.single().tekstForPerson}."
            }

        private fun utledÅrsakTilAvvisningstekstForInstitusjon(formkrav: Set<Formkrav>): String =
            if (formkrav.size > 1) {
                "$INNHOLDSTEKST_PREFIX_INSTITUSJON ${formkrav.joinToString("") { "\n  •  ${it.tekstForInstitusjon}" }}"
            } else {
                "$INNHOLDSTEKST_PREFIX_INSTITUSJON ${formkrav.single().tekstForInstitusjon}."
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
