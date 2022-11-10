package no.nav.familie.klage.brev

import no.nav.familie.klage.brev.BrevInnhold.lagOpprettholdelseBrev
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class BrevInnholdTest {

    private val mottattDato = LocalDate.of(2020, 1, 1)
    private val vedtakstidspunkt = LocalDateTime.of(2021, 11, 5, 14, 56, 22)

    @Test
    internal fun `skal ha med klage mottattdato og vedtaksdato i brev for opprettholdelse`() {
        val brev = lagOpprettholdelseBrev(
            "123456789",
            "Innstilling abc",
            "Navn Navnesen",
            Stønadstype.OVERGANGSSTØNAD,
            FagsystemVedtak("123", "type", "resultat", vedtakstidspunkt),
            mottattDato
        )

        Assertions.assertThat(brev.avsnitt.first().innhold).contains("01.01.2020")
        Assertions.assertThat(brev.avsnitt.first().innhold).contains("05.11.2021")
        Assertions.assertThat(brev.avsnitt.first().innhold).contains("overgangsstønad")
    }
}
