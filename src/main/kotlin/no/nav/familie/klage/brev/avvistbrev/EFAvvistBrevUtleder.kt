package no.nav.familie.klage.brev.avvistbrev

import no.nav.familie.klage.brev.avvistbrev.AvvistBrevUtleder.Companion.utledParagrafer
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import org.springframework.stereotype.Component

@Component
class EFAvvistBrevUtleder : AvvistBrevUtleder<FormkravVilkårEF> {
    override val fagsystem: Fagsystem = Fagsystem.EF

    override fun utledLovtekst(ikkeOppfylteFormkravVilkår: Set<FormkravVilkårEF>): String {
        val folketrygdloven = ikkeOppfylteFormkravVilkår.flatMap { it.folketrygdLoven }.sorted().toSet()
        val forvaltningsloven = ikkeOppfylteFormkravVilkår.flatMap { it.forvaltningsloven }.sorted().toSet()
        val harFolketrygdlov = folketrygdloven.isNotEmpty()
        val harForvaltningslov = forvaltningsloven.isNotEmpty()

        return if (harFolketrygdlov && harForvaltningslov) {
            "Vedtaket er gjort etter folketrygdloven ${utledParagrafer(folketrygdloven)} og forvaltningsloven ${
                utledParagrafer(forvaltningsloven)
            }."
        } else if (harFolketrygdlov) {
            "Vedtaket er gjort etter folketrygdloven ${utledParagrafer(folketrygdloven)}."
        } else {
            "Vedtaket er gjort etter forvaltningsloven ${utledParagrafer(forvaltningsloven)}."
        }
    }

    override fun Set<Formkrav>.tilFormkravVilkår(): Set<FormkravVilkårEF> =
        this
            .map {
                when (it) {
                    Formkrav.KLAGE_PART -> FormkravVilkårEF.KLAGE_PART
                    Formkrav.KLAGE_KONKRET -> FormkravVilkårEF.KLAGE_KONKRET
                    Formkrav.KLAGE_SIGNERT -> FormkravVilkårEF.KLAGE_SIGNERT
                    Formkrav.KLAGEFRIST_OVERHOLDT -> FormkravVilkårEF.KLAGEFRIST_OVERHOLDT
                }
            }.toSet()
}
