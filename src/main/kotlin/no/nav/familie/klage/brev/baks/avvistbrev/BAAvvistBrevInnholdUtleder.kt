package no.nav.familie.klage.brev.baks.avvistbrev

import no.nav.familie.klage.brev.AvvistBrevInnholdUtleder
import no.nav.familie.klage.brev.AvvistBrevInnholdUtleder.Companion.utledParagrafer
import no.nav.familie.klage.brev.Formkrav
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import org.springframework.stereotype.Component

@Component
class BAAvvistBrevInnholdUtleder : AvvistBrevInnholdUtleder<FormkravVilkårBA> {
    override val fagsystem: Fagsystem = Fagsystem.BA

    override fun utledLovtekst(formkravVilkårBA: Set<FormkravVilkårBA>): String {
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
        } else {
            "Vedtaket er gjort etter forvaltningsloven ${utledParagrafer(forvaltningsloven)}."
        }
    }

    override fun Set<Formkrav>.tilFormkravVilkår(): Set<FormkravVilkårBA> =
        this
            .map {
                when (it) {
                    Formkrav.KLAGE_PART -> FormkravVilkårBA.KLAGE_PART
                    Formkrav.KLAGE_KONKRET -> FormkravVilkårBA.KLAGE_KONKRET
                    Formkrav.KLAGE_SIGNERT -> FormkravVilkårBA.KLAGE_SIGNERT
                    Formkrav.KLAGEFRIST_OVERHOLDT -> FormkravVilkårBA.KLAGEFRIST_OVERHOLDT
                }
            }.toSet()
}
