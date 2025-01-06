package no.nav.familie.klage.brev

data class FritekstBrevRequestDto(
    val overskrift: String,
    val avsnitt: List<AvsnittDto>,
    val personIdent: String,
    val navn: String,
)
