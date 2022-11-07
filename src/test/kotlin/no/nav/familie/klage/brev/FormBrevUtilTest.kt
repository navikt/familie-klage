package no.nav.familie.klage.brev

import no.nav.familie.klage.brev.FormBrevUtil.utledIkkeOppfylteFormkrav
import no.nav.familie.klage.brev.FormBrevUtil.utledÅrsakTilAvvisningstekst
import no.nav.familie.klage.brev.FormBrevUtil.utledLovtekst
import no.nav.familie.klage.brev.FormBrevUtil.FormkravVilkår
import no.nav.familie.klage.brev.FormBrevUtil.FormkravVilkår.KLAGE_PART
import no.nav.familie.klage.brev.FormBrevUtil.FormkravVilkår.KLAGE_SIGNERT
import no.nav.familie.klage.brev.FormBrevUtil.FormkravVilkår.KLAGE_KONKRET
import no.nav.familie.klage.brev.FormBrevUtil.FormkravVilkår.KLAGEFRIST_OVERHOLDT
import no.nav.familie.klage.formkrav.domain.FormVilkår.IKKE_OPPFYLT
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.testutil.DomainUtil.oppfyltForm
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

internal class FormBrevUtilTest {


    @Test
    internal fun `et formkrav ikke oppfylt`() {
        val klagePart = oppfyltForm(UUID.randomUUID()).copy(klagePart = IKKE_OPPFYLT)
        val klageKonkret = oppfyltForm(UUID.randomUUID()).copy(klageKonkret = IKKE_OPPFYLT)
        val klageSignert = oppfyltForm(UUID.randomUUID()).copy(klageSignert = IKKE_OPPFYLT)
        val klagefristOverholdt = oppfyltForm(UUID.randomUUID()).copy(klagefristOverholdt = IKKE_OPPFYLT)

        assertThat(utledIkkeOppfylteFormkrav(klagePart)).isEqualTo(setOf(KLAGE_PART))
        assertThat(utledIkkeOppfylteFormkrav(klageKonkret)).isEqualTo(setOf(KLAGE_KONKRET))
        assertThat(utledIkkeOppfylteFormkrav(klageSignert)).isEqualTo(setOf(KLAGE_SIGNERT))
        assertThat(utledIkkeOppfylteFormkrav(klagefristOverholdt)).isEqualTo(setOf(KLAGEFRIST_OVERHOLDT))
    }

    @Test
    internal fun `ingen formkrav oppfylt`() {
        val formkravFireIkkeOppfylt = oppfyltForm(UUID.randomUUID()).copy(klagePart = IKKE_OPPFYLT,
                                                                          klageSignert = IKKE_OPPFYLT,
                                                                          klageKonkret = IKKE_OPPFYLT,
                                                                          klagefristOverholdt = IKKE_OPPFYLT)

        assertThat(utledIkkeOppfylteFormkrav(formkravFireIkkeOppfylt)).isEqualTo(setOf(KLAGE_PART, KLAGE_SIGNERT, KLAGE_KONKRET, KLAGEFRIST_OVERHOLDT))
    }

    @Test
    internal fun `skal ikke utlede innholdstekst dersom alle formkrav er oppfylt`() {
        val feil = assertThrows<Feil> { utledÅrsakTilAvvisningstekst(tomIkkeOppfylteFormkrav) }
        assertThat(feil.frontendFeilmelding).isEqualTo("Skal ikke kunne utlede innholdstekst til formkrav avvist brev uten ikke oppfylte formkrav")
    }

    @Test
    internal fun `skal utlede riktig innholdstekst dersom kun et formkrav er ikke oppfylt`() {
        assertThat(utledÅrsakTilAvvisningstekst(klagePart)).isEqualTo("$innholdstekstPrefix $klagePartTekst.")
        assertThat(utledÅrsakTilAvvisningstekst(klageKonkret)).isEqualTo("$innholdstekstPrefix $klageKonkretTekst.")
        assertThat(utledÅrsakTilAvvisningstekst(klageSignert)).isEqualTo("$innholdstekstPrefix $klageSignertTekst.")
        assertThat(utledÅrsakTilAvvisningstekst(klagefristOverholdt)).isEqualTo("$innholdstekstPrefix $klageFristOverholdtTekst.")
    }

    @Test
    internal fun `skal utlede riktig innholdstekst dersom flere formkrav ikke er oppfylt`() {
        val innholdstekst = utledÅrsakTilAvvisningstekst(ikkeOppfylteFormkrav)

        assertThat(innholdstekst).contains("$innholdstekstPrefix")
        assertThat(innholdstekst).contains(klageKonkretTekst)
        assertThat(innholdstekst).contains(klageSignertTekst)
        assertThat(innholdstekst).contains(klageFristOverholdtTekst)
    }

    @Test
    internal fun `skal ikke utlede lovtekst dersom alle formkrav er oppfylt`() {
        val feil = assertThrows<Feil> { utledLovtekst(tomIkkeOppfylteFormkrav) }
        assertThat(feil.message).isEqualTo("Har ingen paragrafer å utlede i vedtaksbrev ved formkrav avvist")
    }

    @Test
    internal fun `skal utlede riktig lovtekst dersom kun et formkrav er ikke oppfylt`() {
        val klagePartLovtekst = utledLovtekst(klagePart)
        assertThat(klagePartLovtekst).contains(forvaltningslovPrefix)
        assertThat(klagePartLovtekst).contains("28")
        assertThat(klagePartLovtekst).contains("33")
        assertThat(klagePartLovtekst).doesNotContain("31")
        assertThat(klagePartLovtekst).doesNotContain("32")
        assertThat(klagePartLovtekst).doesNotContain("21-12")

        val klageKonkretLovtekst = utledLovtekst(klageKonkret)
        assertThat(klageKonkretLovtekst).contains(forvaltningslovPrefix)
        assertThat(klageKonkretLovtekst).contains("32")
        assertThat(klageKonkretLovtekst).contains("33")
        assertThat(klageKonkretLovtekst).doesNotContain("28")
        assertThat(klageKonkretLovtekst).doesNotContain("31")
        assertThat(klageKonkretLovtekst).doesNotContain("21-12")

        val klageSignertLovtekst = utledLovtekst(klageSignert)
        assertThat(klageSignertLovtekst).contains(forvaltningslovPrefix)
        assertThat(klageSignertLovtekst).contains("31")
        assertThat(klageSignertLovtekst).contains("33")
        assertThat(klageSignertLovtekst).doesNotContain("28")
        assertThat(klageSignertLovtekst).doesNotContain("32")
        assertThat(klageSignertLovtekst).doesNotContain("21-12")

        val klageFristLovtekst = utledLovtekst(klagefristOverholdt)
        assertThat(klageFristLovtekst).contains(folketrygdLovPrefix)
        assertThat(klageFristLovtekst).contains("og forvaltningsloven")
        assertThat(klageFristLovtekst).contains("21-12")
        assertThat(klageFristLovtekst).contains("31")
        assertThat(klageFristLovtekst).contains("33")
        assertThat(klageFristLovtekst).doesNotContain("28")
        assertThat(klageFristLovtekst).doesNotContain("32")
    }

    @Test
    internal fun `skal utlede riktig lovtekst dersom flere formkrav ikke er oppfylt`() {
        val lovtekst = utledLovtekst(ikkeOppfylteFormkrav)

        assertThat(lovtekst).contains("Vedtaket er gjort etter folketrygdloven")
        assertThat(lovtekst).contains("og forvaltningsloven")
        assertThat(lovtekst).contains("28")
        assertThat(lovtekst).contains("31")
        assertThat(lovtekst).contains("32")
        assertThat(lovtekst).contains("33")
        assertThat(lovtekst).contains("21-12")
    }

    val klagePartTekst = "du har klaget på et vedtak som ikke gjelder deg"
    val klageKonkretTekst = "du har ikke sagt hva du klager på"
    val klageSignertTekst = "du ikke har underskrevet den"
    val klageFristOverholdtTekst = "du har klaget for sent"
    val innholdstekstPrefix = "Vi har avvist klagen din fordi"
    val folketrygdLovPrefix = "Vedtaket er gjort etter folketrygdloven"
    val forvaltningslovPrefix = "Vedtaket er gjort etter forvaltningsloven"

    val tomIkkeOppfylteFormkrav = emptySet<FormkravVilkår>()
    val klagePart = setOf(KLAGE_PART)
    val klageKonkret = setOf(KLAGE_KONKRET)
    val klageSignert = setOf(KLAGE_SIGNERT)
    val klagefristOverholdt = setOf(KLAGEFRIST_OVERHOLDT)
    val ikkeOppfylteFormkrav = setOf(KLAGE_PART, KLAGE_KONKRET, KLAGE_SIGNERT, KLAGEFRIST_OVERHOLDT)
}