package no.nav.familie.klage.brev.avvistbrev

enum class Formkrav(val tekst: String) {
    KLAGE_KONKRET("du ikke har sagt hva du klager på"),
    KLAGE_PART("du har klaget på et vedtak som ikke gjelder deg"),
    KLAGE_SIGNERT("du ikke har underskrevet den"),
    KLAGEFRIST_OVERHOLDT("du har klaget for sent")
}