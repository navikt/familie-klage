package no.nav.familie.klage.brev

import no.nav.familie.klage.brev.BrevInnhold.lagFormkravAvvistBrev
import no.nav.familie.klage.brev.BrevInnhold.lagOpprettholdelseBrev
import no.nav.familie.klage.formkrav.domain.FormVilkår
import no.nav.familie.klage.testutil.DomainUtil.oppfyltForm
import no.nav.familie.klage.testutil.DomainUtil.påklagetVedtakDetaljer
import no.nav.familie.kontrakter.felles.klage.FagsystemType
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class BrevInnholdTest {

    private val mottattDato = LocalDate.of(2020, 1, 1)
    private val vedtakstidspunkt = LocalDateTime.of(2021, 11, 5, 14, 56, 22)

    @Test
    internal fun `brev for opprettholdelse skal inneholde blant annat dato og stønadstype`() {
        val brev = lagOpprettholdelseBrev(
            "123456789",
            "Innstilling abc",
            "Navn Navnesen",
            Stønadstype.OVERGANGSSTØNAD,
            påklagetVedtakDetaljer("123", vedtakstidspunkt = vedtakstidspunkt),
            mottattDato
        )

        assertThat(brev.avsnitt.first().innhold).isEqualTo(
            "Vi har 01.01.2020 fått klagen din på vedtaket om overgangsstønad som ble gjort 05.11.2021, " +
                    "og kommet frem til at vedtaket ikke endres. NAV Klageinstans skal derfor vurdere saken din på nytt."
        )
    }

    @Test
    internal fun `brev for opprettholdelse skal ha med info om tilbakebetaling`() {
        val påklagetVedtakDetaljer =
            påklagetVedtakDetaljer("123", vedtakstidspunkt = vedtakstidspunkt, fagsystemType = FagsystemType.TILBAKEKREVING)
        val brev = lagOpprettholdelseBrev(
            "123456789",
            "Innstilling abc",
            "Navn Navnesen",
            Stønadstype.OVERGANGSSTØNAD,
            påklagetVedtakDetaljer,
            mottattDato
        )
        assertThat(brev.avsnitt.first().innhold).isEqualTo(
            "Vi har 01.01.2020 fått klagen din på vedtaket om tilbakebetaling av overgangsstønad som ble gjort 05.11.2021, " +
                    "og kommet frem til at vedtaket ikke endres. NAV Klageinstans skal derfor vurdere saken din på nytt."
        )
    }

    @Test
    internal fun `brev for avvist formkrav skal inneholde blant annat dato og stønadstype`() {
        val brev = lagFormkravAvvistBrev(
            "123456789",
            "Innstilling abc",
            ikkeOppfyltForm(),
            Stønadstype.SKOLEPENGER,
            påklagetVedtakDetaljer("123", vedtakstidspunkt = vedtakstidspunkt)
        )

        assertThat(brev.overskrift).isEqualTo(
            "Vi har avvist klagen din på vedtaket om stønad til skolepenger"
        )
    }

    @Test
    internal fun `brev for avvist formkra skal ha med info om tilbakebetaling`() {
        val påklagetVedtakDetaljer =
            påklagetVedtakDetaljer("123", vedtakstidspunkt = vedtakstidspunkt, fagsystemType = FagsystemType.TILBAKEKREVING)
        val brev = lagFormkravAvvistBrev(
            "123456789",
            "Innstilling abc",
            ikkeOppfyltForm(),
            Stønadstype.BARNETILSYN,
            påklagetVedtakDetaljer
        )
        assertThat(brev.overskrift).isEqualTo("Vi har avvist klagen din på vedtaket om tilbakebetaling av stønad til barnetilsyn")
    }

    private fun ikkeOppfyltForm() =
        oppfyltForm(UUID.randomUUID()).copy(klagePart = FormVilkår.IKKE_OPPFYLT, brevtekst = "brevtekst")
}
