package no.nav.familie.klage.brev

import no.nav.familie.klage.brev.BrevInnhold.lagFormkravAvvistBrev
import no.nav.familie.klage.brev.BrevInnhold.lagFormkravAvvistBrevIkkePåklagetVedtak
import no.nav.familie.klage.brev.BrevInnhold.lagOpprettholdelseBrev
import no.nav.familie.klage.formkrav.domain.FormVilkår
import no.nav.familie.klage.testutil.DomainUtil.oppfyltForm
import no.nav.familie.klage.testutil.DomainUtil.påklagetVedtakDetaljer
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import no.nav.familie.kontrakter.felles.klage.VedtakType
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
            påklagetVedtakDetaljer("123", vedtakstidspunkt = vedtakstidspunkt, vedtakType = VedtakType.TILBAKEKREVING)
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
    internal fun `brev for opprettholdelse skal ha med info om sanksjon`() {
        val påklagetVedtakDetaljer =
            påklagetVedtakDetaljer("123", vedtakstidspunkt = vedtakstidspunkt, vedtakType = VedtakType.SANKSJON_1_MND)
        val brev = lagOpprettholdelseBrev(
            "123456789",
            "Innstilling abc",
            "Navn Navnesen",
            Stønadstype.OVERGANGSSTØNAD,
            påklagetVedtakDetaljer,
            mottattDato
        )
        assertThat(brev.avsnitt.first().innhold).isEqualTo(
            "Vi har 01.01.2020 fått klagen din på vedtaket om sanksjon som ble gjort 05.11.2021, " +
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
    internal fun `brev for avvist formkrav skal ha med info om tilbakebetaling`() {
        val påklagetVedtakDetaljer =
            påklagetVedtakDetaljer("123", vedtakstidspunkt = vedtakstidspunkt, vedtakType = VedtakType.TILBAKEKREVING)
        val brev = lagFormkravAvvistBrev(
            "123456789",
            "Innstilling abc",
            ikkeOppfyltForm(),
            Stønadstype.BARNETILSYN,
            påklagetVedtakDetaljer
        )
        assertThat(brev.overskrift).isEqualTo("Vi har avvist klagen din på vedtaket om tilbakebetaling av stønad til barnetilsyn")
    }

    @Test
    internal fun `brev for avvist formkrav skal ha med info om sanksjon`() {
        val påklagetVedtakDetaljer =
            påklagetVedtakDetaljer("123", vedtakstidspunkt = vedtakstidspunkt, vedtakType = VedtakType.SANKSJON_1_MND)
        val brev = lagFormkravAvvistBrev(
            "123456789",
            "Innstilling abc",
            ikkeOppfyltForm(),
            Stønadstype.BARNETILSYN,
            påklagetVedtakDetaljer
        )
        assertThat(brev.overskrift).isEqualTo("Vi har avvist klagen din på vedtaket om sanksjon")
    }

    @Test
    internal fun `brev for avvist formkrav uten påklaget vedtak skal føre til et eget avvisningsbrev`() {
        val brev = lagFormkravAvvistBrevIkkePåklagetVedtak(
            "123456789",
            "Innstilling abc",
            ikkeOppfyltForm(),
            Stønadstype.BARNETILSYN
        )
        assertThat(brev.overskrift).isEqualTo("Vi har avvist klagen din")
        assertThat(brev.avsnitt.first().innhold).isEqualTo("Vi har avvist klagen din fordi du ikke har klaget på et vedtak.")
        assertThat(brev.avsnitt.elementAt(1).innhold).isEqualTo("brevtekst")
        assertThat(brev.avsnitt.elementAt(2).innhold).isEqualTo("Vedtaket er gjort etter forvaltningsloven §§ 28 og 33.")
        assertThat(brev.avsnitt.elementAt(3).deloverskrift).isEqualTo("Du har rett til å klage")
        assertThat(brev.avsnitt.elementAt(4).deloverskrift).isEqualTo("Du har rett til innsyn")
        assertThat(brev.avsnitt.elementAt(5).deloverskrift).isEqualTo("Har du spørsmål?")
        assertThat(brev.avsnitt.size).isEqualTo(6)
    }

    private fun ikkeOppfyltForm() =
        oppfyltForm(UUID.randomUUID()).copy(klagePart = FormVilkår.IKKE_OPPFYLT, brevtekst = "brevtekst")
}
