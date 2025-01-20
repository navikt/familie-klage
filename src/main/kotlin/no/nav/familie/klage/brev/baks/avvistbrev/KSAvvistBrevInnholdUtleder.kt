package no.nav.familie.klage.brev.baks.avvistbrev

import no.nav.familie.klage.brev.felles.AvvistBrevInnholdUtleder
import no.nav.familie.klage.brev.felles.AvvistBrevInnholdUtleder.Companion.utledParagrafer
import no.nav.familie.klage.brev.felles.Formkrav
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import org.springframework.stereotype.Component

@Component
class KSAvvistBrevInnholdUtleder : AvvistBrevInnholdUtleder<FormkravVilkårKS> {
    override val fagsystem: Fagsystem = Fagsystem.KS

    override fun utledLovtekst(formkravVilkårKS: Set<FormkravVilkårKS>): String {
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
        } else {
            "Vedtaket er gjort etter forvaltningsloven ${utledParagrafer(forvaltningsloven)}."
        }
    }

    override fun Set<Formkrav>.tilFormkravVilkår(): Set<FormkravVilkårKS> =
        this
            .map {
                when (it) {
                    Formkrav.KLAGE_PART -> FormkravVilkårKS.KLAGE_PART
                    Formkrav.KLAGE_KONKRET -> FormkravVilkårKS.KLAGE_KONKRET
                    Formkrav.KLAGE_SIGNERT -> FormkravVilkårKS.KLAGE_SIGNERT
                    Formkrav.KLAGEFRIST_OVERHOLDT -> FormkravVilkårKS.KLAGEFRIST_OVERHOLDT
                }
            }.toSet()
}
