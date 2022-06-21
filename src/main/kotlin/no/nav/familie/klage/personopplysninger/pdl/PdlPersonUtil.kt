package no.nav.familie.klage.personopplysninger.pdl


fun Navn.visningsnavn(): String {
    return if (mellomnavn == null) "$fornavn $etternavn"
    else "$fornavn $mellomnavn $etternavn"
}

fun Personnavn.visningsnavn(): String {
    return if (mellomnavn == null) "$fornavn $etternavn"
    else "$fornavn $mellomnavn $etternavn"
}

fun List<Navn>.gjeldende(): Navn = this.single()
fun List<Bostedsadresse>.gjeldende(): Bostedsadresse? = this.find { !it.metadata.historisk }
fun List<Oppholdsadresse>.gjeldende(): Oppholdsadresse? = this.find { !it.metadata.historisk }
fun List<Sivilstand>.gjeldende(): Sivilstand = this.find { !it.metadata.historisk } ?: this.first()
fun List<Fødsel>.gjeldende(): Fødsel = this.first()
fun List<DeltBosted>.gjeldende(): DeltBosted? = this.find { !it.metadata.historisk }
fun List<Folkeregisterpersonstatus>.gjeldende(): Folkeregisterpersonstatus? = this.find { !it.metadata.historisk }
fun List<Dødsfall>.gjeldende(): Dødsfall? = this.firstOrNull()
fun List<Adressebeskyttelse>.gjeldende(): Adressebeskyttelse? = this.find { !it.metadata.historisk }
fun List<Folkeregisteridentifikator>.gjeldende(): Folkeregisteridentifikator = this.distinct().single() // Distinkt luker vekk feilen med at samme person kan ha flere identiske identer som begge er i bruk

fun PdlIdenter.identer(): Set<String> = this.identer.map { it.ident }.toSet()
