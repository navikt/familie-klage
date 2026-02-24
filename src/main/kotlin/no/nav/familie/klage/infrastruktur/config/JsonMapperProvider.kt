package no.nav.familie.klage.infrastruktur.config

import tools.jackson.databind.json.JsonMapper

object JsonMapperProvider {
    val jsonMapper: JsonMapper = no.nav.familie.kontrakter.felles.jsonMapper
}
