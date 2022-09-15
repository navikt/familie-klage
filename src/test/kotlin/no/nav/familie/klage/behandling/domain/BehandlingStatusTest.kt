package no.nav.familie.klage.behandling.domain

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BehandlingStatusTest {

    @BeforeAll
    fun setUp() {
        mockkObject(SikkerhetContext)
    }

    @AfterAll
    fun ryddOpp() {
        unmockkObject(SikkerhetContext)
    }

    @Test
    fun `Skal være låst for vanlig saksbehandler når status er VENTER`() {

        every { SikkerhetContext.hentSaksbehandler(any()) } returns "saksbehandler"
        Assertions.assertThat(BehandlingStatus.VENTER.erLåstForVidereBehandling()).isTrue
    }

    @Test
    fun `Skal være åpen forsystembruker når status er VENTER`() {
        every { SikkerhetContext.hentSaksbehandler(any()) } returns "VL"
        Assertions.assertThat(BehandlingStatus.VENTER.erLåstForVidereBehandling()).isFalse
    }

    @Test
    fun `Skal være låst for systembruker når status er OPPRETTET`() {
        every { SikkerhetContext.hentSaksbehandler(any()) } returns "VL"
        Assertions.assertThat(BehandlingStatus.OPPRETTET.erLåstForVidereBehandling()).isTrue
    }

    @Test
    fun `Skal være åpen for vanlig saksbehandler når status er OPPRETTET`() {
        every { SikkerhetContext.hentSaksbehandler(any()) } returns "saksbehandler"
        Assertions.assertThat(BehandlingStatus.OPPRETTET.erLåstForVidereBehandling()).isFalse
    }

    @Test
    fun `Skal være låst for alle når status er FERDIGSTILT`() {
        every { SikkerhetContext.hentSaksbehandler(any()) } returns "saksbehandler"
        Assertions.assertThat(BehandlingStatus.FERDIGSTILT.erLåstForVidereBehandling()).isTrue
        every { SikkerhetContext.hentSaksbehandler(any()) } returns "VL"
        Assertions.assertThat(BehandlingStatus.FERDIGSTILT.erLåstForVidereBehandling()).isTrue
    }
}
