package no.nav.familie.klage.søk.dto

import java.util.UUID

data class PersonIdentDto(val personIdent: String, val behandlingId: UUID)

data class PersonTreffDto(val personIdent: String, val navn: String)
