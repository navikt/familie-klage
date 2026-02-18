package no.nav.familie.klage.brev.avvistbrev

enum class Formkrav(
    val tekstForPerson: String,
    val tekstForInstitusjon: String,
) {
    KLAGE_KONKRET("du ikke har sagt hva du klager på", "dere ikke har sagt hva dere klager på"),
    KLAGE_PART("du har klaget på et vedtak som ikke gjelder deg", "dere har klaget på et vedtak som ikke gjelder institusjonen"),
    KLAGE_SIGNERT("du ikke har underskrevet den", "dere ikke har underskrevet den"),
    KLAGEFRIST_OVERHOLDT("du har klaget for sent", "dere har klaget for sent"),
}
