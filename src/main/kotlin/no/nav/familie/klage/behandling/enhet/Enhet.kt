package no.nav.familie.klage.behandling.enhet

import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.kontrakter.felles.klage.Fagsystem

interface Enhet {
    val enhetsnummer: String
    val enhetsnavn: String

    companion object {
        fun finnEnhet(
            fagsystem: Fagsystem,
            enhetsnummer: String,
        ): Enhet =
            when (fagsystem) {
                Fagsystem.BA -> BarnetrygdEnhet.entries.single { it.enhetsnummer == enhetsnummer }
                Fagsystem.KS -> KontantstøtteEnhet.entries.single { it.enhetsnummer == enhetsnummer }
                Fagsystem.EF -> throw Feil("Oppslag av enhet for EF er ikke støttet.")
            }
    }
}
