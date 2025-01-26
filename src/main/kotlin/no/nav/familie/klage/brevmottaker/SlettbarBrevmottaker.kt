package no.nav.familie.klage.brevmottaker

import java.util.UUID

sealed interface SlettbarBrevmottaker
data class SlettbarBrevmottakerPersonUtenIdent(val id: UUID) : SlettbarBrevmottaker
data class SlettbarBrevmottakerPersonMedIdent(val personIdent: String) : SlettbarBrevmottaker
data class SlettbarBrevmottakerOrganisasjon(val organisasjonsnummer: String) : SlettbarBrevmottaker
