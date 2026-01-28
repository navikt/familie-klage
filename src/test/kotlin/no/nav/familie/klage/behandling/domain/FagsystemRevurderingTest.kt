package no.nav.familie.klage.behandling.domain

import org.junit.jupiter.api.Test

internal class FagsystemRevurderingTest {
    @Test
    internal fun `skal kunne mappe enums`() {
        no.nav.familie.kontrakter.felles.klage.IkkeOpprettetÅrsak.entries.forEach {
            IkkeOpprettetÅrsak.valueOf(it.name)
        }
    }
}
