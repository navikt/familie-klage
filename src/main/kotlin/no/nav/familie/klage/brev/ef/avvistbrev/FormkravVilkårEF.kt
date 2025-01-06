package no.nav.familie.klage.brev.ef.avvistbrev

import no.nav.familie.klage.brev.FormkravVilkår

enum class FormkravVilkårEF(
    val folketrygdLoven: Set<String>,
    val forvaltningsloven: Set<String>,
) : FormkravVilkår {
    KLAGE_KONKRET(emptySet(), setOf("32", "33")),
    KLAGE_PART(emptySet(), setOf("28", "33")),
    KLAGE_SIGNERT(emptySet(), setOf("31", "33")),
    KLAGEFRIST_OVERHOLDT(setOf("21-12"), setOf("31", "33")),
}
