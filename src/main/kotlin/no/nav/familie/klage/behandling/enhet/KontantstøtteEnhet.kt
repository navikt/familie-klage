package no.nav.familie.klage.behandling.enhet

enum class KontantstøtteEnhet(
    override val enhetsnummer: String,
    override val enhetsnavn: String,
) : Enhet {
    VIKAFOSSEN("2103", "NAV Vikafossen"),
    DRAMMEN("4806", "NAV Familie- og pensjonsytelser Drammen"),
    VADSØ("4820", "NAV Familie- og pensjonsytelser Vadsø"),
    OSLO("4833", "NAV Familie- og pensjonsytelser Oslo 1"),
    STORD("4842", "NAV Familie- og pensjonsytelser Stord"),
    STEINKJER("4817", "NAV Familie- og pensjonsytelser Steinkjer"),
    BERGEN("4812", "NAV Familie- og pensjonsytelser Bergen"),
    MIDLERTIDIG_ENHET("4863", "Midlertidig enhet"),
    ;

    companion object {
        private val GYLDIGE_BEHANDLENDE_BARNETRYGD_ENHETER =
            listOf(
                VIKAFOSSEN,
                DRAMMEN,
                VADSØ,
                OSLO,
                STORD,
                STEINKJER,
                BERGEN,
            )

        fun erGyldigBehandlendeKontantstøtteEnhet(enhetsnummer: String): Boolean =
            GYLDIGE_BEHANDLENDE_BARNETRYGD_ENHETER.any { it.enhetsnummer == enhetsnummer }
    }
}
