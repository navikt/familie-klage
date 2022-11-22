package no.nav.familie.klage.felles.util

import no.nav.familie.kontrakter.felles.klage.FagsystemType
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import no.nav.familie.kontrakter.felles.klage.Stønadstype

object StønadstypeVisningsnavn {

    private fun Stønadstype.navn() = this.name.lowercase()

    private fun Stønadstype.visningsnavn() = when (this) {
        Stønadstype.BARNETILSYN,
        Stønadstype.SKOLEPENGER -> "stønad til ${this.navn()}"

        Stønadstype.OVERGANGSSTØNAD,
        Stønadstype.KONTANTSTØTTE,
        Stønadstype.BARNETRYGD -> this.navn()
    }

    fun Stønadstype.visningsnavn(fagsystemVedtak: FagsystemVedtak?) =
        if (fagsystemVedtak?.fagsystemType == FagsystemType.TILBAKEKREVING) {
            "tilbakebetaling av ${this.visningsnavn()}"
        } else {
            this.visningsnavn()
        }


}