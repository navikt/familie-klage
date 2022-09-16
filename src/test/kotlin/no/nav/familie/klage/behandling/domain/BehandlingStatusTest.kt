package no.nav.familie.klage.behandling.domain

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import org.assertj.core.api.Assertions.assertThat
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
        assertThat(BehandlingStatus.VENTER.erLåstForVidereBehandling()).isTrue
    }

    @Test
    fun `Skal være åpen forsystembruker når status er VENTER`() {
        every { SikkerhetContext.hentSaksbehandler(any()) } returns SikkerhetContext.SYSTEM_FORKORTELSE
        assertThat(BehandlingStatus.VENTER.erLåstForVidereBehandling()).isFalse
    }

    @Test
    fun `Skal være låst for systembruker når status er OPPRETTET`() {
        every { SikkerhetContext.hentSaksbehandler(any()) } returns SikkerhetContext.SYSTEM_FORKORTELSE
        assertThat(BehandlingStatus.OPPRETTET.erLåstForVidereBehandling()).isTrue
    }

    @Test
    fun `Skal være åpen for vanlig saksbehandler når status er OPPRETTET`() {
        every { SikkerhetContext.hentSaksbehandler(any()) } returns "saksbehandler"
        assertThat(BehandlingStatus.OPPRETTET.erLåstForVidereBehandling()).isFalse
    }

    @Test
    fun `Skal være låst for alle når status er FERDIGSTILT`() {
        listOf("saksbehandler", SikkerhetContext.SYSTEM_FORKORTELSE).forEach {
            every { SikkerhetContext.hentSaksbehandler(any()) } returns it
            assertThat(BehandlingStatus.FERDIGSTILT.erLåstForVidereBehandling()).isTrue
        }
    }
}
