package no.nav.familie.klage.brev

import no.nav.familie.klage.brev.BrevInnhold.lagOpprettholdelseBrev
import no.nav.familie.klage.testutil.DomainUtil.fagsystemVedtak
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.assertj.core.api.Assertions.assertThat
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
            fagsystemVedtak("123", vedtakstidspunkt = vedtakstidspunkt),
            mottattDato
        )

        assertThat(brev.avsnitt.first().innhold).contains("01.01.2020")
        assertThat(brev.avsnitt.first().innhold).contains("05.11.2021")
        assertThat(brev.avsnitt.first().innhold).contains("overgangsstønad")
    }
}
