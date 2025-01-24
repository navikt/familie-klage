package no.nav.familie.klage.brev.baks.avvistbrev

import no.nav.familie.klage.brev.felles.FormkravVilkår

enum class FormkravVilkårKS(
    val kontantstøtteloven: Set<String>,
    val forvaltningsloven: Set<String>,
) : FormkravVilkår {
    KLAGE_KONKRET(emptySet(), setOf("32", "33")),
    KLAGE_PART(emptySet(), setOf("28", "33")),
    KLAGE_SIGNERT(emptySet(), setOf("32", "33")),
    KLAGEFRIST_OVERHOLDT(setOf("15"), setOf("31", "33")),
}
