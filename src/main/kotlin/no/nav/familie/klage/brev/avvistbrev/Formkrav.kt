package no.nav.familie.klage.brev.avvistbrev

enum class Formkrav {
    KLAGE_KONKRET,
    KLAGE_PART,
    KLAGE_SIGNERT,
    KLAGEFRIST_OVERHOLDT,
    ;

    fun hentTekst(erInstitusjon: Boolean): String {
        val (subjekt, objekt) = if (erInstitusjon) "dere" to "institusjonen" else "du" to "deg"
        return when (this) {
            KLAGE_KONKRET -> "$subjekt ikke har sagt hva $subjekt klager på"
            KLAGE_PART -> "$subjekt har klaget på et vedtak som ikke gjelder $objekt"
            KLAGE_SIGNERT -> "$subjekt ikke har underskrevet den"
            KLAGEFRIST_OVERHOLDT -> "$subjekt har klaget for sent"
        }
    }
}
