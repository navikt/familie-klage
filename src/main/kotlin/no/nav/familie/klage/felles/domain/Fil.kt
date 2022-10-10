package no.nav.familie.klage.felles.domain

data class Fil(val bytes: ByteArray) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return (bytes.contentEquals((other as Fil).bytes))
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }

    /**
     * Ønsker ikke å printe ut bytes i loggen hvis det blir feil
     */
    override fun toString(): String {
        return "Fil(bytes=visesIkke)"
    }
}
