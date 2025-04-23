package no.nav.familie.klage.infrastruktur.config

import com.fasterxml.jackson.databind.ObjectMapper

object ObjectMapperProvider {
    val objectMapper: ObjectMapper = no.nav.familie.kontrakter.felles.objectMapper
}
