package no.nav.familie.klage.personopplysninger.pdl

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PdlDtoTest {

    @Test
    fun `navnBolk inneholder samme felter som blir spurt om i query`() {
        val spørringsfelter = PdlTestUtil.parseSpørring("/pdl/navn_bolk.graphql")

        val dtoFelter = PdlTestUtil.finnFeltStruktur(PdlTestdata.pdlNavnBolk)!!

        assertThat(dtoFelter).isEqualTo(spørringsfelter["data"])
    }
}
