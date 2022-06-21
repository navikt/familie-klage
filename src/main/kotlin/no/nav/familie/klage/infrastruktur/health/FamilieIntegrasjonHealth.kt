package no.nav.familie.klage.infrastruktur.health

import no.nav.familie.http.health.AbstractHealthIndicator
import no.nav.familie.klage.personopplysninger.PersonopplysningerIntegrasjonerClient
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!local")
class FamilieIntegrasjonHealth(personopplysningerIntegrasjonerClient: PersonopplysningerIntegrasjonerClient) :
    AbstractHealthIndicator(personopplysningerIntegrasjonerClient, "familie.integrasjoner")
